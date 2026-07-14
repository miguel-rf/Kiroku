# Kiroku project state

Last updated: 2026-07-14

This is the authoritative, self-contained handoff. It distinguishes verified
build/test evidence from work that still needs hosted-CI evidence, and from
later work blocked on additional MangaUpdates contracts.

## 1. Current project objective

Build Kiroku, a production-quality, unofficial Android catalogue, tracking,
and release-management client that uses only the documented MangaUpdates API.
Kiroku is not a manga reader and must not scrape MangaUpdates or fetch,
download, or display copyrighted manga chapters.

The completed product milestone is the first offline-first vertical slice:

    Search -> MangaUpdates API -> Room -> repository -> ViewModel -> Compose
           -> Navigation 3 -> series details

The milestone implementation and every local host/device quality gate are now
complete. Twenty-eight JVM tests pass; four instrumented classes contain nine
tests that pass on separate compact and large API 36 emulator profiles and on
the compact profile at 200% font scale. Authentication, library
synchronization, and releases have not been started.

The active repository task is Plan 001 Milestone 4. Six self-contained living
ExecPlans cover every remaining validation,
authentication, library, release, settings/accessibility, and
production-hardening milestone. The owner-created private repository
`miguel-rf/Kiroku` supplies the verified Git metadata at
`6d33474b79db5592bf4b6458e7e9ab54b50ddb23`. Milestones 1 through 3 restored
provenance, established persistent SDK/host CI, and completed the compact,
large, resilience, and accessibility matrix. Milestone 4 has a statically
validated compact/large emulator workflow plus green working-tree and
clean-candidate gates. The candidate is published; its first hosted run exposed
a pre-test SDK command-path defect that is corrected locally. A successful
hosted rerun and final synchronized handoff remain before closure.

## 2. Architecture and settled technical decisions

- The first version remains one `app` Gradle module with explicit `core`,
  `data`, and `feature` package boundaries. Additional modules require build,
  ownership, or dependency-boundary evidence.
- The app uses offline-first unidirectional data flow. Compose sends explicit
  actions to screen ViewModels; ViewModels expose immutable `StateFlow` state;
  repositories coordinate Room and Retrofit; Room is the UI source of truth.
- API DTOs, Room entities, domain models, and screen UI models are separate.
  DTOs and entities are not exposed to Compose.
- A manual application-owned `AppContainer` provides the small application
  graph. Hilt remains deferred until worker scopes or test-replacement
  complexity justify it.
- Search uses Paging 3 and a Room-backed `RemoteMediator` because the confirmed
  endpoint can report 10,000 results. Requests use the server-confirmed
  25-record page size; result rows and remote keys are committed atomically.
- Search and detail refreshes use foreground coroutines. WorkManager is not a
  dependency and remains reserved for later durable synchronization work.
- Navigation 3 owns a serializable `NavKey` back stack. The host installs both
  saveable-state and ViewModel-store entry decorators. Compact widths use
  single-pane navigation; widths at the stable 600 dp medium breakpoint use an
  explicit list/detail `Row` while retaining the same back stack.
- Stable Material 3 Adaptive window information is used without experimental
  adaptive scaffolds or the alpha adaptive Navigation 3 integration.
- The project uses AGP 8.13.2, Gradle 8.13, Kotlin 2.3.21, KSP 2.3.9,
  Compose BOM 2026.06.00, AndroidX Core 1.18.0, Lifecycle 2.10.0,
  Navigation 3 1.1.4, Material 3 Adaptive 1.2.0, Paging 3.5.0, Room 2.8.4,
  Retrofit 3.0.0, OkHttp 5.4.0, Coil 3.4.0, coroutines 1.11.0, and
  kotlinx.serialization 1.11.0. These pins were checked with Context7 and,
  where version compatibility required it, primary vendor documentation.
- Coil remains at 3.4.0 because 3.5.0 selected Kotlin stdlib 2.4.0, which is
  outside AGP 8.13's supported Kotlin bytecode range and produced R8 warnings.
- CI uses `ubuntu-24.04`, JDK 17, the checked-in wrapper, exact Android SDK
  packages, read-only permissions, full action commit SHAs, and Gradle's basic
  cache provider. Its device matrix uses separate API 36 Google APIs x86_64
  Pixel 2 and Pixel Tablet profiles with animations disabled.
- MangaUpdates identifiers are `Long`; live data contains values larger than
  `Int.MAX_VALUE`.
- JSON ignores unknown keys but does not coerce invalid required values into
  defaults. Only contract-optional or live-confirmed null fields are nullable.
