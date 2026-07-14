# Harden, benchmark, and prepare the production release

This ExecPlan is a living document. Keep `Progress`, `Unexpected discoveries`,
`Decision log`, `Outcomes & Retrospective`, and the revision note current while
work proceeds. Maintain it in accordance with `docs/PLANS.md`.

## Goal

Convert the feature-complete Kiroku application into a releasable, measurable,
and supportable production build. Close security, privacy, migration,
performance, accessibility, adaptive-layout, build reproducibility, licence,
and documentation gaps; add Macrobenchmark and Baseline Profile coverage; and
prove the complete definition of done from a clean checkout.

Execute this plan only after plans 001 through 005 have completed or explicitly
recorded an unavoidable blocker. This plan must not mask missing core behavior
with release notes, broad warning suppressions, destructive migrations, or
benchmark-only optimizations.

## User-visible behavior

A new user can install a signed production candidate, launch while signed out,
search and open a series, sign in, restore the session after restart, manage
reading lists online or offline, recover failed synchronization, browse cached
releases, configure the app, and log out. The same package is usable on compact
phones and large/foldable windows with accessible loading, empty, offline,
error, retry, and sync states.

Startup and the primary scrolling/navigation journeys are smooth on the agreed
baseline device. Database upgrades preserve catalogue and account state through
every released schema version. Cache maintenance remains bounded. Release
builds contain no debug logging, test hooks, plaintext secrets, development
endpoints, or chapter-reading capability.

Developers can build, test, lint, format, inspect licences, run migration tests,
and produce an unsigned or locally signed release candidate from a clean
checkout using documented commands. Release signing secrets remain external to
the repository.

## Relevant existing code

The Phase 2 baseline is one `:app` module, application ID `com.kiroku.app`,
minimum SDK 23, compile/target SDK 36, Java 17, AGP 8.13.2, Gradle 8.13,
Kotlin 2.3.21, and version `0.1.0`/code 1. The release build already enables
R8 and resource shrinking. Reinspect `gradle/libs.versions.toml`, wrapper
properties, `app/build.gradle.kts`, `proguard-rules.pro`, manifests, backup
rules, and CI after all preceding plans because these values may have changed.

`docs/PROJECT_STATE.md` is the handoff of record. `docs/ARCHITECTURE.md`,
`API_NOTES.md`, `SECURITY.md`, `TESTING.md`, `DEPENDENCIES.md`, README, and the
completed living plans contain feature decisions and actual gate evidence.
Do not rely on the original Phase 2 counts once new tests exist.

The Phase 2 repository has Room schema version 1 at identity hash
`0c7bbedd114167b238c5c1d2aad91db8`. Plans 003 and 004 should add sequential
library/outbox and release-cache migrations, normally ending at version 3.
Inspect the exported schemas and migration declarations rather than assuming
that order. Plans 002 and 005 should have added encrypted session storage and
DataStore settings, which require separate compatibility and backup review.

No Macrobenchmark or Baseline Profile module exists in the Phase 2 baseline.
Creating focused benchmark/profile modules is justified here because Android's
tooling requires separate test/application boundaries and the core journeys are
now stable. This does not authorize splitting product features into many
modules.

## Confirmed API contracts

This plan adds no MangaUpdates endpoint, request field, or authentication
behavior. Audit the implementation against the checked-in `openapi.json` and
`docs/API_NOTES.md`. The only permitted operations are the series search/detail,
account, list, list mutation, and release operations already confirmed in the
completed plans.

Verify that no Retrofit annotation, raw OkHttp URL, WebView, scraper, or chapter
download path exists outside that inventory. Verify every authenticated request
uses documented bearer handling, every public request omits fabricated auth,
every mutation respects the documented five-second 412 delay, and release
volume/chapter strings have not been conflated with integer library progress.

