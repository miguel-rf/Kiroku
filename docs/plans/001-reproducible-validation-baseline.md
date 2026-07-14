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

Ten JVM suites under `app/src/test/` contain 28 passing tests. Four
instrumented classes contain nine passing tests:

- `app/src/androidTest/java/com/kiroku/app/KirokuAppJourneyTest.kt`
- `app/src/androidTest/java/com/kiroku/app/core/database/KirokuDatabaseDeviceTest.kt`
- `app/src/androidTest/java/com/kiroku/app/feature/search/SearchScreenTest.kt`
- `app/src/androidTest/java/com/kiroku/app/feature/series/SeriesScreenTest.kt`

The nine-test suite has executed successfully on separate API 36 Pixel 2 and
Pixel Tablet AVDs and again on Pixel 2 at 200% font scale. Its current coverage
includes saved-state restoration, cached offline detail and recovery, adaptive
navigation behavior, and real Room version-1 creation/reopening.

The most recent captured host gate was:

    ./gradlew spotlessCheck testDebugUnitTest lintDebug assembleDebug \
        assembleDebugAndroidTest assembleRelease --no-daemon --console=plain

It completed successfully from Milestone 2's clean validation checkout and from
the final Milestone 4 working tree on 2026-07-14 with 28 JVM tests and no lint
findings. The workflow definition is `.github/workflows/ci.yml`, and `README.md`
documents persistent SDK setup. `scripts/ci-device-test.sh` is the single
device-action command that measures the emulator and runs the instrumented
suite. The ignored `local.properties` points at the
persistent ignored `.android-sdk`, which contains Platform 36, Build Tools
35.0.0, platform-tools, Emulator 36.6.11, and the API 36 Google Play x86_64
image. Pixel 2 and Pixel Tablet AVDs provide the compact and large scenarios.

Milestone 1 resolved the original provenance defect, Milestone 2 resolved host
reproducibility, and Milestone 3 completed local device/accessibility
validation. The complete candidate has also passed clean-checkout host,
compact, and large gates. The active gaps are publication, successful hosted
host/compact/large CI, and the final synchronized handoff.

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
- [x] (2026-07-14 07:58Z) The owner created the private canonical remote
  `miguel-rf/Kiroku`; the GitHub plugin imported all 94 non-ignored files and
  fast-forwarded `main`. The full snapshot import commit is
  `08d92a20e2a99b3c40857e5e174095d563ddd200`. This establishes an
  owner-confirmed forward remote, but the current directory is still not a Git
  worktree and clean-checkout gates remain pending.
- [x] (2026-07-14 08:33Z) Restored trustworthy forward Git provenance from an
  owner-confirmed fresh clone. Before adoption, preserved the non-generated
  working tree at `/tmp/kiroku-pre-provenance-20260714T082846Z.tar.gz`
  (SHA-256 `76e26d989feeaee18aeb24e15589de42b7092c433c489b2c8cd0cb718a2a8dc5`).
  The clone passed `git fsck --full`, and using its index against this workspace
  produced a clean status before its metadata was adopted. Normal
  `rtk git status --short --branch` now succeeds at commit
  `6d33474b79db5592bf4b6458e7e9ab54b50ddb23` on `main...origin/main`.
- [x] (2026-07-14 08:43Z) Re-audited the official Android `sdkmanager`, GitHub
  Actions security, action release, and Gradle action documentation before
  editing Milestone 2. Selected current immutable releases for checkout, JDK
  setup, and Gradle setup, plus the open-source basic Gradle cache provider.
- [x] (2026-07-14 08:49Z) Documented persistent Android SDK installation,
  environment discovery, exact Platform 36/Build Tools 35.0.0 packages, and the
  ignored `local.properties` alternative in `README.md`.
- [x] (2026-07-14 08:49Z) Added the pinned, read-only host workflow and passed
  its complete Gradle gate from a clean clone of the implementation candidate,
  temporary commit `31d55c41c4947db327654ed9d32536ed4fcb9e7c` (tree
  `cc65ee5e41034ca5a19fda21377bda4d65f37c5d`). The checkout had no
  `local.properties` or project build directories and remained Git-clean after
  the build. Evidence-only plan and handoff updates followed the run. The
  GitHub-hosted workflow has not run because these workspace changes have not
  been published, so no remote CI result is claimed.