- Cleartext is disabled in the manifest for the complete min-SDK range.
  Release builds contain no HTTP logger. Debug logging is BASIC metadata only
  and redacts Authorization, Cookie, and Set-Cookie.
- Authentication remains contract-blocked: the official login response does
  not define the token member or expiry metadata. No speculative auth, token
  storage, list, outbox, WorkManager, or release code exists.

## 3. Features already completed

- A runnable launcher activity, Material 3 theme, edge-to-edge root, Navigation
  3 routes, destination-scoped ViewModels, compact navigation, and a stable
  adaptive list/detail layout.
- Public `POST /series/search` and `GET /series/{id}` Retrofit integration with
  finite timeouts, defensive serialization, centralized failures, and safe
  debug logging.
- Room-backed search summaries, normalized query/result caching, remote paging
  keys, recent-search history, full series details, and ordered detail child
  records.
- Debounced search input (350 ms), stale-query cancellation, Paging 3 append
  behavior, documented filters, stable item keys, refresh/retry, and cached
  offline presentation.
- Cached-first series detail refresh with cover, title and alternatives,
  description, type, status, genres/categories, authors/artists, publishers,
  latest chapter when supplied, and graceful missing-field handling.
- Localized loading, empty, recoverable-error, retry, refresh, cached/offline,
  and missing-image states. User-visible strings are resources.
- Debug and minified release packaging, Room schema export, Spotless formatting,
  Android lint configuration, 28 passing JVM tests, and nine passing device
  tests across four instrumented classes.
- A read-only CI workflow with immutable action SHAs, JDK 17, exact Android SDK
  packages, wrapper validation/basic Gradle caching, the complete host gate,
  and independent compact/large emulator jobs.
- Android SDK Platform 36 revision 2, Build Tools 35.0.0, and platform-tools
  37.0.0 plus Emulator 36.6.11 and the API 36 Google Play x86_64 image are
  installed under the ignored persistent `.android-sdk`. The current ignored
  `local.properties` points this checkout there, while `README.md` documents a
  host-level persistent installation that clean checkouts can discover through
  `ANDROID_HOME` without any committed local path.

## 4. Current task and exact implementation status

Plan 001 milestones 1 through 3 are complete. `docs/PLANS.md` indexes six numbered
plans under `docs/plans/`, each with the confirmed contracts, existing-code
entry points, architecture, database impact, milestones, testing requirements,
progress, decisions, discoveries, remaining work, and revision history needed
by a new agent. Plan 001 Milestone 4 is active; plan 002 is blocked on the
official login response shape, and plan 003 depends on it. Plan 004 permits only
its public-access verification while authentication is blocked.

Milestone 2 added `.github/workflows/ci.yml`, updated `README.md`, and refreshed
the living handoff. Milestone 3 made narrow test-only determinism corrections,
added saved-state/offline and on-device Room persistence coverage, provisioned
independent Pixel 2/Pixel Tablet AVDs, and completed the manual resilience and
accessibility matrix without changing production implementation, dependencies,
or the database schema. The workflow now includes both emulator profiles. The
final committed candidate passes clean-checkout host, compact, and large gates
and is published. Hosted run `29332138612` stopped before project tests because
`sdkmanager` was not on the Ubuntu image's command path; a correction based on
that exact image's official manifest is prepared, so no remote CI success is
claimed yet.

The status of the completed search-to-series-details milestone remains:

The private owner-confirmed remote is now
`https://github.com/miguel-rf/Kiroku`. The complete snapshot import commit is
`08d92a20e2a99b3c40857e5e174095d563ddd200`; current `main` descends from its
documentation-only child `6d33474b79db5592bf4b6458e7e9ab54b50ddb23`. A
fresh clone passed `git fsck --full`, matched this complete workspace through
its index and ignore rules, and supplied the adopted Git metadata. Milestone 2's
clean validation candidate was commit
`31d55c41c4947db327654ed9d32536ed4fcb9e7c`, with tree
`cc65ee5e41034ca5a19fda21377bda4d65f37c5d`; its checkout was clean before and
after the complete host gate. The complete Plan 001 candidate is
`5093d60d9092e24f785c82c98c44568e636979f3`, tree
`29a1c1363d236703bbf198577cb7410d9d3fbeea`; its separate clean clone passed
the final host, compact, and large gates and remained Git-clean.

The search-to-series-details milestone is code-complete and verified by all
working-tree quality gates available on this host.

