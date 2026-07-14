# Kiroku project plan

Last reviewed: 2026-07-14

This document is the phase-level roadmap. The executable, self-contained living
plans are indexed in `docs/PLANS.md` and stored under `docs/plans/`. When this
roadmap and a numbered plan differ, update both from current repository evidence
before implementation; do not silently follow stale prose.

## Discovery summary

The repository contained a single 518,717-byte OpenAPI 3.0 JSON document and no
Android source, build files, valid Git worktree, or existing conventions to
preserve. Its SHA-256 at discovery was
cb0fe8290ec7a8f1179f92a62d0be5f5602ac15feca343da6c35bceb269f30aa.

The host provides OpenJDK 21.0.11. Discovery initially found no system Gradle,
Kotlin compiler, Android SDK, emulator, or adb. The project now uses its Gradle
8.13 wrapper, and the explicitly authorized SDK bootstrap installed Android
Platform 36 revision 2, Build Tools 35.0.0, and platform-tools 37.0.0 under
`/tmp/android-sdk`. No emulator is installed; the one attached physical device
is currently unauthorized for ADB.

Context7 was subsequently configured as a project-scoped MCP server, pinned to
@upstash/context7-mcp 3.2.3, verified end to end, and exposed through native
documentation tools. Every foundation library decision was then re-audited;
the evidence and corrections are in DEPENDENCIES.md. Primary vendor
documentation remains the fallback when Context7 coverage is incomplete.

The public API contract and two deliberately small live checks confirmed the
search and detail routes. The search service enforces a 25-item response page
when a smaller per-page value is requested, can report 10,000 total matches,
uses identifiers larger than a signed 32-bit integer, and returns null for some
fields that are not marked nullable by the contract.

## Living execution plans

The remaining work is split into outcome-focused plans so a new agent can
continue with only the repository:

1. `docs/plans/001-reproducible-validation-baseline.md` restores trustworthy
   Git/clean-checkout, SDK, CI, and compact/large device validation.
2. `docs/plans/002-authentication-secure-session.md` implements login, secure
   restoration, expiry, profile, and logout after the official token response
   shape is confirmed.
3. `docs/plans/003-library-offline-sync.md` adds lists, progress, Room local
   overrides/outbox, deterministic reconciliation, and WorkManager upload.
4. `docs/plans/004-releases-background-refresh.md` adds cached releases,
   confirmed filters, Paging, and conservative periodic refresh.
5. `docs/plans/005-settings-adaptive-accessibility.md` completes preferences,
   the top-level adaptive shell, cache/account controls, licences, and the
   accessibility pass.
6. `docs/plans/006-production-hardening-release.md` completes migrations,
   security, benchmarks, Baseline Profiles, packaging, clean-checkout release
   verification, and documentation.

Plan 001 is the immediate next plan. Plan 002 is contract-blocked. Plan 003
depends on plan 002. The public-access verification and public slice of plan 004
may proceed while authentication is blocked; authenticated release search must
wait. Plans 003 and 004 share one sequential Room migration history and must
revise their planned version numbers if their execution order changes.

## Ordered implementation plan

### Phase 0 — Discovery: complete

- Inventory repository and local toolchain.
- Verify base URL, public series operations, DTO shapes, paging, nullability,
  error envelope, authentication declarations, and acceptable-use terms.
- Record assumptions, risks, blockers, architecture, security, and test policy.
- Re-audit dependency, build, persistence, navigation, adaptive UI, networking,
  image, coroutine, security, and testing guidance through Context7.

Exit evidence: this document, ARCHITECTURE.md, API_NOTES.md, SECURITY.md, and
TESTING.md are present and contain no invented API fields.

### Phase 1 — Foundation: complete

1. Create a single-module Gradle Kotlin DSL Android project with a version
   catalog and pinned stable dependency set.
2. Configure Compose, Material 3, Material 3 Adaptive, Navigation 3,
   localization-ready resources, edge-to-edge rendering, and a manual
   AppContainer.
3. Configure Retrofit, OkHttp, kotlinx.serialization, centralized failures,
   debug-only redacted request logging, and fixed timeouts.
4. Create Room schema version 1, export schemas, and add deterministic test
   infrastructure.
5. Defer secure-token interfaces until the undocumented login payload is
   confirmed, avoiding a speculative security abstraction.

Exit evidence: debug and minified release packaging pass; 28 JVM tests pass;
Android lint reports no findings; Spotless passes; Room version 1 is exported
and inspected. The current command record is in PROJECT_STATE.md.

Implementation checkpoint: the Gradle project, compatible stable version pins,
wrapper, Compose/theme resources, manual AppContainer, network/error
foundation, and generated Room version-1 schema are verified. SDK licenses were
accepted only after explicit user authorization. No auth/token implementation
was introduced.

### Phase 2 — Search to series details: complete on available gates

