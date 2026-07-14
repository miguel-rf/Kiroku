# Implement the offline-first library and durable synchronization

This ExecPlan is a living document. Keep `Progress`, `Unexpected discoveries`,
`Decision log`, `Outcomes & Retrospective`, and the revision note current while
work proceeds. Maintain it in accordance with `docs/PLANS.md`.

## Goal

Implement MangaUpdates reading lists, add/move/remove operations, progress
updates, local search/sort/filter, sync status, and failed-operation recovery.
Every user mutation must update Room immediately and enqueue a durable outbox
operation in the same transaction. WorkManager later uploads operations in a
deterministic order and reconciles ambiguous results without discarding unsent
intent.

Plan 002 must provide a working authenticated session before this plan can send
list requests. This plan does not implement release browsing or settings beyond
the minimum controls needed to explain and retry library synchronization.

## User-visible behavior

An authenticated user can open their MangaUpdates lists, browse or search the
locally cached library, filter and sort it, open a series, add it to a list,
move it, update volume/chapter progress, and remove it. Each change appears
immediately, including while offline, and carries a textual and semantic sync
state rather than relying on color alone.

When connectivity returns, pending changes upload in order. Successful changes
lose their pending indicator after server confirmation. Temporary failures
retry with backoff; authentication failures preserve work and ask the user to
sign in; permanent validation or permission failures remain visible with an
explanation and recovery action. A user can edit or discard a permanently
failed operation without corrupting server-confirmed cached state.

The UI remains useful after process death and device restart. Refreshing remote
lists never silently overwrites unsent local changes. Large screens use the
established list/detail pattern, and compact screens retain normal back
navigation.

## Relevant existing code

`app/src/main/java/com/kiroku/app/core/database/KirokuDatabase.kt` is Room
schema version 1. Its exported schema is
`app/schemas/com.kiroku.app.core.database.KirokuDatabase/1.json`, with identity
hash `0c7bbedd114167b238c5c1d2aad91db8`. The current ten tables cache public
series, detail relations, search queries/results, and remote keys. Review all
entities and DAOs under `core/database/` before choosing new table and index
names.

`data/repository/OfflineFirstCatalogueRepository.kt` demonstrates the existing
Room-source-of-truth pattern. `AppContainer.kt` manually supplies dependencies.
`KirokuApp.kt` and `feature/series/` provide the Navigation 3 and adaptive
detail hooks into which authenticated library controls will be added. The
network layer already has strict JSON and centralized error mapping, but
`MangaUpdatesApi.kt` has no list methods.

Plan 002 is expected to add `SessionRepository`, authenticated request wiring,
and an account identifier. Reuse those exact names if implemented; update this
plan if plan 002 settles different names. There is currently no WorkManager,
worker factory, outbox, user-list schema, library repository, or library UI.

Before implementation, read `docs/API_NOTES.md`, `docs/ARCHITECTURE.md`,
`docs/SECURITY.md`, the completed plan 002, and the list definitions in
`openapi.json`. Query Context7 for the exact stable Room migration,
WorkManager/CoroutineWorker, worker-testing, and manual WorkerFactory APIs that
match the selected version; confirm with primary Android documentation if any
result is mixed-era or pre-release.

## Confirmed API contracts

All operations below use `https://api.mangaupdates.com/v1/` and bearer
authentication from plan 002.

- `GET /lists` returns an array of `ListsModelV1`.
- `GET /lists/{id}` returns `ListsModelV1`; path `id` is an integer and 404 is
  documented. It optionally supports `unrenderedFields`, which Kiroku does not
  need.
- `POST /lists/{id}/search` accepts `ListsSearchRequestV1` with optional `page`,
  `perpage`, and `search`; it returns `ListsSearchResponseV1` with
  `total_hits`, `page`, `per_page`, `list`, and `results`.
- `GET /lists/series/{series_id}` returns `ListsSeriesModelV1`; 404 means no
  entry is documented for that series.
- `POST /lists/series` accepts an array of `ListsSeriesModelUpdateV1`. A 200 has
  no response schema; 400 has no response schema; 412 is the documented
  five-second update delay.
- `POST /lists/series/update` accepts the same update array. A 200 and 400 use
  `ApiResponseV1`; 412 is the same update delay.
- `POST /lists/series/delete` accepts an array of integer series IDs. A 200 and
  400 use `ApiResponseV1`; 412 is the update delay.