- `MainActivity` launches `KirokuApp`.
- `SearchRoute` and `SeriesRoute(Long)` use `rememberNavBackStack` and
  `NavDisplay` with saveable-state and ViewModel-store decorators.
- Compact and 600 dp+ presentations share navigation/selection state.
- Search and detail use the API-to-Room-to-UI path; network responses are not
  rendered directly.
- A refused TCP connection is explicitly classified as `OFFLINE`; a regression
  test covers that mapping.
- Debug, unsigned minified release, and debug Android-test APKs build.
- JVM tests pass and lint reports no findings.
- The nine-test instrumentation suite passes with zero skips/failures on API 36
  Pixel 2 (1080x1920 at 420 dpi), API 36 Pixel Tablet (2560x1600 at 320 dpi),
  and Pixel 2 at 200% font scale. The final normalized compact command was
  explicitly pinned to `emulator-5556` because an attached dozing Realme is
  also ADB-authorized.
- Manual tablet validation covers live launch/search/detail, cached offline
  content and recovery, actual process death, rotation/resize across the 600 dp
  breakpoint, Back, 200% text, and TalkBack focus/labels.

No implementation for authentication, list management, synchronization,
outbox processing, WorkManager, or releases was added in this milestone.

## 5. Files changed during the completed slice and baseline tasks

The 2026-07-14 planning and Plan 001 infrastructure tasks added or updated:

- `.codex/agents/luna_max_testing.toml`
- `.github/workflows/ci.yml`
- `AGENTS.md`
- `README.md`
- `docs/PLANS.md`
- `docs/PROJECT_PLAN.md`
- `docs/PROJECT_STATE.md`
- `docs/plans/001-reproducible-validation-baseline.md`
- `docs/plans/002-authentication-secure-session.md`
- `docs/plans/003-library-offline-sync.md`
- `docs/plans/004-releases-background-refresh.md`
- `docs/plans/005-settings-adaptive-accessibility.md`
- `docs/plans/006-production-hardening-release.md`

No Android production, Gradle, schema, or dependency file changed during Plan
001. Milestone 3 changed only instrumentation tests: it corrected two
determinism assumptions, expanded the app journey, and added the on-device Room
version-1 reopen check. The remainder of this section records the earlier
completed search-to-series-details slice.

Before Plan 001 milestone 1 restored Git metadata, this change inventory was
established by comparing the working tree with the preserved pre-milestone
snapshot at `/tmp/kiroku-milestone-before.CBmweB`.

Build and configuration:

- `.editorconfig`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `gradle/wrapper/gradle-wrapper.properties`
- `app/build.gradle.kts`
- `app/lint.xml`
- ignored local machine file `local.properties`

Application and resources:

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/kiroku/app/MainActivity.kt`
- `app/src/main/java/com/kiroku/app/KirokuApp.kt`
- `app/src/main/java/com/kiroku/app/core/network/NetworkModule.kt`
- `app/src/main/java/com/kiroku/app/core/network/MangaUpdatesRemoteDataSource.kt`
- `app/src/main/java/com/kiroku/app/feature/search/SearchScreen.kt`
- `app/src/main/java/com/kiroku/app/feature/search/SearchViewModel.kt`
- `app/src/main/java/com/kiroku/app/feature/series/SeriesScreen.kt`
- `app/src/main/java/com/kiroku/app/feature/series/SeriesViewModel.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- deleted `app/src/main/res/xml/network_security_config.xml`; the manifest now
  disables cleartext directly for all supported API levels.

Spotless also normalized these existing Kotlin files without changing their
intended behavior:

- `core/common/Clock.kt`, `core/common/MarkdownPlainText.kt`
- `core/database/DatabaseConverters.kt`, `KirokuDatabase.kt`,
  `SearchEntities.kt`, `SeriesDao.kt`, `SeriesDetailRecord.kt`, and
  `SeriesEntities.kt`
- `core/designsystem/FeedbackComponents.kt`, `KirokuTheme.kt`
- `core/model/SearchSpec.kt`, `Series.kt`
- `core/network/AppFailure.kt`, `MangaUpdatesApi.kt`,
  `MangaUpdatesDtos.kt`, and `NetworkMonitor.kt`
- `data/mapper/SeriesMappers.kt`
- `data/repository/OfflineFirstCatalogueRepository.kt` and
  `SeriesSearchRemoteMediator.kt`
- `feature/series/SeriesUiModel.kt`

Generated schema and tests:

