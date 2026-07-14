# Implement cached releases and durable background refresh

This ExecPlan is a living document. Keep `Progress`, `Unexpected discoveries`,
`Decision log`, `Outcomes & Retrospective`, and the revision note current while
work proceeds. Maintain it in accordance with `docs/PLANS.md`.

## Goal

Add a recent-releases destination backed by the documented MangaUpdates
release endpoints, Room cache, Paging where the result stream is genuinely
large, foreground refresh, and conservative periodic WorkManager refresh. The
feature displays catalogue metadata only. It must never fetch, download, link
to, or render manga chapter content.

The signed-out availability of the recent-days endpoint must be verified before
being promised because the OpenAPI document applies global bearer security but
does not restate it on that operation. Authenticated release search depends on
plan 002. This plan does not implement library mutations or the complete
settings UI.

## User-visible behavior

A user can open Releases and see cached recent release records promptly,
including a title and the supplied release date, volume, chapter, and group
information when present. Missing fields degrade gracefully. The screen has
localized loading, empty, offline, recoverable error, retry, and explicit
refresh states. Cached rows remain visible during a failed refresh.

The list loads additional pages without repeating or reordering existing rows.
Supported filters expose only confirmed request values. If official evidence
confirms that `GET /releases/days` is public, signed-out users can browse it;
otherwise the screen explains that sign-in is required. An authenticated user
may use the documented release search operation.

When periodic refresh is enabled, the app refreshes a bounded recent window
under a connectivity constraint even after process death. It avoids aggressive
polling, does not wake solely for foreground refresh, and retains the last good
cache on temporary failure.

## Relevant existing code

`MangaUpdatesApi.kt` currently exposes only public series search and detail.
`NetworkModule.kt` supplies the shared strict JSON, timeouts, User-Agent, and
debug-safe logging. Plan 002 is expected to add authenticated request handling.

Room version 1 and its public catalogue/search schema live under
`app/src/main/java/com/kiroku/app/core/database/`, with the exported schema in
`app/schemas/com.kiroku.app.core.database.KirokuDatabase/1.json`. The existing
search implementation already demonstrates Paging 3 with a Room-backed
`RemoteMediator`, stable list keys, cached content, and foreground retry. Reuse
its proven patterns where they fit, but do not expose release DTOs or entities
to UI.

`AppContainer.kt` is the manual dependency graph. `KirokuApp.kt` currently has
only search and series routes. Material 3 Adaptive 1.2.0, Navigation 3 1.1.4,
Paging 3.5.0, and Room 2.8.4 are already catalogued. WorkManager and DataStore
are not dependencies yet. Plan 003 may have added a manual WorkerFactory and
advanced Room; inspect the actual tree rather than assuming its proposed names.

Before implementation, read `docs/API_NOTES.md`, the completed preceding
plans, and the release schemas in `openapi.json`. Query Context7 for the exact
stable Paging, Room migration, WorkManager periodic/test, and any DataStore APIs
used. Constrain queries to the repository's selected versions and check primary
Android documentation when results include older or pre-release examples.

## Confirmed API contracts

The base URL is `https://api.mangaupdates.com/v1/`.

`GET /releases/days` accepts optional integer `page` and boolean
`include_metadata` with documented default `false`. A 200 returns
`ReleaseSearchResponseV1`. The operation contains no local security declaration,
but the OpenAPI root applies bearer authentication globally. Make one small,
read-only official-documentation or production verification before deciding
whether this operation is available signed out. Record status, response shape,
and date without dumping release content. Do not send a fabricated bearer
value.

`POST /releases/search` uses bearer authentication and accepts
`ReleaseSearchRequestV1`. Confirmed optional members are:

- `search` and `search_type`, where type is `series` or `regular`;
- `added_by`, `page`, `perpage`, and `letter`;
- `orderby` values `date`, `time`, `title`, `vol`, or `chap`;
- `start_date` and `end_date` strings;
- `asc` values `asc` or `desc`;
- `group_id`, `pending`, and `include_metadata`.

A 200 returns `ReleaseSearchResponseV1`; a 400 uses `ApiResponseV1`. The
contract does not define date-string syntax or per-page bounds. Do not expose
date entry or hard-code a per-page value until official evidence confirms the
accepted format/bounds. It is safe to expose only confirmed enums and text
search initially.

`ReleaseSearchResponseV1` contains optional/defined `total_hits`, `page`,
`per_page`, `results`, and metadata describing series, user-list, and user
filters. Each result record is `ReleaseModelSearchV1`; its properties are
optional and include `id`, `title`, string `volume`, string `chapter`, group
objects with `name`, `group_id`, and `url`, string `release_date`, and string
`time_added`.