1. Define separate API DTO, Room entity, domain, and UI representations.
2. Implement POST /series/search using a 25-item Paging 3 RemoteMediator.
3. Store series summaries, query results, remote keys, and recent searches in
   Room transactions.
4. Debounce query changes and cancel stale paging streams with flatMapLatest.
5. Implement GET /series/{id}, cache its complete supported metadata, and make
   the detail screen observe Room.
6. Add compact Navigation 3 navigation and expanded list-detail presentation.
7. Cover initial, loading, empty, cached/offline, recoverable error, retry, and
   refresh states.
8. Add DTO, mapper, DAO, network, repository, ViewModel, and critical Compose
   UI tests.

Exit evidence: all eight items are implemented. `MainActivity` launches a
Navigation 3 host with both lifecycle entry decorators, compact single-pane
navigation, and an explicit stable 600 dp+ list/detail layout. Search and detail
are Room-backed, refreshable, cache-aware, and covered by 28 passing JVM tests.
Three Compose instrumented test classes compile and package. Spotless, lint,
debug APK, Android-test APK, and minified unsigned release APK gates pass.

Device execution remains an environment limitation rather than an unreported
pass: the attached device is unauthorized and no emulator is installed. Run
the packaged Compose tests and manual compact/tablet accessibility smoke checks
when a device becomes available. The authoritative handoff and exact command
history are in PROJECT_STATE.md.

### Phase 3 — Authentication and library: blocked on one API fact

1. Confirm the exact login success context containing the bearer JWT.
2. Implement credentials-in-memory-only login, encrypted token persistence,
   session restoration, logout, and expired-session handling.
3. Cache lists and reading state in Room.
4. Implement atomic local updates plus an ordered outbox for add, move, remove,
   and progress operations.
5. Add durable WorkManager upload and reconciliation jobs.

Blocker: Account login returns ApiResponseV1, whose context is an unrestricted
object. The token field and any expiry metadata are not defined. Kiroku will not
guess them. A sanitized successful response sample or an updated official
contract is required.

### Phase 4 — Releases and product integration

- Use the documented GET /releases/days operation for recent releases.
- Cache release records and provide refresh/filter behavior.
- Add conservative optional periodic release refresh based on user preference.
- Complete the Search/Library/Releases/Settings shell, typed non-sensitive
  settings, safe cache controls, licences, adaptive layouts, and accessibility.

### Phase 5 — Production hardening

- Complete TalkBack, large-font, contrast, focus, touch-target, and tablet/
  foldable audits.
- Add Room migration fixtures when schema version 2 is introduced.
- Profile startup, search, scrolling, and details before adding optimizations.
- Add Baseline Profiles and macrobenchmarks after flows stabilize.
- Complete licenses, privacy/release notes, CI, signing guidance, and a clean
  checkout verification.

## Decisions

- Minimum SDK 23 aligns with current stable Room and Coil releases.
- Compile and target SDK 36 avoid a dependency on preview API 37 while
  supporting current Android behavior.
- A manual AppContainer is preferred until worker scoping or test replacement
  complexity provides evidence for Hilt.
- Search uses Paging 3 because the real endpoint reports very large result sets.
- Search and detail refreshes use foreground coroutines; WorkManager is reserved
  for later durable synchronization.
- All MangaUpdates identifiers are Long values.
- No production code depends on live API calls during tests.
- Navigation 3 1.1.4 and KSP 2.3.9 replaced their earlier stable pins after the
  Context7 audit and current primary release check; Room schema export uses the
  official Room Gradle plugin.

## Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| OpenAPI optionality and observed JSON nulls disagree | Nullable DTO fields at confirmed optional/null points; strict domain validation and realistic fixtures |
| API publishes no numeric rate limit | 25-item pages, Room cache, debouncing, no duplicate refresh, no speculative prefetch |
| POST search cannot rely on ordinary HTTP disk caching | Persist normalized query/result mappings and remote keys in Room |
| Very large descriptions and category lists | Store off the main thread, render lazily, and collapse long category sections |
| Process death during future list mutations | Same-transaction outbox and WorkManager in Phase 3 |
| Temporary SDK under `/tmp` is not durable across host cleanup | Re-provision the documented packages or point `local.properties` at a persistent SDK before the next clean checkout |
| Context7 service unavailable | Use primary vendor documentation, record the fallback, and never guess version-sensitive APIs |

## Resume order

1. Execute and maintain
   `docs/plans/001-reproducible-validation-baseline.md`, including Git
   provenance, persistent SDK/CI, device execution, and accessibility smoke
   checks.
2. Seek official login success evidence and update
   `docs/plans/002-authentication-secure-session.md`; do not write a token
   parser until the response fields are confirmed.
3. If authentication remains blocked, perform only the public-access contract
   verification allowed by `docs/plans/004-releases-background-refresh.md`.
   Do not begin authenticated library or release behavior.

Revision note (2026-07-14): Added the numbered living ExecPlan index and made
it the execution-level source of truth. No Android implementation changed.