- `app/schemas/com.kiroku.app.core.database.KirokuDatabase/1.json`
- all files under `app/src/test/`, including the two JSON fixtures
- `app/src/androidTest/java/com/kiroku/app/KirokuAppJourneyTest.kt`
- `app/src/androidTest/java/com/kiroku/app/core/database/KirokuDatabaseDeviceTest.kt`
- `app/src/androidTest/java/com/kiroku/app/feature/search/SearchScreenTest.kt`
- `app/src/androidTest/java/com/kiroku/app/feature/series/SeriesScreenTest.kt`

Documentation updated to match the verified state:

- `README.md`
- `docs/PROJECT_STATE.md`
- `docs/PROJECT_PLAN.md` (the phase roadmap)
- `docs/ARCHITECTURE.md`
- `docs/API_NOTES.md`
- `docs/SECURITY.md`
- `docs/TESTING.md`
- `docs/DEPENDENCIES.md`

## 6. Important API findings and assumptions

- Confirmed base URL: `https://api.mangaupdates.com/v1/`.
- This milestone implements only documented `POST /series/search` and
  `GET /series/{id}` operations.
- Search sends `search`, `stype = title`, optional documented `type` and
  `filters`, `page`, and `perpage = 25`. The response supplies `total_hits`,
  `page`, `per_page`, and result records.
- A read-only live request for `perpage = 2` returned 25 records and
  `per_page = 25`, with `total_hits = 10,000`. The allowed bounds and whether
  10,000 is a cap remain undocumented.
- A live result included series ID `55,099,564,912`, so IDs remain `Long`.
- Live search/detail responses confirmed nullable or absent descriptions,
  ratings, image URLs/dimensions, update strings, year, and latest chapter even
  where the simple OpenAPI schema is less explicit.
- Detail responses can contain Markdown and very large category collections.
  Kiroku stores the source text and renders a safe plain-text presentation.
- Errors use `status`, `reason`, and optional arbitrary/validation `context`.
  HTTP status mapping distinguishes offline, timeout, authentication,
  permission, validation, not found, rate limit, server, serialization, and
  unknown failures. Standard numeric `Retry-After` is preserved when present.
- No numeric rate limit is documented. Kiroku debounces, caches in Room, avoids
  duplicate/speculative requests, and does not invent a quota.
- Login declares bearer JWT auth but returns an unrestricted `context` object;
  the token and expiry field names are unknown. Cookie refresh is not assumed
  to refresh an Android bearer token.
- No authenticated, mutating, list, or release request has been issued.

## 7. Database schema and migration status

`KirokuDatabase` is version 1 with `exportSchema = true`. The generated schema
is checked in at
`app/schemas/com.kiroku.app.core.database.KirokuDatabase/1.json`.
Its identity hash is `0c7bbedd114167b238c5c1d2aad91db8`.

Version 1 has ten tables:

1. `series_summaries`
2. `series_details`
3. `series_alternative_titles`
4. `series_categories`
5. `series_contributors`
6. `series_publishers`
7. `search_queries`
8. `search_results`
9. `search_remote_keys`
10. `recent_searches`

Foreign keys, indexes, ordered child rows, and result replacement behavior are
covered by Room/repository tests. Version 1 is the initial schema, so no
migration exists or is required yet. No destructive migration fallback is
configured. A migration fixture and test must be added with version 2 before
that version ships. Library, progress, outbox, account, and release tables do
not exist.

## 8. Tests that exist

The JVM suite has 28 tests across ten classes, all passing:

- `KirokuAppTest` (2)
- `SearchSpecTest` (2)
- `MangaUpdatesDtoTest` (3)
- `MangaUpdatesRemoteDataSourceTest` (5)
- `KirokuDatabaseTest` (3)
- `SeriesMappersTest` (3)
- `OfflineFirstCatalogueRepositoryTest` (2)
- `SeriesSearchRemoteMediatorTest` (3)
- `SearchViewModelTest` (3)
- `SeriesViewModelTest` (2)

Coverage includes realistic JSON and large IDs, mapping, ordered Room data,
cache replacement, mediator refresh/append, repository cache behavior,
debounce/state transitions, HTTP request shape, rate limits, malformed JSON,
timeout, refused connection, compact navigation, and adaptive root decisions.

Four instrumentation classes contain nine tests:

- `KirokuAppJourneyTest`
- `KirokuDatabaseDeviceTest`
- `SearchScreenTest`
- `SeriesScreenTest`

All nine pass on the independent compact and large API 36 emulator profiles and
again on compact at 200% font scale. Coverage includes screen states/actions,
compact Back, large list/detail selection, saved-state restoration, cached
offline detail with retry recovery, and real Room version-1 creation/reopen with
ten tables and the expected identity hash. Automated device tests use fakes and
do not call the production service.

