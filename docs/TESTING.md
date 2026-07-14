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

    ./gradlew spotlessCheck testDebugUnitTest lintDebug assembleDebug \
        assembleDebugAndroidTest assembleRelease --no-daemon --console=plain

Device/emulator checks:

    ./gradlew connectedDebugAndroidTest

When more than one device is attached, select the intended target explicitly:

    ANDROID_SERIAL=emulator-5556 \
        ./gradlew connectedDebugAndroidTest --no-daemon --console=plain

`.github/workflows/ci.yml` runs the complete host command and a two-entry API 36
Google APIs x86_64 device matrix. The `pixel_2` entry exercises a compact
profile; `pixel_tablet` independently exercises a 600 dp-or-wider profile.
System animations are disabled and every workflow action is pinned to a full
commit SHA.

## Current vertical-slice evidence

The host command runs Spotless, 28 JVM tests across ten suites, debug lint,
debug packaging, Android-test packaging, and the minified release build. The
committed candidate `5093d60d9092e24f785c82c98c44568e636979f3` was cloned
into a fresh checkout without `local.properties` or project build directories.
The exact gate completed successfully in 19 seconds. A forced uncached JVM run
then regenerated all ten suites in 50 seconds: 28 tests, zero failures, errors,
or skips. Lint reports `No issues found.`, all three APKs are valid archives,
Room schema version 1 is unchanged, and the checkout remained Git-clean.

Corrected hosted-validation implementation
`99f68c9cf73f3422bc3b138928899ea50701cb55` was also exercised locally with
Android Platform 37.1 present to match the hosted image's newer-platform drift.
The exact host gate completed in 1 minute 8 seconds; the regenerated ten JVM
suites contain 28 tests with zero failures, errors, or skips, lint reports
exactly `No issues found.`, and all three APKs are non-empty. Only
runner-image-dependent lint issue `OldTargetApi` is suppressed while this
baseline intentionally pins target SDK 36; every other lint finding remains
subject to the exact zero-findings assertion.

The JVM suites cover DTOs, mappers, Room DAOs/relations, repository caching,
Paging `RemoteMediator`, ViewModel transitions/debounce, root navigation/layout
decisions, and MockWebServer request/error behavior.

Four instrumented classes contain nine tests. They cover search and detail
screen states/actions, compact navigation, expanded list/detail selection,
saved-state restoration, cached detail during an offline refresh failure and
retry recovery, plus real on-device creation and reopening of Room version 1.
The Room test verifies all ten application tables, `PRAGMA user_version = 1`,
identity hash `0c7bbedd114167b238c5c1d2aad91db8`, and persisted data after reopen.
No automated device test calls the production MangaUpdates service.

The nine-test suite passed with zero failures, errors, or skips on each of these
independent configurations:

| Scenario | API/profile | Resolution and density | Observable coverage |
| --- | --- | --- | --- |
| Compact | API 36 Pixel 2 | 1080x1920, 420 dpi (about 411 dp wide) | Single-pane result-to-detail navigation and Back |
| Large | API 36 Pixel Tablet | 2560x1600, 320 dpi (1280 dp wide) | Simultaneous list/detail panes with no compact Back action |
| Large text | API 36 Pixel 2, `font_scale=2.0` | 1080x1920, 420 dpi | Same nine tests at 200% font scale |

The same clean candidate checkout reran all nine tests on separately booted
compact and large API 36 AVDs. The compact run completed in 42 seconds and its
XML timestamp is `2026-07-14T12:11:28`; the large run completed in 35 seconds
and its XML timestamp is `2026-07-14T12:14:38`. Both reports contain nine tests
with zero failures, errors, or skips. Display size and density were measured
from the corresponding emulator session before each run; Android's generated
XML identifies both ephemeral sessions only as `emulator-5556 - 16`.

The normalized compact suite was rerun with `ANDROID_SERIAL=emulator-5556` on
2026-07-14 after restoring font scale 1.0, native size/density, disabling
TalkBack, and keeping all animation scales at zero. Its generated XML reports
9 tests, 0 failures, 0 errors, and 0 skipped. A separate unfiltered invocation
also discovered an attached Realme whose locked/dozing surface exposed no
Compose hierarchy; that known physical-device failure is not used as emulator
acceptance evidence.

Manual API 36 Pixel Tablet smoke evidence covers launch, a small read-only
public One Piece search, selection and details, cached results/details while
airplane mode reports no active network, retry after connectivity recovery,
actual process termination and cold task restoration, portrait rotation,
native-to-590 dp resize and return to the two-pane layout, and compact Back.
At 200% font scale, search cards and detail metadata wrapped without overlap
and remained reachable by scrolling. With TalkBack bound and touch exploration
enabled, accessibility focus reached the labelled search field and the labelled
Back action on details; the emulator was restored afterward.

The hosted chronology is complete. Run `29332138612` stopped before Gradle or
emulator execution because Ubuntu's installed `sdkmanager` was not on the shell
path; the workflow now invokes the image-manifested Command Line Tools binary
by absolute path. Run `29332720434` proved that fix and both emulator profiles,
then exposed android-emulator-runner's per-line shell behavior; the device
command became the single checked-in `sh scripts/ci-device-test.sh` invocation.
Run `29333616527` completed both device jobs green and the full host Gradle gate,
then exposed an exact-report mismatch caused by preinstalled Platforms 37/37.1
adding nonfatal `OldTargetApi` for pinned target 36. That behavior was
reproduced locally and narrowed without weakening other lint checks.

Final hosted run
[`29335569517`](https://github.com/miguel-rf/Kiroku/actions/runs/29335569517)
at commit `99f68c9cf73f3422bc3b138928899ea50701cb55` passed all three jobs:

- Host: `BUILD SUCCESSFUL in 13m 5s`; 148 actionable tasks (138 executed, 10
  from cache), followed by successful exact lint and non-empty debug,
  Android-test, and minified release APK verification.
- Compact: `1080px at 420dpi = 411dp wide`; 9/9 tests finished and `BUILD
  SUCCESSFUL in 12m 23s`.
- Large: `2560px at 320dpi = 1280dp wide`; 9/9 tests finished and `BUILD
  SUCCESSFUL in 15m 28s`.

The matrix emitted only non-failing cache-reservation annotations when parallel
device jobs attempted to save the same Gradle cache key; every job conclusion
was `success`.

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