- [x] (2026-07-14 10:34Z) Installed a persistent ignored Android SDK containing
  Emulator 36.6.11 and API 36 Google Play x86_64 system image revision 7, then
  created separate Pixel 2 (1080x1920 at 420 dpi) and Pixel Tablet (2560x1600 at
  320 dpi) AVDs. Host KVM acceleration is usable. The compact AVD booted and
  includes TalkBack.
- [x] (2026-07-14 10:34Z) Ran the original six instrumented tests unchanged.
  The authorized physical device was dozing behind its lock/notification surface
  and all tests failed before assertions with no Compose hierarchy. On the fully
  interactive compact AVD, three tests passed and three exposed deterministic
  test defects: result matchers also selected the query field, and one detail
  assertion did not scroll below the viewport. Narrow test-only corrections are
  complete.
- [x] (2026-07-14 10:55Z) The corrected original six-test compact suite passed,
  then expanded device coverage passed 9/9 on the API 36 Pixel 2 AVD at
  1080x1920, 420 dpi (approximately 411 dp wide). The additions prove saved
  navigation state restoration, cached detail during an offline refresh failure
  and retry recovery, compact back navigation, and on-device creation/reopening
  of Room version 1 with its ten tables and expected identity hash. No test was
  skipped and no production network was used.
- [x] (2026-07-14 11:12Z) Passed the same expanded 9/9 suite with zero
  skips/failures on the independent API 36 Pixel Tablet AVD at 2560x1600,
  320 dpi (1280 dp wide). The adaptive journey verified simultaneous search and
  detail panes with no compact Back action.
- [x] (2026-07-14 11:12Z) Completed the tablet manual launch/search/detail,
  offline/recovery, process-death, rotation, resize, and Back smoke path. A
  small read-only public search cached One Piece; with airplane mode reporting
  no active network, the cached result and detail stayed visible with the
  offline messages, and retry cleared the error after connectivity returned.
  After backgrounding and terminating PID 6226, Android cold-restored the
  existing task with the selected route and cached detail. Rotation produced a
  1600x2560 portrait list/detail window. A real 1180x1600 override at 320 dpi
  crossed to 590 dp compact detail with Back, native-size reset restored both
  panes, and Back returned to search.
- [x] (2026-07-14 11:37Z) Completed the accessibility smoke. The compact suite
  passed 9/9 again with Android `font_scale=2.0`, and manual search-result and
  detail inspection at 200% confirmed readable wrapping and scroll reachability
  without overlap. With the bundled Google TalkBack service bound and touch
  exploration enabled, focus reached the labelled search field, navigated the
  selected result into details, and visibly focused the labelled Back action.
  The emulator was then restored to font scale 1.0 with accessibility disabled.
- [x] (2026-07-14 11:40Z) Added the API 36 compact/large device CI matrix using
  Pixel 2 and Pixel Tablet profiles, disabled animations, Ubuntu KVM, and
  android-emulator-runner 2.37.0 pinned to its full release SHA. `yq` parsed the
  workflow and all seven `uses:` entries resolve through 40-character SHAs.
- [x] (2026-07-14 11:40Z) Reran the exact host gate in the working tree. It
  completed with `BUILD SUCCESSFUL in 31s`; Spotless and the JVM task were
  up-to-date, while lint and packaging work executed. The retained ten JUnit XML
  suites were independently parsed as 28 tests with zero failures, errors, or
  skips; lint says `No issues found.`, and all three expected APKs passed archive
  integrity checks. A clean/uncached final JVM execution remains part of the
  candidate-checkout gate. The Room schema is unchanged.
- [x] (2026-07-14 11:56Z) Completed the final normalized compact device gate
  with `ANDROID_SERIAL=emulator-5556`. The command finished with `BUILD
  SUCCESSFUL in 31s`; its generated XML records 9 tests, zero failures, zero
  errors, and zero skips on the API 36 Pixel 2 AVD. This corrects the earlier
  stopping-point note: the pinned Gradle session had completed before the later
  user stop, even though the result had not yet been reconciled into the plan.
  The preceding unpinned multi-device failure remains recorded as diagnostic
  evidence only.