## 9. Commands actually run and results

Living-plan authoring and validation on 2026-07-14:

- The configured Context7 server resolved `/androidx/androidx` and
  `/tink-crypto/tink`; Room, WorkManager, DataStore, and Tink documentation
  queries returned successfully. Mixed-era results are recorded as an
  implementation-time version-specific verification requirement rather than
  copied as current APIs.
- The official OpenAI ExecPlan guide was inspected before creating
  `docs/PLANS.md` and the six numbered plans.
- `rtk git status --short --branch` exited 128 and `rtk git diff --stat` exited
  129 because this directory is still not a usable Git worktree.
- A `jq -e` contract-presence check confirmed OpenAPI 3.0.0/API 1.0.0 and the
  account-login, list-update, and recent-release operations; it exited 0 with
  `true`.
- `rg` structural checks found all twelve user-requested sections in each of
  the six plans, plus `Outcomes & Retrospective` and a revision note in every
  plan.
- Snapshot-based `diff -qr` reported only the intended planning files.
  `git diff --no-index --check` returned the expected difference status with no
  whitespace-error output for docs, `AGENTS.md`, or README.
- The first `./gradlew spotlessCheck --no-daemon --console=plain` attempt exited
  1 because the sandbox could not write the existing Gradle wrapper lock under
  `~/.gradle`. The approved host-cache rerun succeeded in 13 seconds; all four
  Spotless tasks were up-to-date.

Plan 001 milestone 1 provenance restoration on 2026-07-14:

- Pre-adoption inspection found no recoverable local metadata: `.git` was empty
  and `rtk git status --short --branch` exited 128.
- The non-generated working tree was preserved at
  `/tmp/kiroku-pre-provenance-20260714T082846Z.tar.gz`; its SHA-256 is
  `76e26d989feeaee18aeb24e15589de42b7092c433c489b2c8cd0cb718a2a8dc5`.
- `gh repo clone miguel-rf/Kiroku /tmp/kiroku-trusted-20260714 -- --branch main
  --single-branch` completed. The clone checked out
  `6d33474b79db5592bf4b6458e7e9ab54b50ddb23`, and `git fsck --full` exited 0.
- Evaluating the clone's Git directory and index against
  `/home/miguel/Kiroku` returned clean `main...origin/main` status with all
  untracked files included, proving the tracked tree matched and local extras
  were ignored as intended.
- After adopting the verified clone metadata, normal
  `rtk git status --short --branch` succeeded from the repository root at
  `main...origin/main`; `git rev-parse --show-toplevel`, `git rev-parse HEAD`,
  and `git fsck --full` all exited 0.

Plan 001 milestone 2 reproducible toolchain and host CI on 2026-07-14:

- Official primary documentation was re-audited before editing. The current
  releases selected and pinned by full commit SHA were checkout 7.0.0,
  setup-java 5.5.0, and Gradle actions 6.2.0. GitHub's secure-use guidance
  identifies a full SHA as the immutable action reference. Android's
  `sdkmanager` documentation confirms the package-install and licence commands.
  Gradle's version 6 documentation led to explicitly selecting the open-source
  `basic` cache provider instead of the default enhanced component with separate
  terms.
- `yq '.' .github/workflows/ci.yml` exited 0 and preserved the expected event,
  permission, concurrency, runner, and job structure. Focused `rg` scans found
  exactly three `uses:` entries and confirmed that each uses a 40-character SHA.
- The implementation diff plus the new workflow and pre-result documentation
  was committed only inside `/tmp/kiroku-m2-stage.LQRcps`, producing temporary
  validation commit
  `31d55c41c4947db327654ed9d32536ed4fcb9e7c` and tree
  `cc65ee5e41034ca5a19fda21377bda4d65f37c5d`. Cloning that candidate to
  `/tmp/kiroku-m2-clean.QUMVB8` produced an empty-build checkout with clean Git
  status and no `local.properties`, `.gradle`, root `build`, or `app/build`.
- The first required Gradle invocation stopped before project tasks because the
  filesystem sandbox could not write the existing wrapper lock under
  `~/.gradle`. The approved rerun used the host cache and ran the exact command:

      ./gradlew spotlessCheck testDebugUnitTest lintDebug assembleDebug \
          assembleDebugAndroidTest assembleRelease --no-daemon --console=plain

  It completed with `BUILD SUCCESSFUL in 25s`; 148 tasks were actionable, with
  68 executed, 78 restored from cache, and two up-to-date. The only packaging
  notice was the already-known unstripped prebuilt
  `libandroidx.graphics.path.so`.