Volume and chapter are strings here and must not be coerced to the integer
progress model used by lists. The schema does not specify the date formats.
Preserve raw values in the cache, parse only after official or controlled
observational evidence, and provide a safe display fallback. A group URL is
metadata, not authorization to browse, scrape, or retrieve chapter content.

No numeric rate limit or cache validator is documented. Refresh must therefore
be conservative, user-triggered work must be bounded, and automated tests must
not call production.

## Architecture

Add release DTOs, entities, domain models, optional screen presentation models,
mappers, a remote data source, and `ReleasesRepository`. The repository exposes
a Paging stream backed by Room plus refresh/filter operations. Room remains the
source of truth; a successful network response is written transactionally and
the UI observes database state.

Treat the release feed as large/unbounded and use Paging 3. Cache a query
descriptor, ordered result mappings, records, groups, and page/remote-key state.
Use the server response's returned `page` and `per_page` to calculate progress;
stop on an empty result or when returned pagination demonstrates the end. Do not
assume request page size is honored.

Because server `id` is optional, distinguish the remote ID from a local cache
identity. Prefer a positive remote ID when supplied. For an otherwise displayable
record without an ID, derive a namespaced deterministic cache key from the exact
canonical record fields and ordered group identifiers; document that it is a
local deduplication key, not an API identifier. Drop only records that cannot
provide any meaningful title/release display, and test that rule.

Foreground refresh and retry use ViewModel coroutines and Paging APIs. Use
WorkManager only for durable periodic refresh. Reuse plan 003's WorkerFactory if
it exists; otherwise introduce one manual factory shared by future workers.
Schedule one uniquely named periodic request with a connectivity constraint and
a conservative default cadence, initially no more frequent than six hours.
Refresh only a bounded first/recent window. Use WorkManager backoff for temporary
failure; never loop or sleep inside a worker.

If a minimal preference is needed to enable/disable periodic work before plan
005, use one application-scoped Preferences DataStore instance and a typed
repository boundary. Resolve the current stable DataStore setup/edit/migration
APIs through Context7 and primary Android documentation. Store no tokens,
credentials, private list contents, or worker Authorization data in DataStore.
Plan 005 will own the visible sync controls.

An HTTP/network refresh failure leaves cached rows and timestamps intact while
recording a non-sensitive refresh status. A new successful query replaces only
that query's result mapping and prunes orphaned release records safely; it must
not delete another filter's cache. Access to authenticated search follows the
plan-002 session state and handles 401 without its own retry loop.

## Database changes

If plan 003 completed first, add one explicit version 2-to-3 migration. If this
plan runs first while authentication is blocked, add version 1-to-2 and revise
plan 003 so its library tables use the next version. Record the actual sequence
before editing `KirokuDatabase`; there must be one linear set of exported
schemas and migration tests.

Represent at least:

- Release records with a local cache key, nullable remote ID, raw title,
  volume, chapter, release-date, time-added, and observation timestamp.
- Release groups ordered per record, with nullable name, group ID, and URL.
- Query descriptors keyed by normalized endpoint/filter/account scope.
- Ordered query-result mappings with position.
- Per-query pagination/remote-key state using returned page and per-page data.

Index remote IDs, query/position, observation time, and any cleanup lookup.
Partition authenticated query caches by account identity; public recent-days
cache uses an explicit public scope. Preserve raw optional strings without
inventing dates or numeric progress. Do not store bearer tokens, authorization
headers, or raw error bodies.

Write and test an explicit Room migration from every prior version, including a
populated public catalogue. Export the new JSON schema and verify all old
tables and rows survive. Do not use destructive fallback.

## Implementation milestones

Milestone 1 closes the public-access and parsing questions. Reinspect official
documentation, perform the minimal signed-out verification if needed, record
the result, and add realistic redacted fixtures. Confirm only enough date
format behavior to parse if there is reliable evidence; otherwise keep raw
display strings.

Milestone 2 adds DTOs, API methods, error mapping, domain models, and mappers for
recent days and authenticated search. MockWebServer tests cover pagination,
missing fields, string volume/chapter, groups, malformed records, bodyless
errors, and authentication. No production response becomes a committed fixture
unless it is sanitized and suitable under the API's terms.

Milestone 3 adds the next Room schema, explicit migration, DAOs, and
Room-backed Paging implementation. Prove refresh replacement, append behavior,
stable ordering, deduplication, public/account partitioning, cache retention on
failure, and orphan cleanup.

Milestone 4 adds `ReleasesRepository`, ViewModel state, the route, and Compose
screen. Implement stable keys, appropriately sized cover thumbnails only when
metadata supplies them, confirmed filters, refresh, retry, cached/offline
states, and compact/expanded navigation. Make release/group/chapter semantics
clear without presenting a reader action.