The API defines no numeric request limit, idempotency key, guaranteed bearer
refresh, release-date grammar, or universal per-page bound. Those remain
unknown unless a preceding plan recorded new official evidence. Production
hardening must not turn an unconfirmed behavior into an assumption.

## Architecture

Preserve the settled one-module product architecture, manual `AppContainer`,
Room source of truth, repository boundaries, immutable StateFlow UI state,
Navigation 3 shell, and WorkManager-only durable jobs. Add benchmark/profile
modules solely for Android performance tooling. Introduce Hilt or another DI
framework only if a completed-plan decision log contains measured replacement
or scope complexity; do not migrate DI during hardening for convention.

Perform four explicit audits:

1. Security and privacy: trace username/password/token/account data from UI to
   network and storage; inspect logs, backups, screenshots, crash diagnostics,
   worker input, Room, DataStore, encrypted files, and logout. Validate AEAD
   failure/rotation behavior and Android Keystore use against the exact stable
   Tink implementation. Confirm cleartext remains disabled and release builds
   contain no HTTP logger.
2. Data integrity: enumerate every Room version and migration path, outbox
   transition, account partition, cache-clear transaction, DataStore migration,
   secure-envelope version, and process-death boundary. Never use destructive
   fallback for user data.
3. Performance: measure cold/warm startup, frame timing, recomposition,
   allocations, Paging/Room query plans, image sizing/cache, worker batching,
   and APK size before changing code. Optimize only demonstrated bottlenecks and
   record before/after evidence.
4. Product/accessibility: execute every critical journey on compact and large
   configurations, offline/online transitions, TalkBack, 200% font scale,
   keyboard/D-pad, dark/light/dynamic color, rotation, resize, and process
   recreation. Fix causes rather than adding broad suppressions.

Resolve current stable Baseline Profile, Macrobenchmark, ProfileInstaller, AGP,
and test APIs through Context7 and primary Android documentation before editing
the build. Select versions compatible with the settled AGP/Kotlin/SDK matrix;
do not upgrade the whole toolchain merely to copy a newer sample. Record any
necessary version change in `docs/DEPENDENCIES.md` with compatibility evidence.

Create deterministic performance journeys without production network access or
credentials. Use a benchmark-only/test-only fixture path that seeds realistic
local Room data or a controlled local server and is absent from the production
variant. The benchmark path must not add fake success behavior to production.
Generate Baseline Profiles for startup, search result scrolling, opening series
details, library scrolling, progress controls, and releases. Measure on a
profileable release-like build and verify the generated profile is packaged.

Add durable cache cleanup only if it was not completed earlier. WorkManager may
delete stale orphaned public catalogue/release rows and image cache entries in
bounded transactions under suitable constraints. It must preserve current query
results, user library state, local overrides, pending/failed outbox operations,
settings, and session material. Foreground user-initiated clearing remains a
normal coroutine.

Release signing uses an owner-provided keystore and credentials supplied through
local untracked properties or CI secrets. Do not create, commit, print, or
request a real signing password in a plan or test. Keep an unsigned release
build available for ordinary verification. Document key ownership and rotation
outside version control without exposing values.

## Database changes

Start by inventorying every exported schema JSON and every registered migration.
The expected normal history is version 1 public catalogue, version 2 library and
outbox, and version 3 release cache, but the actual completed plans are
authoritative. Add tests for every adjacent transition and for a direct upgrade
from every previously released version to the current version using Room's
supported migration chain.

No new schema version is planned solely for hardening. If measured performance
requires an index or cleanup metadata, update this plan first with the query
plan evidence, add exactly the next version, write an explicit migration,
export the schema, and test populated upgrades. Never edit an already shipped
schema JSON or use destructive migration to make tests pass.

Validate:

- all foreign keys and expected indexes;
- large `Long` series IDs;
- confirmed-vs-local library overlay and outbox preservation;
- public and account-scoped release cache isolation;
- migration of empty, minimal, and realistically populated old databases;
- downgrade behavior is explicitly unsupported unless the product deliberately
  provides it; and
