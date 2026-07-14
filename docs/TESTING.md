# Testing

## Principles

Tests are deterministic, use controlled dispatchers and clocks, and do not call
the production MangaUpdates API. No arbitrary delay is permitted. Network tests
use MockWebServer; Flow tests use Turbine; Room tests use isolated temporary or
in-memory databases.

Test fixtures contain public catalogue-shaped samples and fake credentials
only. Tokens use unmistakable values such as test-token and are never copied
from a real account.

The testing stack and patterns were re-audited through Context7. In particular,
Turbine requires all events to be consumed or an explicit cancellation,
coroutines `runTest` supplies virtual time, Robolectric local resource tests
require `unitTests.isIncludeAndroidResources = true`, and Room migration tests
must validate exported schemas. Full provenance is in DEPENDENCIES.md.

## Commands

Local JVM and static checks:

    ./gradlew spotlessCheck
    ./gradlew testDebugUnitTest
    ./gradlew lintDebug
    ./gradlew assembleDebug
    ./gradlew assembleRelease

Device/emulator checks:

    ./gradlew connectedDebugAndroidTest

For CI, run compilation and JVM tests before lint so failures are quick and
clear. Instrumented tests require an API 23+ emulator; the eventual CI image
should use a pinned system image and disable animations.

## Current vertical-slice evidence

The final host command ran Spotless, 28 JVM tests, debug lint, debug packaging,
Android-test packaging, and the minified release build together. It completed
successfully; lint reports no issues and all JVM suites report zero failures,
errors, or skips.

The JVM suites cover DTOs, mappers, Room DAOs/relations, repository caching,
Paging `RemoteMediator`, ViewModel transitions/debounce, root navigation/layout
decisions, and MockWebServer request/error behavior. Three Compose instrumented
classes cover the launch/search/detail journey and screen state/action behavior.
They compile and package, but they have not executed because the only attached
device is unauthorized and no emulator is installed. They must not be described
as passing until `connectedDebugAndroidTest` succeeds.

## First vertical slice matrix

| Area | Required coverage |
| --- | --- |
| DTOs | realistic search/detail JSON, explicit nulls, absent fields, unknown keys, IDs above Int range, invalid required domain invariants |
| Mappers | summary and full detail mapping, author/artist split, Markdown-to-plain presentation, child ordering |
| Network | correct POST path/body/page, GET detail path, 400 envelope, 404, 429, 500, malformed JSON, disconnect, timeout |
| DAO | ordered query results, replacement transaction, recent search ordering, full detail relations, cache retention |
| Paging repository | refresh and append keys, empty terminal page, refresh replacement, cached initialization, failed append retention |
| ViewModel | debounce with virtual time, stale query cancellation, retry/refresh, cached content plus refresh error, initial fatal error |
| Compose | signed-out launch/search prompt, loading, empty, error/retry, list item click, compact detail navigation, expanded list-detail, large text |

Paging's `RemoteMediator` and Flow's stale-stream operators are the only narrow
experimental opt-ins needed by this slice. Each opt-in is placed at its use
site; mediator refresh, append, keys, transaction, and failure behavior are
covered directly, while ViewModel tests use virtual time for debounce and
cancellation behavior.

## Later-phase matrix

- Authentication state restoration, successful and failed login, token
  corruption, logout, expired session, and concurrent refresh.
- Same-transaction library/outbox updates, deterministic ordering, coalescing,
  temporary/permanent retry, process restart, and reconciliation overlay.
- Release caching and filtering.
- Room migration tests for every version transition. Schema version 1 exports
  its JSON now; the first migration fixture is added when version 2 is designed.
- Baseline Profile and macrobenchmark coverage for startup, search, library
  scrolling, and opening detail only after those flows stabilize.

## Reporting

Completion reports list each command actually run and its result. Missing SDK,
network, emulator, or host capability is reported as unavailable, never as a
pass. Warnings are fixed or narrowly suppressed with a reason.