`ListsModelV1` properties are optional in the schema: integer `list_id`,
`title`, `description`, `icon`, boolean `custom`, and `type` values `read`,
`wish`, `complete`, `unfinished`, or `hold`. Optional list settings include
public visibility; sort values `title`, `priority`, `date`, `rating`, `release`,
`unread`, or `userrating`; rating/status/comment display options; per-page
settings; and latest-chapter display. Required domain identity must not be
invented when an optional `list_id` or title is absent.

`ListsSeriesModelV1` requires a nested series record whose `id` is required.
Its optional state includes integer `list_id`, string `list_type`, string
`list_icon`, integer `status.volume`, integer `status.chapter`, integer
`priority`, and `time_added`, plus series metadata and optional user rating in
search results. Store series IDs as `Long`, matching the public catalogue.

`ListsSeriesModelUpdateV1` requires nested `series.id`. It optionally accepts
integer `list_id`, `status.volume`, `status.chapter`,
`status.increment_volume`, `status.increment_chapter`, and `priority`. Prefer
absolute volume/chapter values for deterministic replay; use increments only if
new official evidence demonstrates a necessary behavior. Preserve the API's
integer model even if the release catalogue represents volume/chapter as text.

No idempotency key or operation identifier is documented. Do not add an
unsupported header or field. No numeric list-search `perpage` bounds are
specified, so initially omit it and honor the response `per_page` until an
official bound is confirmed. HTTP 412 is temporary scheduling feedback, not a
permanent domain conflict.

## Architecture

Room holds two layers of library state. Server-confirmed rows represent the last
successful remote observation. Local overrides represent the user's effective
unsent intent. UI queries overlay the newest local override on the confirmed
row, so refresh can replace confirmed values without erasing pending work.
Repositories expose domain models and Flows; neither entities nor DTOs reach a
Composable.

For every add, move, progress, priority, or remove action, one Room transaction
must update the local override and insert or safely coalesce an outbox operation.
The transaction returns only after the effective UI state is durable. If
enqueue fails, the visible mutation must fail too. Give every operation a local
UUID and a monotonically increasing database sequence. The API cannot receive
the UUID, but it makes local execution, logging, and recovery deterministic.

Coalesce only queued, never-started operations for the same account and series
when the resulting absolute desired state is equivalent. For example, several
pending progress edits may become one absolute update. Never coalesce across an
in-flight or permanently failed operation without a tested state transition.
A remove supersedes earlier unsent add/update intent only when the effective
server outcome is provably the same. Keep these rules in one tested reducer,
not in workers or Composables.

Use WorkManager only for durable upload and reconciliation. Enqueue unique
connected work when the transaction commits and on app/session restoration.
`LibrarySyncWorker` reads the next eligible operation by sequence, loads the
current account token at execution time, and processes a bounded batch. It must
not store a token in WorkManager input data. Use a custom WorkerFactory supplied
by the application and manual container unless implementation evidence makes
Hilt materially simpler; record any change before adding Hilt.

Classify offline I/O, timeouts, HTTP 412, 429, and 5xx as temporary. For 412,
set an operation `notBefore` at least five seconds from the response and return
without sleeping a worker thread. Respect a valid Retry-After value when one is
provided. Treat 400 validation, 403 permission, and an authoritative 404 as
permanent after mapping any safe server reason. A 401 blocks the queue and
preserves operations until plan 002 restores authentication.

A timeout or connection loss after a mutation is ambiguous because the API has
no documented idempotency mechanism. Before resubmitting, reconcile with
`GET /lists/series/{series_id}`. If the server already equals the absolute
desired state, confirm the operation without resubmitting. For a desired
deletion, a documented 404 is confirmation. Otherwise retry the deterministic
absolute mutation after backoff. Never use increment mutations in this path.

Remote refresh writes list metadata and confirmed entries, then reapplies local
overrides in the query layer. When a successful mutation is acknowledged,
refresh or fetch that series state, update the confirmed row, and remove the
outbox/override atomically only if no newer sequence exists. Server state is
authoritative after confirmation; newer unsent local intent remains visible.

Partition every authenticated table and job by a stable account identifier from
plan 002. On logout, cancel that account's unique work. Ask whether cached list
data and unsent changes should be removed before deletion; never upload one
account's rows under another account's token.

## Database changes