- cache cleanup cannot cascade into user intent.

DataStore and encrypted-session envelope versions need their own migration/
corruption tests even though they are not Room schemas. Backup/restore tests or
packaged-rule inspection must demonstrate that token material stays excluded
and that restored settings/data cannot be associated with the wrong account.

## Implementation milestones

Milestone 1 freezes and audits the release candidate scope. Read every completed
plan and current handoff, inventory endpoints/dependencies/schemas/routes/jobs,
inspect the complete diff and Git history, run the current gates without
changes, and record all failures. Confirm there are no placeholders, untracked
required files, committed secrets, broad suppressions, or unauthorized
dependencies.

Milestone 2 closes correctness, migration, security, and privacy findings. Add
or repair migration chains, token and backup behavior, account isolation,
outbox recovery, release parsing, R8 rules, error mapping, worker constraints,
and deterministic tests. Run static secret scanning suitable for the repository
and manually inspect release logs/storage. Do not include real credentials in
the scan baseline or output.

Milestone 3 completes cross-device product validation. Execute all critical
journeys on compact phone, tablet/600 dp+, and resizable or foldable profiles;
exercise API 23 and target-era API behavior where feasible; run TalkBack,
keyboard/D-pad, large text/display, dark/light/dynamic color, rotation, process
death, offline/reconnect, and expired-session cases. Fix observed issues and
record device evidence.

Milestone 4 measures and optimizes. Establish startup/frame/query/image/worker
baselines, identify material bottlenecks, make small reviewable fixes, and
capture before/after results. Add benchmark and Baseline Profile modules only
after stable API/version verification. Generate, commit, package, and validate
the profile for the agreed journeys.

Milestone 5 validates production packaging. Exercise minified debug/profile and
release-like variants, inspect APK/AAB manifests and contents, verify R8 mapping
retention policy without committing sensitive artifacts, validate licence
notices, backup rules, network security, versioning, app label/icon, unofficial
notice, and release signing through external secrets. Install and smoke-test the
exact candidate artifact.

Milestone 6 completes reproducibility and documentation. Build from an owner-
confirmed clean checkout in a separate directory, run CI-equivalent and device
gates, compare generated schemas/profiles/licences, and update README,
architecture, API, security, testing, dependency, project-state, changelog or
release notes, and every living plan. Record limitations honestly and complete
the retrospective only after observable acceptance.

## Testing requirements

Retain and pass every DTO, mapper, repository, DAO, migration, authentication,
token-storage, outbox/retry, ViewModel, Turbine, MockWebServer, WorkManager,
Compose, and settings test from earlier plans. Add regression tests for every
hardening fix. Tests use fake clocks, deterministic dispatchers/UUIDs, controlled
responses, temporary stores, and WorkManager test drivers; no arbitrary sleeps,
production network calls, or real credentials.

Run at minimum, adapting task names only when the completed build defines them:

    ./gradlew spotlessCheck testDebugUnitTest lintDebug assembleDebug \
        assembleDebugAndroidTest assembleRelease --no-daemon --console=plain

Run all Room migration tests, licence/report verification, Baseline Profile
generation/verification, and Macrobenchmark tasks explicitly. Run
`connectedDebugAndroidTest` on actual compact and large configurations and run
the benchmark suite on the documented physical or emulator baseline. Record
exact Gradle task names, versions, device model/API/resolution/density, iteration
counts, and raw report locations.

The end-to-end acceptance matrix must prove:

1. signed-out launch;
2. search and series detail online, cached, and after restart;
3. successful and failed login;
4. secure session restoration and expired session;
5. add, move, remove, and progress update;
6. offline mutation, process death, reconnection, reconciliation, and recovery;
7. cached and refreshed releases;
8. settings restoration and safe cache controls;
9. compact, tablet, and resize/fold navigation; and
10. TalkBack, large text, dark/light, and focus behavior.

