# Establish a reproducible validation and CI baseline

This ExecPlan is a living document. Keep `Progress`, `Unexpected discoveries`,
`Decision log`, `Outcomes & Retrospective`, and the revision note current while
work proceeds. Maintain it in accordance with `docs/PLANS.md`.

## Goal

Turn the currently host-verified search-to-series-details slice into a
reproducible baseline that can be trusted by the plans that follow. A fresh
checkout must build without relying on an undocumented temporary environment,
continuous integration must run the non-device quality gates, and the existing
Compose tests must actually execute on compact and large Android form factors.

This plan does not add authentication, library, synchronization, or release
features. Its purpose is to prove the behavior already implemented and establish
safe source-control and CI feedback before more stateful work is layered on top.

## User-visible behavior

After this plan, a tester can install Kiroku on a phone, search for a series,
open details, rotate or resize the window, restart the process, and observe the
same cached behavior already promised by the first slice. On a tablet or
foldable-sized display, search and selected details appear together. Turning
network access off leaves cached content visible with an offline message, and
restoring connectivity makes retry or refresh recover.

Developers can clone the repository, provision the documented SDK, run one
quality-gate command, and receive the same result locally and in CI. The visible
proof is a successful CI run plus successful `connectedDebugAndroidTest` runs on
both a compact and a 600 dp-or-wider configuration.

## Relevant existing code

The repository root is `/home/miguel/Kiroku`. `app/build.gradle.kts` defines a
single Android application with compile/target SDK 36, minimum SDK 23, Java 17,
Compose, Room schema export, debug and minified release variants, and
`androidx.test.runner.AndroidJUnitRunner`.

`app/src/main/java/com/kiroku/app/KirokuApp.kt` owns the Navigation 3 back stack.
It shows one pane below 600 dp and an explicit search/detail row at 600 dp or
wider. `feature/search/` and `feature/series/` contain the ViewModels and Compose
screens. `data/repository/OfflineFirstCatalogueRepository.kt` coordinates the
public API and Room. `core/database/KirokuDatabase.kt` declares schema version 1.

Ten JVM suites under `app/src/test/` currently contain 28 passing tests. The
instrumented tests that compile but have not run are:

- `app/src/androidTest/java/com/kiroku/app/KirokuAppJourneyTest.kt`
- `app/src/androidTest/java/com/kiroku/app/feature/search/SearchScreenTest.kt`
- `app/src/androidTest/java/com/kiroku/app/feature/series/SeriesScreenTest.kt`

The last captured host gate was:

    ./gradlew spotlessCheck testDebugUnitTest lintDebug assembleDebug \
        assembleDebugAndroidTest assembleRelease --no-daemon --console=plain

It completed successfully on 2026-07-13 with 28 JVM tests and no lint findings.
The ignored `local.properties` points at `/tmp/android-sdk`, which currently has
Platform 36 revision 2, Build Tools 35.0.0, and platform-tools 37.0.0. That SDK
path is temporary. `adb devices -l` reported device `6c604f95` as
`unauthorized`; no emulator or system image was installed.

The most important infrastructure defect is external to the Android code: this
directory is not a usable Git worktree. `git status` exits 128. Do not assume a
new Git history is equivalent to restoring the project repository.

## Confirmed API contracts

This validation plan exercises only the two already implemented public
operations under base URL `https://api.mangaupdates.com/v1/`:

- `POST /series/search` accepts the checked-in `SeriesSearchRequestV1` shape and
  returns `SeriesSearchResponseV1` with `total_hits`, `page`, `per_page`, and
  results. Kiroku requests 25 records because a discovery request confirmed the
  server returns that page size.
- `GET /series/{id}` returns `SeriesModelV1`. IDs are represented as `Long`; a
  live result contained `55,099,564,912`.

Automated tests must use fakes or MockWebServer. They must never depend on the
production service. Manual smoke testing may use the public service, but it must
remain small, read-only, and respectful of the API's caching requirement.

## Architecture

Do not redesign the product architecture in this plan. Room remains the source
of truth, the manual `AppContainer` remains the dependency boundary, and
Navigation 3 remains the route owner. Add test seams only where an observable
test cannot be made deterministic with existing constructor injection.

Continuous integration belongs under `.github/workflows/` once authentic Git
metadata is restored. Use the checked-in Gradle wrapper, a JDK 17 runtime, SDK
Platform 36, Build Tools 35.0.0, and dependency caches keyed from the wrapper
and version catalog. Pin maintained stable action revisions after checking their
current primary documentation; do not copy stale action versions from examples.
The host job runs formatting, JVM tests, lint, debug Android-test packaging, and
the minified release build. A separate emulator job runs instrumented tests on a
pinned API 23-or-newer system image and disables system animations.

Treat the compact and large configurations as separate observable scenarios.
If one emulator cannot change density reliably, use two managed device profiles
or two emulator definitions instead of faking a pass. Keep device tests free of
real network and arbitrary sleeps; use semantic conditions such as
`waitUntil` only with bounded failure diagnostics.

## Database changes

No schema change is allowed in this plan. Version 1 must remain at identity hash
`0c7bbedd114167b238c5c1d2aad91db8` in
`app/schemas/com.kiroku.app.core.database.KirokuDatabase/1.json`.

Add validation that creates the version-1 database and checks its ten expected
tables if existing tests do not already demonstrate this on the target device.
Do not add a migration or destructive fallback. Any unexpected schema output is
a regression to investigate, not a reason to overwrite the checked-in schema.

## Implementation milestones