- [x] (2026-07-14 12:04Z) Completed an independent Luna Max read-only candidate
  audit and resolved its blocking findings. The journey fake now verifies the
  exact query/filter/series inputs; device CI explicitly installs the planned
  SDK packages and rejects a compact job above 599 dp or a large job below
  600 dp using measured display size/density. `yq` parsed the final workflow,
  its extracted portable shell script passed `sh -n`, all seven actions remain
  pinned to full SHAs, and `git diff --check` is clean.
- [x] (2026-07-14 12:04Z) Reran the exact host gate after those final candidate
  edits. It completed with `BUILD SUCCESSFUL in 31s`; 12 of 148 tasks executed,
  including Android-test compilation/packaging, Spotless, and lint. The clean
  candidate checkout remains the authoritative no-local-state gate.
- [x] (2026-07-14 12:17Z) Validated committed candidate
  `5093d60d9092e24f785c82c98c44568e636979f3` (tree
  `29a1c1363d236703bbf198577cb7410d9d3fbeea`) from fresh clone
  `/tmp/kiroku-plan001-clean.kYba8o`. It began without `local.properties`,
  project build directories, or local Gradle state; `git fsck --full` and Git
  status were clean. The exact host gate completed with `BUILD SUCCESSFUL in
  19s`; a forced `testDebugUnitTest --rerun-tasks --no-build-cache` completed
  in 50s and regenerated 28 tests across ten suites with zero
  failures/errors/skips. Lint reported no issues, all three APKs passed archive
  integrity checks, and Room version 1 remained unchanged.
- [x] (2026-07-14 12:17Z) Ran the clean candidate's complete device suite on
  both required API 36 profiles. Pixel 2 at 1080x1920 and 420 dpi (about 411 dp)
  completed 9/9 in 42s; Pixel Tablet at 2560x1600 and 320 dpi (1280 dp)
  completed 9/9 in 35s. Both XML reports contain zero failures, errors, or
  skips. The checkout remained Git-clean, and the temporary emulators were
  stopped after evidence capture.
- [x] (2026-07-14 12:23Z) Published the candidate and observed hosted run
  `29332138612` fail before any Gradle or device test. All three jobs reported
  `sdkmanager: command not found` in their SDK-package step. The run's exact
  Ubuntu 24.04 image manifest documents Android Command Line Tools 12.0 and SDK
  root `/usr/local/lib/android/sdk`; the tool is installed but its directory is
  not on the step's command path. The workflow now declares the documented SDK
  root, invokes the image-defined `cmdline-tools/latest/bin/sdkmanager`
  directly, and exports its directory for android-emulator-runner. No
  application, dependency, or schema change was required.
- [x] (2026-07-14 12:40Z) Hosted rerun `29332720434` proved the SDK-path fix:
  every job installed the exact Android packages, and the compact and large
  emulators booted at 1080x1920/420 dpi and 2560x1600/320 dpi. Both device jobs
  then stopped before Gradle because android-emulator-runner executes each
  newline in `script:` as a separate `/usr/bin/sh -c`; the multi-line `if`
  therefore ended before `fi`, and variables would not persist across lines.
  Moved the complete width guard and Gradle invocation into checked-in
  `scripts/ci-device-test.sh`, leaving one action command. `sh -n` passes; a
  fake-device harness accepted 411 dp compact and 1280 dp large inputs and
  rejected a 411 dp input labelled large. The still-running host job is not
  claimed as evidence and will be superseded by the corrected push.
- [ ] Complete Milestone 4's final handoff update and plan-wide retrospective.

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
- Decision: Treat `miguel-rf/Kiroku` as the owner-confirmed forward repository,
  not as proof that missing earlier Git history was reconstructed.
  Rationale: The user created the private repository and explicitly authorized
  a plugin upload from this snapshot. The local directory still lacks usable
  Git metadata, so a fresh clone and comparison are required before claiming a
  clean checkout.
  Date/Author: 2026-07-14, Codex.
- Decision: Adopt the verified fresh clone's Git metadata into the existing
  workspace only after comparing that workspace against the clone's complete
  index and ignore rules.
  Rationale: Git reported no tracked or untracked differences when the clone's
  metadata was evaluated against `/home/miguel/Kiroku`. This preserves the
  ignored local SDK pointer and build caches while making the verified-identical
  workspace the trusted clone worktree; a source archive provides an additional
  rollback copy.
  Date/Author: 2026-07-14, Codex.
