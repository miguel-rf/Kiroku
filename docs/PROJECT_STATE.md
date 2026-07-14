# Kiroku project state

Last updated: 2026-07-14

This is the authoritative, self-contained handoff. It distinguishes verified
build/test evidence from work that still needs a physical device, an emulator,
or additional MangaUpdates contract evidence.

## 1. Current project objective

Build Kiroku, a production-quality, unofficial Android catalogue, tracking,
and release-management client that uses only the documented MangaUpdates API.
Kiroku is not a manga reader and must not scrape MangaUpdates or fetch,
download, or display copyrighted manga chapters.

The completed product milestone is the first offline-first vertical slice:

    Search -> MangaUpdates API -> Room -> repository -> ViewModel -> Compose
           -> Navigation 3 -> series details

The milestone implementation and every host-executable quality gate are now
complete. Instrumented tests compile and package, but their execution remains
unavailable because the only attached Android device is unauthorized and no
emulator is installed. Authentication, library synchronization, and releases
have not been started.

The current repository task was planning-only: six self-contained living
ExecPlans now cover every remaining validation, authentication, library,
release, settings/accessibility, and production-hardening milestone. Their
convention and dependency order are in `docs/PLANS.md`. No application source,
schema, dependency, or test changed during that planning task.

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
  Android lint configuration, 28 passing JVM tests, and three compiled Compose
  instrumented test classes.
- Android SDK Platform 36 revision 2, Build Tools 35.0.0, and platform-tools
  37.0.0 are installed under `/tmp/android-sdk`; all seven displayed Google SDK
  licenses were accepted with the user's explicit authorization. The ignored
  `local.properties` points this checkout to that SDK.

## 4. Current task and exact implementation status

The living-plan authoring task is complete. `docs/PLANS.md` indexes six numbered
plans under `docs/plans/`, each with the confirmed contracts, existing-code
entry points, architecture, database impact, milestones, testing requirements,
progress, decisions, discoveries, remaining work, and revision history needed
by a new agent. Plan 001 is the next executable plan; plan 002 is blocked on the
official login response shape, and plan 003 depends on it. Plan 004 permits only
its public-access verification while authentication is blocked.

No production implementation was changed by this task. The status of the
completed search-to-series-details milestone remains:

The search-to-series-details milestone is code-complete and verified by all
quality gates available on this host.

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
- Compose instrumentation is compiled and packaged but has not run: `adb`
  reports device `6c604f95` as `unauthorized`, and the installed SDK has no
  emulator or system image.

No implementation for authentication, list management, synchronization,
outbox processing, WorkManager, or releases was added in this milestone.

## 5. Files changed during the completed slice and current planning task

The 2026-07-14 planning-only task added or updated:

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

No Android application, Gradle, schema, or test file changed during this
planning task. The remainder of this section records the earlier completed
search-to-series-details slice.

Git metadata is absent or unusable, so the change inventory was established by
comparing the working tree with the preserved pre-milestone snapshot at
`/tmp/kiroku-milestone-before.CBmweB`.

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

Three Compose instrumentation classes also exist:

- `KirokuAppJourneyTest`
- `SearchScreenTest`
- `SeriesScreenTest`

Their Kotlin, resources, dex, and APK package successfully. They have not run
on a device, so they are not counted as passing tests.

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

Repository/diff inspection:

- `rtk git status --short --branch` exited 128: not a Git repository.
- `rtk git diff --stat` exited 129 for the same reason.
- A pre-milestone snapshot was created at
  `/tmp/kiroku-milestone-before.CBmweB`.
- `diff -qr` and full `diff -ruN`/whitespace-insensitive sections against that
  snapshot were inspected. The final inventory excludes generated build
  directories and ignored `local.properties`.

SDK provisioning:

- `/tmp/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root=/tmp/android-sdk --licenses`
  accepted all seven displayed agreements with explicit user authorization.
- `sdkmanager --sdk_root=/tmp/android-sdk "platforms;android-36" "build-tools;35.0.0" "platform-tools"`
  completed successfully.
- `sdkmanager --list_installed` confirmed Platform 36 revision 2, Build Tools
  35.0.0, and platform-tools 37.0.0.
- `adb version` reported 37.0.0-14910828; `aapt2 version` reported
  2.19-11948202.
- `adb devices -l` found `6c604f95 unauthorized`; connected tests were not run.

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

Blocking external verification:

- This directory is not a usable Git worktree. A trusted `git status`, Git
  diff, clean-checkout build, and commit cannot be produced until repository
  metadata is restored. The temporary snapshot is not a durable substitute.
- Compose instrumented tests cannot execute until the attached device accepts
  this host's ADB key or an API 23+ emulator/system image is installed.

Later-phase blockers:

- Authentication and therefore authenticated library work remain blocked on
  the undocumented login success token/expiry shape. This did not block the
  public search/detail milestone.

Technical debt and remaining validation:

- Run the three Compose test classes on compact and tablet/foldable form
  factors, including TalkBack and large-font manual checks.
- The slice has explicit refresh/retry actions but no pull-to-refresh gesture.
- No database migration test exists because there is only schema version 1;
  version 2 must introduce the first migration fixture.
- The release APK is unsigned, as expected for a local release build. Signing,
  CI, baseline profiles, macrobenchmarks, and production release setup belong
  to Phase 5.

## 11. Next three concrete implementation steps

1. Execute plan 001: restore the repository's real Git provenance, establish a
   persistent documented SDK/CI path, and repeat the complete host gate from an
   owner-confirmed clean checkout.
2. Authorize the attached device or provision a pinned emulator and complete
   plan 001's compact/600 dp+ instrumented, TalkBack, large-font, rotation,
   resize, and offline/restart validation.
3. Obtain official login token/expiry response evidence and update plan 002;
   only then re-audit the exact stable Tink/Keystore API and begin authentication.

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