- Because the canonical gate restored `testDebugUnitTest` from Gradle's build
  cache, `./gradlew testDebugUnitTest --rerun-tasks --no-build-cache --no-daemon
  --console=plain` was also run. It completed with `BUILD SUCCESSFUL in 1m 3s`
  and all 33 scheduled tasks executed. The regenerated ten JUnit suites contain
  28 tests, zero skipped, zero failures, and zero errors.
- Lint reports `No issues found.` The debug APK is 20,957,237 bytes, the debug
  Android-test APK is 1,126,143 bytes, and the unsigned minified release APK is
  1,986,704 bytes. The checkout remained Git-clean after both builds. Room is
  still version 1 with ten entities and identity hash
  `0c7bbedd114167b238c5c1d2aad91db8`.
- `sdkmanager --list_installed` in the clean checkout's configured environment
  confirmed Build Tools 35.0.0, Platform 36 revision 2, and platform-tools
  37.0.0. The GitHub-hosted workflow itself has not run because these changes
  have not been published; no remote result is claimed.

Plan 001 milestone 3 device and accessibility validation on 2026-07-14:

- Installed Emulator 36.6.11 and the API 36 Google Play x86_64 system image in
  ignored `.android-sdk`, then created Pixel 2 and Pixel Tablet AVDs. Their
  observable configurations are 1080x1920 at 420 dpi (about 411 dp wide) and
  2560x1600 at 320 dpi (1280 dp wide).
- The original six tests first exposed three test-harness defects on the compact
  AVD: ambiguous title nodes and a below-viewport LazyColumn assertion. Narrow
  matcher/scroll corrections made the original suite pass 6/6.
- Expanded coverage passed 9/9 with zero skips/failures on both AVDs and passed
  9/9 again on compact with `font_scale=2.0`. The added tests prove saved-state
  restoration, cached offline detail plus retry recovery, and real Room
  version-1 creation/reopen with ten tables and identity hash
  `0c7bbedd114167b238c5c1d2aad91db8`.
- Manual tablet validation used one small read-only One Piece query. Cached
  search/detail remained visible offline, retry recovered after connectivity,
  selected detail survived actual process death, and rotation/resize crossed
  between 1280 dp two-pane and 590 dp compact layouts. Back, 200% text, and
  TalkBack focus/labels were also observed. No credentials were used.

Plan 001 milestone 4 candidate validation on 2026-07-14:

- Added the two-entry API 36 Google APIs x86_64 emulator matrix to
  `.github/workflows/ci.yml`, using Pixel 2/Pixel Tablet profiles, disabled
  animations, Ubuntu KVM, and android-emulator-runner 2.37.0 pinned to full SHA
  `e89f39f1abbbd05b1113a29cf4db69e7540cae5a`. `yq` parsed the workflow and all
  seven `uses:` entries are 40-character SHAs.
- The exact host gate completed with `BUILD SUCCESSFUL in 31s`; retained JUnit
  XML contains 28 tests with zero failures/errors/skips, lint reports `No
  issues found.`, and the debug, debug Android-test, and unsigned minified
  release APKs exist.
- An unfiltered device command selected both the interactive AVD and an attached
  dozing Realme. The AVD completed 9/9 while seven Realme cases failed before
  assertions with no Compose hierarchy, so that aggregate command failed. The
  corrected `ANDROID_SERIAL=emulator-5556` rerun completed with `BUILD
  SUCCESSFUL in 31s`; its XML records 9 tests, zero failures/errors/skips.
- Candidate `5093d60d9092e24f785c82c98c44568e636979f3`, tree
  `29a1c1363d236703bbf198577cb7410d9d3fbeea`, was cloned into
  `/tmp/kiroku-plan001-clean.kYba8o` without local project state. The exact host
  gate completed in 19 seconds; a forced uncached JVM run completed in 50
  seconds with all 28 tests green. Lint, APK integrity, unchanged Room schema,
  `git fsck --full`, and post-run Git cleanliness all passed.
- The same clean checkout completed all 9 instrumented tests with zero
  failures/errors/skips on separately booted API 36 Pixel 2 (1080x1920, 420
  dpi, about 411 dp) and Pixel Tablet (2560x1600, 320 dpi, 1280 dp) AVDs in 42
  and 35 seconds. The generated XML timestamps are 12:11:28Z and 12:14:38Z.