- Decision: Pin every workflow action to the full commit SHA of its current
  stable release and grant the workflow only read access to repository content.
  Rationale: GitHub identifies a full-length commit SHA as the only immutable
  action reference. The host gate needs no write permission or secrets.
  Date/Author: 2026-07-14, Codex.
- Decision: Use `gradle/actions/setup-gradle` 6.2.0 with
  `cache-provider: basic` rather than its default enhanced provider.
  Rationale: Version 6 validates the wrapper and keys cached Gradle state from
  build inputs. The documented basic provider is open source and avoids the
  separate terms attached to the default proprietary caching component.
  Date/Author: 2026-07-14, Codex.
- Decision: Add only the host job in Milestone 2.
  Rationale: The plan deliberately reserves a pinned compact/large emulator job
  and actual instrumented execution for Milestone 3; reporting packaged tests as
  executed would be incorrect.
  Date/Author: 2026-07-14, Codex.
- Decision: Use separate API 36 Pixel 2 and Pixel Tablet AVD definitions for
  compact and large validation, with the authorized Realme device as additional
  compact evidence once it is interactive.
  Rationale: Their fixed profiles provide independently observable widths of
  approximately 411 dp and 1280 dp. The Play Store image also supplies TalkBack,
  and KVM makes repeated device execution practical.
  Date/Author: 2026-07-14, Codex.
- Decision: Use Compose `StateRestorationTester` for deterministic process-state
  restoration coverage and a real Room file close/reopen test for cached data
  persistence, then retain actual rotation and resize as emulator smoke checks.
  Rationale: These tests exercise the saved-state and durable-cache boundaries
  without introducing a debug-only activity or depending on the live service;
  actual window changes remain separately observable on the provisioned AVDs.
  Date/Author: 2026-07-14, Codex.
- Decision: Run the device CI gate as a two-entry Pixel 2/Pixel Tablet matrix on
  API 36 Google APIs x86_64 images using android-emulator-runner 2.37.0 pinned
  to commit `e89f39f1abbbd05b1113a29cf4db69e7540cae5a`.
  Rationale: These profiles reproduce the independently tested compact and
  600 dp-or-wider scenarios. The maintained runner's current primary
  documentation supports profile selection, animation disabling, and KVM on
  Ubuntu; the release commit is immutable.
  Date/Author: 2026-07-14, Codex.
- Decision: Assert each CI emulator's calculated width in dp before executing
  the adaptive tests.
  Rationale: The journey assertions intentionally follow the runtime width; a
  matrix label alone would not prove the tablet job actually crossed the
  600 dp breakpoint. The runner now fails unless compact is at most 599 dp and
  large is at least 600 dp, while also logging pixels and density.
  Date/Author: 2026-07-14, Codex.
- Decision: Install Platform 36, Build Tools 35.0.0, and platform-tools
  explicitly in each device job before the emulator runner.
  Rationale: The runner's current release can install a newer default Build
  Tools revision; explicit packages keep the device build aligned with the
  plan's host toolchain instead of relying on hosted-image state.
  Date/Author: 2026-07-14, Codex.
- Decision: Invoke the Ubuntu 24.04 image's documented Android Command Line
  Tools 12.0 binary at its image-defined absolute SDK path and export that path
  for the emulator action.
  Rationale: Hosted evidence proved the tool is not on the shell command path,
  while the exact runner-image manifest identifies its installed version and
  SDK root. This is narrower and more reproducible than adding another setup
  action or downloading an unpinned latest package.
  Date/Author: 2026-07-14, Codex.
- Decision: Give android-emulator-runner one command that invokes a checked-in
  POSIX shell script for display validation and Gradle execution.
  Rationale: Hosted logs prove the action runs each `script:` line in an
  independent shell. A repository script preserves variables and structured
  control flow, can be syntax-checked directly, and keeps the action input
  unambiguous.
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
- Observation: An owner-confirmed private GitHub remote now exists and contains
  the complete non-ignored snapshot.
  Evidence: the GitHub plugin created a full tree of 94 files, moved `main`
  without force, and all 94 returned blob SHAs matched local `git hash-object`
  results. Representative remote files, including `openapi.json`, the Gradle
  wrapper JAR, Room schema, source, README, and plans, matched their expected
  SHAs.
