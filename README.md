# Kiroku

Kiroku is an unofficial Android catalogue and tracking client for
[MangaUpdates](https://www.mangaupdates.com/). It is designed for finding
series, reviewing catalogue metadata, managing MangaUpdates lists, and tracking
release information. It is not a manga reader and does not download or display
manga chapters.

MangaUpdates is not affiliated with or responsible for Kiroku. Catalogue data
is provided by MangaUpdates and is cached in accordance with its API acceptable
use policy.

## Project status

The repository began with the official MangaUpdates OpenAPI contract only.
Phase 0 discovery, the foundation, and the first offline-first
search-to-series-details slice are complete on all host-executable gates. The
app has compact and adaptive list/detail navigation, Room-backed search/detail
caching, and deterministic JVM/integration tests. Instrumented tests compile
but still need an authorized device or emulator. See
[docs/PROJECT_STATE.md](docs/PROJECT_STATE.md) for exact evidence and
[docs/PROJECT_PLAN.md](docs/PROJECT_PLAN.md) for roadmap status. Remaining work
is organized as self-contained living ExecPlans in
[docs/PLANS.md](docs/PLANS.md); start with the earliest uncompleted numbered
plan under `docs/plans/`.

All foundation library choices were re-audited through the project-configured
Context7 server after it became available. The resolved documentation sources,
stable-version checks, experimental API policy, and resulting build corrections
are recorded in [docs/DEPENDENCIES.md](docs/DEPENDENCIES.md).

## Prerequisites

- JDK 17 or newer; Gradle runs with the checked-in wrapper.
- Android SDK Platform 36 and Android SDK Build Tools 35.0.0 or newer.
- An Android device or emulator running API 23 or newer for instrumented tests.
- Network access to api.mangaupdates.com for live app use. Automated tests use
  deterministic local responses and do not require the production API.

No API key is required for public MangaUpdates operations. Account credentials
must never be added to project files.

## Build and test

From the repository root:

    ./gradlew assembleDebug
    ./gradlew testDebugUnitTest
    ./gradlew lintDebug
    ./gradlew spotlessCheck
    ./gradlew assembleRelease

When an emulator or device is available:

    ./gradlew connectedDebugAndroidTest

The authoritative test matrix and environment limitations are recorded in
[docs/TESTING.md](docs/TESTING.md).

## Architecture

Kiroku uses a single application module for the first production version.
Packages are split into core infrastructure, data coordination, and features.
Compose screens send actions to ViewModels; ViewModels expose immutable state;
repositories coordinate the MangaUpdates API and Room; and Room is the UI's
source of truth for cached catalogue data.

Search uses Paging only because the confirmed API can report at least 10,000
matches. A Room-backed RemoteMediator keeps paging, cancellation, refresh, and
offline behavior consistent. Series details are read from Room and refreshed
with a foreground coroutine.

Dependency injection starts with a manual AppContainer. The current graph is
small, has application-lifetime dependencies only, and does not justify Hilt.
See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## API and security

The confirmed API base URL is:

    https://api.mangaupdates.com/v1/

Only operations present in the checked-in OpenAPI contract are eligible for
implementation. Contract findings and unresolved inconsistencies are in
[docs/API_NOTES.md](docs/API_NOTES.md).

Public requests carry no credentials. Future session tokens will be encrypted
with authenticated encryption backed by Android Keystore; passwords will exist
only long enough to submit the login request. Security decisions and logging
rules are in [docs/SECURITY.md](docs/SECURITY.md).

## Known limitations

- Authentication and library synchronization are later phases. The checked-in
  contract does not define the login response context field that contains the
  bearer token, so that field must be confirmed before implementation.
- No numeric API rate limit is published. Kiroku caches aggressively and avoids
  speculative or duplicate requests.
- The current host has a temporary project SDK under `/tmp/android-sdk`, but no
  emulator. The attached physical device is unauthorized, so the packaged
  Compose tests have not executed. Context7 MCP 3.2.3 is pinned in the project
  configuration and has been used for version-sensitive library decisions. If
  first added while a Codex CLI or IDE session is already running, that session
  may need a restart for tool discovery. Verification results are reported
  rather than inferred.
