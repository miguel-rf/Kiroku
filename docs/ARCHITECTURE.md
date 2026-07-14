# Architecture

## Goals and boundaries

Kiroku is an offline-first Android catalogue, list tracking, and release
management app. It never fetches, downloads, or renders manga chapter content.
The only remote data source is the documented MangaUpdates API.

Library-level architecture decisions were re-verified through Context7 before
implementation continued. Resolved library IDs, queried behavior, stable
release checks, and deliberate experimental-API exceptions are maintained in
DEPENDENCIES.md. MangaUpdates request and response facts remain sourced only
from the official contract described in API_NOTES.md.

The first production version uses one Gradle application module. Package
boundaries provide clarity without paying the coordination and build cost of
premature modules:

    app/src/main/java/com/kiroku/app/
      core/common/
      core/model/
      core/network/
      core/database/
      core/security/
      core/designsystem/
      data/mapper/
      data/repository/
      feature/search/
      feature/series/
      sync/

DTOs stay in core/network, Room types stay in core/database, domain models stay
in core/model, and screen-specific presentation stays in each feature. No DTO
or entity crosses into Compose.

## Implementation checkpoint

The public search/detail vertical slice is implemented and verified on every
host-executable gate. `MainActivity` launches the Navigation 3/adaptive host;
the manual container, network layer, generated Room version-1 schema,
repository/mediator, ViewModels, and Compose screens compile in debug and
minified release variants. Twenty-eight JVM tests pass, lint reports no issues,
and the three Compose test classes compile and package. Device execution is not
claimed because the attached device is unauthorized and no emulator exists.
PROJECT_STATE.md is the authoritative handoff and command record.

## Data and state flow

    Compose UI
        | explicit action
        v
    Screen ViewModel -- StateFlow<UiState> --> Compose UI
        |
        v
    Repository
        |                      ^
        v                      |
    MangaUpdates API -----> Room transactions
                               |
                               +---- Flow / PagingSource ----> Repository

Room is the source of truth. A successful network response is validated,
mapped, and committed before the UI observes it. A failed refresh leaves cached
data intact and exposes a refresh/offline message separately from the content.

## Dependency injection

Application-lifetime objects are built in a manual AppContainer owned by the
Application class. The initial graph has a database, API service, network
monitor, repositories, and small factories; it has no nested runtime scopes and
no durable workers yet. Constructor injection remains explicit and tests can
replace the container at the Application boundary.

Hilt is intentionally deferred. Reconsider it when Phase 3 introduces multiple
Worker dependencies or when test replacement becomes demonstrably repetitive.

## Search cache and paging

The API exposes page/perpage and reports total_hits; a confirmed query returned
10,000 hits. Search therefore qualifies as a genuinely large result set.

Room tables separate:

- series summary records keyed by Long series ID;
- normalized search specifications keyed by a deterministic cache key;
- ordered query-to-series result rows;
- next-page/end-state remote keys;
- recent user-entered searches.

A Paging 3 RemoteMediator loads page 1 on refresh and later pages on append. The
network response and its remote key are written in one Room transaction.
Changing query or filter state creates a new Pager through flatMapLatest, which
cancels collection of stale results. POST response data is never treated as
cacheable by OkHttp; Room owns search caching.

Cache initialization may reuse a complete query result set younger than six
hours. Explicit refresh always asks the API. Old query mappings are eligible for
cleanup after 30 days, while shared series summaries remain if referenced by
details or library data.

## Series details

Series summaries and detail-only data are stored separately so a later search
summary cannot erase richer cached fields. Child tables keep alternative
titles, genres, categories, contributors, and publishers ordered and
queryable. Refresh replaces detail children transactionally.

The detail ViewModel observes Room immediately and starts a foreground refresh.
With cache present, refresh errors produce a non-blocking offline/error banner.
Without cache, the same failure becomes a full recoverable error state.

API descriptions contain Markdown. The domain retains the source text; a
screen mapper produces safe plain presentation text outside Composables. The app
does not interpret arbitrary HTML.

## Navigation and adaptive layout