- Observation: The remote `main` head is now
  `6d33474b79db5592bf4b6458e7e9ab54b50ddb23`, a direct child of the documented
  snapshot import commit `08d92a20e2a99b3c40857e5e174095d563ddd200`.
  Evidence: the fresh clone showed one documentation-only commit named
  `Document canonical GitHub import`; its checked-out tree matched the current
  workspace exactly, and `git fsck --full` exited 0.
- Observation: The unusable local metadata contained no recoverable Git data.
  Evidence: pre-adoption inspection showed `.git` as an empty mode-0555
  directory and `rtk git status --short --branch` exited 128. No history was
  rewritten or synthesized; provenance came only from the owner-confirmed
  clone.
- Observation: Current maintained action releases are newer than the versions
  in older workflow examples: checkout 7.0.0, setup-java 5.5.0, and Gradle
  actions 6.2.0 were current on 2026-07-14.
  Evidence: each official release page identified that version as latest and
  linked the verified release commit used in `.github/workflows/ci.yml`.
- Observation: Gradle actions version 6 defaults to an enhanced caching
  component with separate terms, while version 6.1 and newer documents a basic
  cache provider built on `actions/cache`.
  Evidence: the official version 6 setup documentation and 6.2.0 release notes
  describe both providers; the workflow explicitly selects `basic`.
- Observation: The filesystem sandbox cannot create Gradle wrapper lock files
  in the existing user cache.
  Evidence: the first clean-clone gate stopped before any project task with a
  read-only-filesystem error for the Gradle 8.13 distribution lock. The approved
  host-cache rerun completed successfully; this is an execution-environment
  restriction, not a project failure.
- Observation: The clean-clone gate restored the JVM test task from Gradle's
  build cache, including an older report timestamp.
  Evidence: the canonical gate reported `:app:testDebugUnitTest FROM-CACHE`, so
  a second `testDebugUnitTest --rerun-tasks --no-build-cache` command was run.
  It executed all 33 scheduled tasks and regenerated 10 suites containing 28
  tests with zero skips, failures, or errors.
- Observation: The physical Realme RMX3851 is now ADB-authorized, but its first
  run occurred while `dumpsys power` reported `mWakefulness=Dozing` and Window
  Manager focused `NotificationShade`.
  Evidence: `connectedDebugAndroidTest` started all six tests on API 36, but each
  failed before its first UI assertion with `No compose hierarchies found`.
- Observation: Actual compact execution found three test-harness defects that
  compilation could not reveal.
  Evidence: on the API 36 Pixel 2 AVD, the result matcher found both the search
  field and result card, while the description assertion targeted content below
  the visible LazyColumn viewport. The other three original tests passed.
- Observation: Compose exposes the editable query and clickable result as
  separate nodes that can both contain the title; child metadata is not a
  reliable click target.
  Evidence: successive compact reruns showed exact node counts and callbacks.
  Matching the title plus click action while negating `hasSetTextAction()`
  uniquely targets the result and passed in both the focused and complete runs.
- Observation: Room version 1 creates and reopens cleanly on the target Android
  runtime without changing the exported schema.
  Evidence: the new device test found exactly the ten expected application
  tables, `PRAGMA user_version = 1`, identity hash
  `0c7bbedd114167b238c5c1d2aad91db8`, and the seeded recent search after close
  and reopen.
- Observation: Navigation 3's saved back stack survives an actual Android
  process death in the large layout, independently of the deterministic
  `StateRestorationTester` coverage.
  Evidence: the debuggable app was backgrounded, PID 6226 was terminated, and
  `am start -W` reported a cold launch that brought the existing task forward;
  the hierarchy still contained `Series details` and the selected One Piece
  detail. The search query itself reset, but durable Room content and the
  selected route remained available.
- Observation: The existing Compose semantics remained usable under TalkBack
  without production changes.
  Evidence: with the spoken-feedback service bound and touch exploration
  active, the accessibility focus indicator reached the search field and the
  labelled Back action after navigating a result to details; the hierarchy also
  exposed the Refresh action by its content description.
- Observation: android-emulator-runner 2.37.0 is the current maintained release
  and its release updated the bundled SDK build tools to 36.0.0.
  Evidence: the official release page dated 2026-03-14 identifies commit
  `e89f39f1abbbd05b1113a29cf4db69e7540cae5a`; the official README documents
  Ubuntu KVM setup and the `api-level`, `target`, `arch`, `profile`,
  `emulator-options`, and `disable-animations` inputs used by this workflow.