- Published hosted run `29332138612` failed all three jobs before Gradle or
  device execution with `sdkmanager: command not found`. Its exact Ubuntu 24.04
  image manifest records SDK root `/usr/local/lib/android/sdk` and Android
  Command Line Tools 12.0; the corresponding image-build source places it under
  `cmdline-tools/latest/bin`. The workflow now declares that root, invokes the
  binary directly, and exports its path for the emulator action. A successful
  hosted result remains pending and is not claimed here.

GitHub publication on 2026-07-14:

- The GitHub plugin authenticated as `miguel-rf`, confirmed admin/push access
  to the newly created private `miguel-rf/Kiroku` repository, and found it
  empty with default branch `main`.
- `rg --files --hidden -g '!.git/**'` identified 94 non-ignored files totaling
  approximately 1.2 MiB. `local.properties`, SDK/cache/build directories, and
  other `.gitignore` entries were absent. No file exceeded 50 MiB, and a focused
  credential-pattern scan returned no matches.
- The plugin created initial README commit
  `f5d45e8d002d557fd05aa1b7b03a742d2df90038`, then created blobs for all 94
  files, a complete tree, and full snapshot commit
  `08d92a20e2a99b3c40857e5e174095d563ddd200`. It fast-forwarded `main` without
  force.
- Local `git hash-object` results matched all 94 plugin-returned blob SHAs with
  zero mismatches. GitHub reported 93 added files in the full snapshot commit
  because README was already present. Remote SHA checks passed for README,
  `openapi.json`, the Gradle wrapper JAR, `KirokuApp.kt`, the Room schema, and
  the production-hardening plan.

Repository/diff inspection:

- `rtk git status --short --branch` exited 128: not a Git repository.
- `rtk git diff --stat` exited 129 for the same reason.
- A pre-milestone snapshot was created at
  `/tmp/kiroku-milestone-before.CBmweB`.
- `diff -qr` and full `diff -ruN`/whitespace-insensitive sections against that
  snapshot were inspected. The final inventory excludes generated build
  directories and ignored `local.properties`.

Initial SDK provisioning before the persistent emulator setup:

- `/tmp/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root=/tmp/android-sdk --licenses`
  accepted all seven displayed agreements with explicit user authorization.
- `sdkmanager --sdk_root=/tmp/android-sdk "platforms;android-36" "build-tools;35.0.0" "platform-tools"`
  completed successfully.
- `sdkmanager --list_installed` confirmed Platform 36 revision 2, Build Tools
  35.0.0, and platform-tools 37.0.0.
- `adb version` reported 37.0.0-14910828; `aapt2 version` reported
  2.19-11948202.
- At that earlier checkpoint, `adb devices -l` found `6c604f95 unauthorized`;
  connected tests had not yet run. The later Milestone 3 evidence above
  supersedes that availability state.

Build and correction history:

- `./gradlew tasks` succeeded after the Gradle 8.13 wrapper downloaded.
- The first `compileDebugKotlin` failed because no SDK location was configured.
- The next compile failed AAR metadata checks because AndroidX Core 1.19.0 and
  Lifecycle 2.11.0 require the API 37/AGP 9.1 toolchain. Context7 and primary
  Android documentation were checked; stable compatible pins 1.18.0 and 2.10.0
  were applied.
- Subsequent compile attempts exposed and then resolved source-level adaptive,
  Coil extension, and JVM signature issues.
- `spotlessApply testDebugUnitTest` succeeded after the first test increment.
- The first `assembleDebug assembleDebugAndroidTest` failed on Compose test API
  imports and exposed a Kotlin 2.4 R8 warning selected by Coil 3.5.0.
  Compose tests were corrected and Coil was pinned to compatible 3.4.0.
- `spotlessApply assembleDebug assembleDebugAndroidTest` then succeeded.
- Initial `lintDebug` succeeded with 24 warnings; each actionable warning was
  fixed or narrowly documented. The current report is `No issues found.`
- A combined command that scheduled `clean` together with Spotless failed when
  `clean` deleted an Android resource intermediate while Spotless snapshotted
  it. This was a task-scheduling race, not a source/test failure.
- Standalone `./gradlew clean` succeeded. A subsequent clean-workspace gate
  `spotlessCheck testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`
  succeeded in 41 seconds.
- The final network-error audit added explicit `ConnectException` mapping and
  its 28th test.
- The first final `spotlessApply` attempt inside the filesystem sandbox failed
  because the Gradle wrapper lock under `~/.gradle` was read-only. The same
  command was rerun with approved host-cache access and succeeded in 12 seconds.