Navigation 3 owns the compact-screen back stack with serializable navigation
keys. Material 3 Adaptive window information selects the presentation:

- compact: search and details are separate destinations;
- medium/expanded: search results and the selected detail can share a
  list-detail surface;
- a direct detail destination still works at every width.

Selection is derived from the saveable Navigation 3 back stack, so the same
route survives recomposition and presentation-width changes. Lazy content uses
Long series IDs as stable keys.

The Context7 audit confirmed the stable `NavKey` plus `rememberNavBackStack`
and `NavDisplay` pattern. It also showed that the high-level navigable adaptive
list-detail scaffold remains annotated as experimental in relevant API
surfaces. Kiroku therefore uses stable Navigation 3 and Material 3 Adaptive
releases with an explicit two-pane composition, avoiding the alpha adaptive
Navigation 3 integration and experimental adaptive scaffold APIs.

Destination-scoped ViewModels require the stable
androidx.lifecycle:lifecycle-viewmodel-navigation3 integration. The navigation
host installs rememberSaveableStateHolderNavEntryDecorator and
rememberViewModelStoreNavEntryDecorator so saveable state and ViewModel stores
follow each entry lifecycle. Both decorators and the integration dependency are
present in the verified application host.

Adaptive width selection uses stable currentWindowAdaptiveInfo and the stable
WindowSizeClass medium-width breakpoint API. It does not opt into experimental
window-size or pane-scaffold APIs.

## Error model

Infrastructure maps failures into a centralized sealed model:

- Offline
- Timeout
- Authentication
- Permission
- RateLimited, with retry metadata when the server supplies it
- Validation, with safe field messages when parseable
- NotFound
- Server
- Serialization
- Unknown

Network exceptions keep their cause for development diagnostics. UI models map
failure kinds to localized resources and never display exception strings.
Logging never includes request/response bodies, credentials, tokens, cookies,
authorization headers, or account data.

`UnknownHostException`, `ConnectException`, and other non-timeout I/O failures
map to Offline; socket/interrupted timeouts map to Timeout; serialization causes
are preserved as Serialization. HTTP failures retain only safe diagnostic
reason and retry metadata. Deterministic tests cover malformed success JSON,
timeout, refused connection, and rate limiting.

## Future local-first mutation and conflict policy

Phase 3 list mutations will execute in one Room transaction:

1. update the local library row;
2. append an outbox record with a UUID, monotonic creation sequence, operation
   type, target IDs, payload, attempt count, and state;
3. return so the UI reflects the change immediately.

The synchronizer sends operations deterministically by creation sequence and
coalesces safe superseded progress changes before transmission. It deletes only
server-confirmed operations. Temporary failures retain the operation and use
exponential backoff; authentication and validation failures become visible
permanent failures until the user resolves or retries them.

Server state is authoritative after confirmation. During reconciliation,
confirmed server values replace local confirmed fields, while fields affected
by unsent outbox operations are overlaid locally. This prevents refresh from
discarding offline work. Because the API does not document idempotency keys,
operations will use stable local IDs for bookkeeping and idempotent
read-before/write or final-state updates where the documented API permits; no
unsupported header will be invented.

## Threading and performance

- Retrofit suspend calls and Room suspend/Flow APIs keep I/O off the main
  thread.
- Immutable state is built in ViewModels and mappers.
- Search lists use stable keys and appropriately sized Coil requests.
- Rounded covers use Compose clipping, not bitmap transformations.
- Expensive text/category transformation is outside composition.
- Optimization beyond these defaults requires profile evidence.

Room schema output is configured through the stable Room Gradle plugin, which
the official Room documentation recommends for reproducible and cacheable
schema generation. KSP2 is used because Context7 and KSP's primary
documentation both mark KSP1 unsupported for Kotlin 2.3+.

The database is version 1 with ten search/detail cache tables and no migrations.
The Room Gradle plugin generated
`app/schemas/com.kiroku.app.core.database.KirokuDatabase/1.json`, whose identity
hash is `0c7bbedd114167b238c5c1d2aad91db8`. Version 1 is tested in-memory; the
first migration fixture is required with version 2. No destructive migration
fallback is allowed.