- Observation: local `connectedDebugAndroidTest` targets every connected device
  unless `ANDROID_SERIAL` is set.
  Evidence: the final normalization attempt started nine tests on both
  `emulator-5556` and `RMX3851`; the emulator completed all nine, but the
  dozing/covered physical UI reproduced the known no-Compose-hierarchy failures
  and failed the aggregate task. The resumable command is therefore explicitly
  pinned to `emulator-5556`.
- Observation: An independent audit found the journey fake initially ignored
  the passed `SearchSpec` and selected/refreshed series IDs.
  Evidence: the fake now rejects any query other than `One Piece`, any
  unexpected type/filter selection, or any observed/refreshed ID other than
  `55,099,564,912`. This keeps the journey deterministic while proving that the
  UI forwards the intended search and selection inputs.
- Observation: An adaptive test that branches on runtime width cannot by itself
  prove the CI matrix provisioned two different form factors.
  Evidence: an independent completion audit found both matrix entries would
  accept compact behavior if the requested tablet profile silently booted at a
  narrow width. The workflow now derives width dp from `wm size` and `wm
  density` and enforces the expected side of the 600 dp breakpoint before the
  Gradle command.
- Observation: The hosted Ubuntu 24.04 runner installs Android Command Line
  Tools but does not expose `sdkmanager` on the job shell's command path.
  Evidence: run `29332138612` failed identically in the host, compact, and large
  package-install steps with exit 127. Its runner metadata links image
  `20260705.232.1`, whose official manifest records Command Line Tools 12.0 and
  SDK root `/usr/local/lib/android/sdk`; the corresponding official image-build
  source installs the executable at `cmdline-tools/latest/bin/sdkmanager`.
- Observation: android-emulator-runner does not execute a multi-line `script:`
  block as one shell program.
  Evidence: in run `29332720434`, both device jobs logged a separate
  `/usr/bin/sh -c` for each line. The assignment lines completed, then the
  isolated `if ... then` line failed with `expecting "fi"`. Both AVDs had
  already booted with the expected profile geometry, so this was command
  framing rather than an emulator or application failure.

## Outcomes & Retrospective

Milestone 1 completed on 2026-07-14 at 08:33Z. The current workspace is now a
normal Git worktree backed by the owner-confirmed private remote, with `main`
tracking `origin/main` at `6d33474b79db5592bf4b6458e7e9ab54b50ddb23` before
this plan update. The fresh clone's object database passed `git fsck --full`,
and its complete index plus ignore rules reported the existing workspace clean
before adoption. The preserved source archive and its SHA-256 are recorded in
`Progress`. No Android source, schema, dependency, or test changed in this
milestone. Clean-checkout build/CI, device, and accessibility outcomes remain
for milestones 2 through 4 at that stopping point and must not be inferred from
the provenance result alone.

Milestone 2 completed locally on 2026-07-14 at 08:49Z. `README.md` now gives a
persistent, clean-checkout-safe SDK setup and `.github/workflows/ci.yml` defines
the host gate on `ubuntu-24.04` with JDK 17, exact Android SDK packages,
immutable action SHAs, read-only permissions, and the Gradle basic cache
provider. `yq` parsed the workflow and a focused scan confirmed that all three
actions use 40-character SHAs.

The implementation and pre-result documentation were committed only inside a
temporary staging clone, then cloned again to
`/tmp/kiroku-m2-clean.QUMVB8`. That checkout was clean and had no
`local.properties`, `.gradle`, root `build`, or `app/build` before the gate.
The prescribed command completed with `BUILD SUCCESSFUL in 25s`; 148 tasks
were actionable, with 68 executed, 78 restored from cache, and two up-to-date.
A forced no-build-cache JVM run then executed the tests themselves: 28 tests
across 10 suites, zero skips, failures, or errors. Lint reported `No issues
found.` The debug, debug Android-test, and unsigned minified release APKs were
20,957,237, 1,126,143, and 1,986,704 bytes. The post-build checkout was clean,
and Room remained version 1 with ten tables and identity hash
`0c7bbedd114167b238c5c1d2aad91db8`. Evidence-only plan and handoff changes were
then made in the working tree to record those results.