Create one explicit migration from version 1 to version 2 unless plan 004 has
already advanced the schema. If releases ran first, update this plan before
editing code so it creates the next sequential version. Never use destructive
migration fallback. Export the new schema JSON and test every transition from
version 1.

The exact names may be aligned with existing conventions, but the schema must
represent these concepts and indexes:

- User-list metadata keyed by `(accountId, listId)`, including server type,
  title, icon, custom flag, and refresh timestamp.
- Server-confirmed library entries keyed by `(accountId, seriesId)`, linked to
  cached series and containing list, absolute volume/chapter, priority, and
  server observation time.
- A list-query cache and ordered result mapping keyed by account, list/query,
  and position, with any required remote-key state for paging.
- Local overrides keyed by `(accountId, seriesId)`, containing the complete
  desired state or deletion tombstone and the latest operation sequence.
- Pending operations keyed by local UUID and ordered by an auto-incrementing or
  otherwise monotonic sequence. Include account, series, operation kind,
  absolute target values, creation time, attempt count, `notBefore`, state, and
  a sanitized permanent-failure category.

Use foreign keys and cascades only where they cannot erase outbox intent.
Outbox operations must survive replacement of cached server entries. Add
indexes for account/list browsing, local library text search, next eligible
operation, per-series override lookup, and cleanup. Store no bearer token,
password, raw Authorization header, or sensitive response body in Room.

Write migration SQL explicitly, validate it with `MigrationTestHelper`, and
assert the migrated schema preserves every version-1 catalogue table and row.
Test migration both with empty and realistically populated version-1 data. A
schema identity mismatch is a failure; do not regenerate expected JSON to hide
it.

## Implementation milestones

Milestone 1 adds confirmed DTOs, service methods, mappers, and MockWebServer
contract tests for list metadata, list search, per-series state, add, update,
delete, bodyless success/error responses, and 412. Add no UI yet. Demonstrate
that malformed required nested series identities fail mapping.

Milestone 2 introduces version 2 of Room. Add entities, DAOs, aggregate query
models, the local-override reducer, and the explicit migration. Export and
verify the schema. DAO tests must prove atomic mutation-plus-outbox insertion,
overlay precedence, deterministic sequencing, safe coalescing, account
partitioning, and that refresh cannot erase a local override.

Milestone 3 implements `LibraryRepository`. Add list refresh, cached
observation, local search/sort/filter, add/move/progress/remove methods, and
effective-state Flows. Wire authenticated series-detail controls through this
repository. Foreground refresh uses normal coroutines; a user mutation schedules
durable upload only after its transaction commits.

Milestone 4 implements durable synchronization. Resolve the stable WorkManager
APIs through Context7, add the dependency and test artifact, configure the
application WorkerFactory, unique work, connectivity constraints, exponential
backoff, 412 scheduling, bounded batches, reconciliation, and session-aware
blocking. Add a failure-recovery API for retry, edit, or discard.

Milestone 5 delivers library and detail UI. Add the library route, list picker,
progress editor, add/move/remove confirmations, local search/sort/filter,
empty/offline/error/loading states, pull-to-refresh or an explicit stable
refresh action, and visible pending/failed/synced semantics. Adapt list/detail
behavior to compact and expanded widths without embedding business rules in
Composables.

Milestone 6 validates the complete offline journey. Inspect the full diff; run
all host and device gates; execute a controlled online-to-offline-to-online
scenario across process death; verify no duplicate visible mutation; inspect
the Room schema and WorkManager state; and update architecture, API, security,
testing, project-state, and this living plan with actual evidence.

## Testing requirements

Add DTO and mapper tests for all confirmed list shapes, nullable optional list
metadata, integer progress, large series IDs, missing required nested identity,
bodyless responses, API validation envelopes, and unknown keys. MockWebServer
must cover each endpoint, bearer attachment without token logging, pagination,
404 state, 400/401/403/412/429/5xx, timeouts, connection loss before and after a
request body, and deterministic reconciliation.

Room DAO and migration tests must cover version 1 to the new version, existing
catalogue preservation, atomic outbox insertion, rollback on enqueue failure,
effective-state overlays, tombstones, indexes, account isolation, operation
order, safe coalescing, retry metadata, permanent failures, and cleanup.
Repository and reducer tests use fake clocks and UUID sources. Flow assertions
use Turbine and controlled dispatchers.