Milestone 1 restores trustworthy repository provenance. Inspect `.git` without
modifying it and obtain either the original repository metadata or a fresh clone
from an owner-confirmed remote. Preserve the current working tree as a patch or
copy before moving it into the trusted clone. Do not run `git init`, rewrite
history, or choose a remote without explicit evidence. At the end, `git status
--short --branch` must work and the complete milestone diff must be reviewable.

Milestone 2 makes the toolchain reproducible. Document persistent Android SDK
installation in `README.md` without committing `local.properties`. Add the CI
workflow after checking current official action documentation. Run the complete
host gate from a fresh checkout with empty project build directories. The
expected result is formatting success, all JVM tests passing, no lint findings,
and debug, Android-test, and minified release APKs produced.

Milestone 3 executes device validation. Authorize the physical device or install
a pinned emulator/system image after any required SDK licence approval. Run the
existing instrumented tests first. Fix only genuine search/detail defects or
determinism problems discovered by those tests. Add missing device coverage for
process recreation, offline cached detail, compact back navigation, large
list/detail selection, rotation/resize, and large text where the current tests
do not prove them. Capture the exact device/API/display configuration.

Milestone 4 closes the baseline. Perform a short manual accessibility smoke test
with TalkBack and a large font scale, inspect the complete Git diff, rerun all
gates, and update `docs/PROJECT_STATE.md`, `docs/TESTING.md`, and this plan with
actual results. Do not mark this plan complete merely because device execution
is inconvenient.

## Testing requirements

From the repository root, run the host gate exactly as follows:

    ./gradlew spotlessCheck testDebugUnitTest lintDebug assembleDebug \
        assembleDebugAndroidTest assembleRelease --no-daemon --console=plain

Expect `BUILD SUCCESSFUL`, zero JVM failures/errors/skips unless a deliberately
documented test is conditionally unavailable, and `No issues found.` in
`app/build/reports/lint-results-debug.txt`. Record the new test count rather than
copying the baseline count of 28.

On each device configuration, run:

    ./gradlew connectedDebugAndroidTest --no-daemon --console=plain

Record the emulator/device model, API level, resolution, density, and final test
summary. Manually verify launch, search, details, offline cached content,
retry/recovery, back, rotation/resize, 600 dp layout, TalkBack labels, and 200%
font scale. No production credentials are needed or permitted.

After Git is restored, verify a clean checkout in a separate directory using the
same commands. CI and local results must agree. A difference caused by an
uncommitted generated schema, missing SDK declaration, or local-only file is a
failure of this plan.

## Progress

- [x] (2026-07-14 07:18Z) Inspected the current project handoff, build files,
  source layout, schema, and test inventory.
- [x] (2026-07-14 07:18Z) Confirmed the last host gate succeeded with 28 JVM
  tests, no lint findings, and all three APK types packaged.
- [ ] Restore original Git provenance or move the preserved working tree into
  an owner-confirmed fresh clone without losing changes.
- [ ] Replace the temporary SDK assumption with documented reproducible setup.
- [ ] Add and pass host CI gates from a clean checkout.
- [ ] Run and pass instrumented tests on compact and 600 dp-or-wider devices.
- [ ] Complete TalkBack, large-font, resize, offline/restart, and rotation smoke
  validation and record evidence.
- [ ] Update handoff documentation and complete the retrospective.

## Decision log

- Decision: Do not create a replacement Git repository with `git init`.
  Rationale: It would hide missing provenance and could discard the relationship
  to the real upstream history. Restoring or obtaining the correct repository is
  an external prerequisite.
  Date/Author: 2026-07-14, Codex.
- Decision: Keep device tests deterministic and independent of the production
  MangaUpdates service.
  Rationale: Network availability and live catalogue changes would make CI
  nondeterministic and could violate reasonable-use expectations.
  Date/Author: 2026-07-14, Codex.
- Decision: Treat compact and large layout execution as separate acceptance
  evidence.
  Rationale: Compilation and a width-unit test do not prove actual window,
  navigation, focus, or semantics behavior on Android.
  Date/Author: 2026-07-14, Codex.

## Unexpected discoveries

- Observation: The Android implementation is present, but the directory is not
  recognized as a Git worktree.
  Evidence: `rtk git status --short --branch` exits 128 with `Not a git
  repository`.
- Observation: Instrumented tests package but have never executed.
  Evidence: `adb devices -l` reported `6c604f95 unauthorized`, and the SDK has no
  emulator executable or system image.
- Observation: The working SDK is under `/tmp`, so it can disappear during host
  cleanup.
  Evidence: ignored `local.properties` contains `sdk.dir=/tmp/android-sdk`.
- Observation: Scheduling `clean` in the same Gradle invocation as Spotless can
  race over generated Android intermediates.
  Evidence: the prior combined run failed when `clean` deleted an `arsc.flat`
  file while Spotless snapshotted it; standalone `clean` followed by gates
  succeeded.

## Outcomes & Retrospective

No implementation milestone has completed yet. The existing host-verified
slice is the input baseline, not the outcome of this plan. At completion,
replace this paragraph with the clean-checkout, CI, device, accessibility, and
remaining-gap results.

## Remaining work

All implementation milestones remain. The immediate blocker is trustworthy Git
provenance; device authorization or emulator provisioning is the next external
dependency. Work that does not depend on those items may be prepared, but this
plan cannot be declared complete without both clean-checkout and actual device
evidence.

Revision note (2026-07-14): Initial plan created from the verified Phase 2
handoff. It deliberately adds no product feature scope.