No Android source, dependency, test, or schema changed in Milestone 2. The
workflow is ready to run when the current changes are published, but a
GitHub-hosted run is deliberately not reported as evidence yet.

Milestone 3 completed on 2026-07-14 at 11:37Z. The original compact tests were corrected
only for semantics disambiguation and lazy-list scrolling, then passed 6/6.
The expanded suite passed 9/9 on the compact Pixel 2 AVD, covering the required
saved-state, offline/recovery, back-navigation, and on-device Room persistence
boundaries with deterministic fakes. It also passed 9/9 on the independent
1280 dp Pixel Tablet AVD. Manual tablet evidence now covers the public
launch/search/detail smoke, cached offline behavior and recovery, actual cold
process restoration, portrait rotation, a native-to-590 dp resize transition,
and Back. The full 9-test compact suite also passed at 200% font scale; manual
large-text inspection found only expected wrapping and scrolling. TalkBack
touch exploration reached the search field and the labelled Back action while
navigating the result-to-detail path. No production source or database schema
change was needed for the device milestone.

Milestone 4 is in progress. The final working-tree gates and the complete
candidate's clean-checkout host/compact/large gates are green. The two-profile
emulator CI matrix is width-enforcing and statically validated. An independent
audit also closed gaps in journey input/ID validation and explicit device-job
SDK installation. The unpinned local device invocation is not a green
aggregate result because it also selected the covered physical device; the
subsequent pinned and clean-checkout emulator invocations are the acceptance
evidence. The candidate is published; the first hosted run exposed and led to
a narrow SDK command-path correction before any project test executed. The
second run proved that correction and both emulator profiles, then exposed the
action's per-line script semantics before Gradle. The width/test logic now lives
in one checked-in shell script. No successful hosted GitHub Actions result is
claimed yet.

## Remaining work

Milestones 1 through 3 are complete, and the committed candidate passes all
clean-checkout host, compact, and large gates. Publish the reviewed
single-command device-script correction, wait for the hosted
host/compact/large matrix, and finish
`docs/PROJECT_STATE.md`, `docs/TESTING.md`, the plan index, and this
retrospective. The plan cannot be declared complete without the hosted CI and
final-audit results.

Revision note (2026-07-14): Initial plan created from the verified Phase 2
handoff. Later the same day, recorded the owner-created private GitHub remote
and verified plugin import without claiming that local Git or clean-checkout
validation was restored. At 08:33Z, recorded completion of milestone 1 after a
fresh clone, object verification, full worktree comparison, backup, and safe
metadata adoption. At 08:43Z, recorded Milestone 2's primary-documentation
audit and CI security/cache decisions before validation. At 08:49Z, recorded
the reproducible SDK documentation, pinned host workflow, clean-clone gate,
forced uncached JVM execution, unchanged schema, and precise remote-CI boundary
that complete Milestone 2. At 10:34Z, recorded persistent emulator provisioning,
the first physical/compact executions, and the narrow test determinism defects
they exposed. At 10:55Z, recorded the green expanded compact suite and its
saved-state/offline/Room coverage. At 11:12Z, recorded the green large-profile
suite and manual tablet search/detail, offline recovery, process death,
rotation, resize, and Back evidence. At 11:37Z, recorded the green 200%-font
suite and manual large-text/TalkBack smoke, completing Milestone 3. No product
feature scope was added. At 11:49Z, recorded the pinned two-profile CI matrix,
green final host gate, known multi-device aggregate failure, exact resumable
compact command, and user-requested stop without claiming the interrupted run.
At 11:56Z, reconciled the retained Gradle output and XML proving that the pinned
compact rerun had in fact completed 9/9 before the later stop, and narrowed the
remaining work to clean-candidate, publication, hosted-CI, and final handoff
evidence. At 12:04Z, recorded the independent candidate audit, enforced real CI
width/toolchain invariants, hardened journey inputs, and captured the green
post-edit host gate. At 12:17Z, recorded the fresh-clone candidate's green host,
forced JVM, compact, and large gates and narrowed the remaining work to
publication, hosted CI, and final synchronized closure. At 12:23Z, recorded the
first hosted run's pre-test SDK command-path failure and the image-manifest-based
workflow correction without misreporting the failed run as test evidence. At
12:40Z, recorded the corrected run's successful SDK/emulator setup, the
action's per-line script behavior, and the locally harnessed single-script fix.