Inspect lint's generated report for zero findings rather than trusting only a
task exit code. Treat warnings as failures unless a narrow, commented
suppression has documented evidence. Confirm the minified release starts and
completes every reflection/serialization/Room/Tink/worker path. A successful
debug build does not prove release shrinking.

For a clean checkout, provision only the documented JDK/SDK and external signing
inputs, run the wrapper commands, and verify no local generated file or secret
is required. CI and local results must agree. Report any unavailable store,
hardware, signing, or publication action as unavailable, not passed.

## Progress

- [x] (2026-07-14 07:24Z) Captured the Phase 2 build, schema, test, and release-
  variant baseline from `docs/PROJECT_STATE.md` and Gradle configuration.
- [x] (2026-07-14 07:24Z) Reserved benchmark/profile modules until core user
  journeys are stable, as required by the product plan.
- [ ] Complete or formally resolve plans 001 through 005.
- [ ] Audit the complete release-candidate scope, endpoints, dependencies,
  schemas, security boundaries, and quality-gate baseline.
- [ ] Close correctness, migration, security, privacy, and accessibility gaps.
- [ ] Measure performance and add verified Macrobenchmark/Baseline Profiles.
- [ ] Validate minified packaging, notices, backups, signing integration, and
  the exact installable candidate.
- [ ] Pass clean-checkout host/device gates and complete all release/handoff
  documentation.

## Decision log

- Decision: Defer benchmark and Baseline Profile modules until this plan.
  Rationale: Their journeys and build boundaries are useful only after core
  flows stabilize; adding them earlier would profile placeholders.
  Date/Author: 2026-07-14, Codex.
- Decision: Optimize only measured bottlenecks.
  Rationale: The current architecture already uses Room, Paging, sized images,
  and immutable state; speculative tuning would increase risk without evidence.
  Date/Author: 2026-07-14, Codex.
- Decision: Preserve the manual product DI graph during hardening.
  Rationale: A late framework migration is not release hardening unless prior
  implementation evidence proves the current graph unmanageable.
  Date/Author: 2026-07-14, Codex.
- Decision: Keep signing material external and maintain unsigned verification.
  Rationale: Builds and tests must remain reproducible without committing a
  private key, while owners retain control of production identity.
  Date/Author: 2026-07-14, Codex.
- Decision: No new API behavior is authorized in the hardening phase.
  Rationale: Release pressure must not bypass the official-contract gate.
  Date/Author: 2026-07-14, Codex.

## Unexpected discoveries

- Observation: The initial release build is already minified and shrinkable,
  but it predates authenticated storage, workers, and migrations.
  Evidence: Phase 2 `app/build.gradle.kts` enables R8/resource shrinking; those
  later code paths do not yet exist and require a fresh release-variant audit.
- Observation: The original repository state could not prove a clean checkout
  or execute device tests.
  Evidence: plan 001 records unusable Git metadata, a temporary SDK, an
  unauthorized device, and no emulator. This plan cannot waive those gates.
- Observation: Context7 documentation queries can mix version eras unless they
  are constrained.
  Evidence: the 2026-07-14 AndroidX/Tink queries included older and pre-release
  snippets. Benchmark/security APIs must be resolved against selected stable
  versions at implementation time.

## Outcomes & Retrospective

No hardening milestone has completed. The release acceptance and evidence
requirements are now explicit. Replace this section with final test counts,
device/performance results, artifact details, security/migration findings,
limitations, and lessons only after the exact candidate passes.

## Remaining work

Every milestone remains pending behind completion or explicit resolution of
plans 001 through 005. The next agent must begin with the current handoff and
actual working tree, not the Phase 2 numbers embedded here. Kiroku is not done
until the clean-checkout, migration, device, security, and release-like gates
have actually executed and the remaining limitations are documented.

Revision note (2026-07-14): Initial self-contained production-hardening and
release plan created from the Phase 2 baseline and the preceding feature plans.
No application implementation was changed.