- Final command:

      ./gradlew spotlessCheck testDebugUnitTest lintDebug assembleDebug \
          assembleDebugAndroidTest assembleRelease --no-daemon --console=plain

  Result: `BUILD SUCCESSFUL in 1m 52s`; 148 actionable tasks, 11 executed and
  137 up-to-date. The generated artifacts are a 20.0 MiB debug APK, a 1.1 MiB
  debug Android-test APK, and a 1.9 MiB unsigned minified release APK. The only
  packaging notice was that the prebuilt `libandroidx.graphics.path.so` could
  not be stripped and was packaged unchanged.

Verification:

- The ten JUnit XML suites report 28 tests, 0 skipped, 0 failures, 0 errors.
- `app/build/reports/lint-results-debug.txt` reports `No issues found.`
- `aapt2` manifest inspection confirmed compile/target SDK 36, min SDK 23,
  launcher activity export, and `usesCleartextTraffic=false`.
- XML and generated schema JSON validation succeeded.
- Source scans found no unfinished-work comment markers and no implemented
  authentication/list/release endpoint beyond generic error terminology.

## 10. Known failures, blockers, and technical debt

Plan 001 closing verification:

- Local and clean-candidate host, compact, and large checks are green, as are
  large-font, manual resilience, and accessibility checks. The corrected
  workflow must be published and complete a successful hosted
  host/compact/large run before Plan 001 closes.
- The attached Realme can remain dozing behind its lock/notification surface.
  Unpinned connected-test commands therefore discover a non-interactive target;
  local acceptance runs must set `ANDROID_SERIAL` to the intended emulator.

Later-phase blockers:

- Authentication and therefore authenticated library work remain blocked on
  the undocumented login success token/expiry shape. This did not block the
  public search/detail milestone.

Technical debt and remaining validation:

- The complete host/device workflow is present and structurally validated, but
  no hosted pass is claimed until the candidate is published and all three jobs
  finish successfully.
- The slice has explicit refresh/retry actions but no pull-to-refresh gesture.
- No database migration test exists because there is only schema version 1;
  version 2 must introduce the first migration fixture.
- The release APK is unsigned, as expected for a local release build. Signing,
  distribution automation, baseline profiles, macrobenchmarks, and production
  release setup belong to Phase 5.

## 11. Next three concrete implementation steps

1. Inspect and publish the Ubuntu SDK command-path correction.
2. Monitor and fix the hosted host/compact/large workflow until every job is
   green.
3. Record the immutable commit and hosted run evidence in the plan index,
   testing guide, this handoff, and Plan 001; run the final completion audit and
   close the plan only when no requirement remains.

## 12. Decisions that must not be revisited without new evidence

- Use only documented MangaUpdates endpoints and fields. Do not scrape, guess
  token fields, invent refresh behavior, or add unsupported idempotency headers.
- Kiroku remains a catalogue/tracking app, never a chapter reader.
- Keep Room as the source of truth and retain distinct DTO, entity, domain, and
  UI models.
- Keep MangaUpdates identifiers as `Long`.
- Keep Paging 3 for search, a 25-record request page, and the narrow
  `RemoteMediator` experimental opt-in.
- Keep the first version in one app module with a manual `AppContainer`; add
  modules or Hilt only with concrete complexity or build evidence.
- Keep stable Navigation 3 and stable adaptive window information with the
  explicit two-pane host. Do not adopt alpha adaptive navigation or
  experimental pane scaffolds for convenience.
- Keep Core 1.18.0, Lifecycle 2.10.0, Coil 3.4.0, Kotlin 2.3.21, and AGP 8.13.2
  together unless a documented compatible toolchain migration is planned and
  verified through Context7 and primary release notes.
- Keep CI actions immutable by full commit SHA and re-audit the official release
  and security documentation before changing those pins. Retain the basic
  Gradle cache provider unless a deliberate, documented licensing decision
  changes it.
- Reserve WorkManager for durable operations; use ordinary coroutines for
  foreground search and detail refresh.
- Do not implement auth or add Tink before the login contract is confirmed and
  Android security guidance is re-audited.
- Never store the MangaUpdates password or log credentials, tokens,
  authorization headers, cookies, request/response bodies, or account data.
- Do not add destructive Room migration fallback. Export and test every future
  schema transition.
- Never claim a build or test passed unless the exact command actually ran and
  succeeded; compiled instrumented tests are not passed instrumented tests.