Use WorkManager's current official test driver/facilities rather than arbitrary
delays. Test constraints, unique work, backoff, 412 `notBefore`, process
recreation, bounded batching, temporary retry, permanent failure, 401 blocking,
successful reconciliation after an ambiguous response, and a newer operation
arriving while an older one completes. Assert that worker input/output never
contains a token.

ViewModel tests cover initial cache, refresh, list selection, local search and
filters, each mutation, pending and failed transitions, offline edits, retry,
discard, logout/account change, and expired authentication. Compose tests cover
the ten critical journeys relevant here on compact and large layouts, including
large fonts and TalkBack-friendly sync/error messaging.

Run the complete host gate from plan 001 and actual compact/large device tests.
Also run migration tests and WorkManager integration tests explicitly if they
are not included by the aggregate task. Record exact commands, counts, devices,
and outcomes. A worker that is merely enqueued or an Android test APK that only
compiles is not acceptance evidence.

## Progress

- [x] (2026-07-14 07:24Z) Extracted the list, list-search, per-series state, and
  mutation contracts from the checked-in OpenAPI document.
- [x] (2026-07-14 07:24Z) Identified the absence of idempotency support and the
  documented five-second HTTP 412 mutation delay.
- [x] (2026-07-14 07:24Z) Queried the configured Context7 server for Room and
  WorkManager concepts; version-specific stable APIs remain an implementation-
  time check.
- [ ] Complete plan 002 and establish an authenticated account identifier.
- [ ] Add and migrate the library/outbox database schema.
- [ ] Implement and test list networking and repository behavior.
- [ ] Implement and test WorkManager synchronization and reconciliation.
- [ ] Deliver adaptive library and series-control UI.
- [ ] Run the complete offline/restart/reconnection acceptance journey and all
  quality gates.

## Decision log

- Decision: Model server-confirmed state separately from local unsent intent.
  Rationale: A remote refresh must not erase an offline edit, while successful
  reconciliation still needs an authoritative baseline.
  Date/Author: 2026-07-14, Codex.
- Decision: Persist absolute desired progress and use deterministic operations.
  Rationale: The API lacks documented idempotency, and increment replay after an
  ambiguous response could double-advance progress.
  Date/Author: 2026-07-14, Codex.
- Decision: Reconcile ambiguous mutations before retrying them.
  Rationale: The per-series state endpoint can prove that an add/update/delete
  already succeeded without inventing an unsupported idempotency header.
  Date/Author: 2026-07-14, Codex.
- Decision: Keep the manual container and inject Workers through a custom
  WorkerFactory initially.
  Rationale: This adds the needed process-recreation boundary without adopting
  a DI framework solely by convention.
  Date/Author: 2026-07-14, Codex.
- Decision: Treat HTTP 412 as delayed retry state.
  Rationale: The official mutation contract identifies it as a five-second
  update delay, not a permanent validation failure.
  Date/Author: 2026-07-14, Codex.

## Unexpected discoveries

- Observation: List updates accept integer progress, while release volume and
  chapter values are strings.
  Evidence: `ListsSeriesModelUpdateV1.status` uses integers; release contracts
  use string `volume` and `chapter`. These must remain separate domain concepts.
- Observation: Mutation success bodies are inconsistent across endpoints.
  Evidence: add documents no 200 response schema, while update and delete use
  `ApiResponseV1`. The remote layer must accept documented bodyless success.
- Observation: There is no documented server idempotency mechanism.
  Evidence: the mutation parameters and headers in `openapi.json` contain no
  idempotency field or key.
- Observation: Context7 search results are useful for locating WorkManager and
  Room APIs but may mix library eras.
  Evidence: the 2026-07-14 query returned old and current-main examples, so the
  exact stable artifact version must be resolved again when dependencies change.

## Outcomes & Retrospective

No implementation milestone has completed. This plan currently captures the
contract and the synchronization invariants needed to prevent data loss or
duplicate progress. Replace this section with schema, worker, UI, offline
journey, and gate outcomes after implementation.

## Remaining work

All implementation remains pending, and authenticated session support from
plan 002 is the first prerequisite. If plan 004 changes Room first while
authentication is blocked, revise the migration numbers and schema references
here before touching the database; there must be one linear, tested migration
history.

Revision note (2026-07-14): Initial self-contained library/outbox plan created
from the version-1 database and confirmed OpenAPI list contracts. No application
implementation was changed.