Milestone 5 adds durable refresh. Resolve stable WorkManager and optional
DataStore APIs, configure unique periodic work, connectivity and backoff,
bounded refresh, boot/process restoration, enable/disable behavior, and tests.
Keep foreground refresh out of WorkManager.

Milestone 6 validates and documents. Inspect the complete diff, run all host
and device gates, exercise cached launch and periodic work with controlled
time/constraints, verify no chapter-content requests exist, and update API,
architecture, testing, project-state, and this plan with actual evidence.

## Testing requirements

DTO/mapping tests must cover realistic response pages, all confirmed enum
filters, optional metadata, absent IDs, empty/null strings, very large IDs,
multiple groups, string volume/chapter values, unknown keys, malformed required
response structure, and unparseable dates. MockWebServer tests cover both
endpoints, public/auth behavior as verified, exact query/body serialization,
returned page sizes, 400/401/429/5xx, timeout, offline I/O, and malformed JSON.

DAO and migration tests cover every actual schema transition, populated old
data preservation, record/group ordering, query isolation, append/refresh,
transaction rollback, duplicate pages, local fallback keys, paging end
detection, and cleanup. Paging tests use `paging-testing`; repository and
ViewModel Flow tests use controlled dispatchers, fake clocks, and Turbine.

WorkManager tests use the current official test driver. Cover unique scheduling,
network constraints, enable/disable, conservative cadence, backoff, bounded
page count, temporary/permanent/auth failures, process recreation, and account
change. No arbitrary delay or production call is allowed.

Compose tests cover loading, cached content, empty, offline, recoverable error,
append error, refresh, filters, signed-out gating if required, stable selection,
large fonts, TalkBack descriptions, and compact/expanded layouts. Manually
confirm the UI has no read/download/open-chapter affordance.

Run the full host gate from plan 001, explicit migration and WorkManager tests
if not included, and actual compact/large device tests. Record exact commands,
test counts, device configurations, and outcomes.

## Progress

- [x] (2026-07-14 07:24Z) Extracted the recent-days and authenticated release-
  search request/response contracts from `openapi.json`.
- [x] (2026-07-14 07:24Z) Recorded the global-versus-operation security
  ambiguity and unspecified date/per-page formats.
- [ ] Verify signed-out access to `GET /releases/days` through official evidence
  or one minimal read-only request and update this plan.
- [ ] Add and migrate the release cache schema.
- [ ] Implement and test release networking, mapping, Paging, and repository.
- [ ] Implement and test release UI and confirmed filters.
- [ ] Implement and test conservative periodic refresh.
- [ ] Run all host/device gates and content-boundary review.

## Decision log

- Decision: Use Paging 3 for releases.
  Rationale: Release history is a genuinely large, continuing result set, and
  the response exposes pages and total hits.
  Date/Author: 2026-07-14, Codex.
- Decision: Preserve release volume, chapter, and date values as strings unless
  their formats are officially confirmed.
  Rationale: The contract defines strings and does not specify date grammar;
  coercion could lose valid catalogue information.
  Date/Author: 2026-07-14, Codex.
- Decision: Use WorkManager only for periodic refresh, not user refresh.
  Rationale: Periodic work must survive process death, while foreground actions
  need immediate cancellable coroutine feedback.
  Date/Author: 2026-07-14, Codex.
- Decision: Do not expose group URLs as chapter-reading actions.
  Rationale: Kiroku is catalogue/tracking software and may not retrieve or
  display copyrighted chapter content.
  Date/Author: 2026-07-14, Codex.

## Unexpected discoveries

- Observation: `GET /releases/days` does not locally declare security, while
  the OpenAPI root declares bearer security.
  Evidence: inspection of the operation and root `security` member in the
  checked-in specification. Signed-out behavior remains unconfirmed.
- Observation: Release progress-like fields are strings, unlike library
  progress integers.
  Evidence: `ReleaseModelSearchV1.volume` and `.chapter` are strings.
- Observation: Date members are strings without an OpenAPI `format`.
  Evidence: `release_date`, `time_added`, `start_date`, and `end_date` do not
  declare a grammar in the checked-in schema.

## Outcomes & Retrospective

No implementation milestone has completed. The release contract and the
reader-content boundary are documented; replace this section with cache,
Paging, worker, UI, and gate results when the plan finishes.

## Remaining work

All implementation remains. The public-access check can proceed independently
while plan 002 is blocked. Authenticated search waits for plan 002. If this plan
advances Room before plan 003, revise both plans' migration numbers first.

Revision note (2026-07-14): Initial self-contained releases and periodic-
refresh plan created from the version-1 repository and confirmed OpenAPI
schemas. No application implementation was changed.
