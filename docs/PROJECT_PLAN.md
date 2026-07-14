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

The host provides OpenJDK 21.0.11, while local and hosted project gates use JDK
17 with the checked-in Gradle 8.13 wrapper. The persistent ignored
`.android-sdk` contains Android Platform 36 revision 2, Build Tools 35.0.0,
platform-tools 37.0.0, Emulator 36.6.11, and the API 36 Google Play x86_64
image. Separate Pixel 2 and Pixel Tablet AVDs provide compact and large local
validation. The attached Realme is ADB-authorized but can remain dozing behind
its lock surface, so acceptance commands explicitly select an interactive AVD.

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

1. `docs/plans/001-reproducible-validation-baseline.md` completed trustworthy
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

Plan 001 is complete. Plan 002 is contract-blocked, and Plan 003 depends on it.
The public-access verification and public slice of Plan 004 are therefore the
next executable scope; authenticated release search must wait. Plans 003 and
004 share one sequential Room migration history and must revise their planned
version numbers if their execution order changes.

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

### Phase 2 — Search to series details: complete

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
Four instrumented classes contain nine tests that pass on separate API 36 Pixel
2 and Pixel Tablet AVDs and again on compact at 200% font scale. Spotless, lint,
debug APK, Android-test APK, and minified unsigned release APK gates pass from a
clean checkout. Manual evidence covers offline recovery, process death,
rotation/resize, Back, TalkBack, and large text. Corrected commit
`99f68c9cf73f3422bc3b138928899ea50701cb55` passed the hosted host, compact, and
large jobs in
[run 29335569517](https://github.com/miguel-rf/Kiroku/actions/runs/29335569517).
The authoritative handoff and exact command history are in PROJECT_STATE.md.

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
| Local SDK or hosted-runner package drift | Use the documented persistent SDK, install exact CI packages, and keep runner-image-dependent checks narrowly explained and deterministic |
| Context7 service unavailable | Use primary vendor documentation, record the fallback, and never guess version-sensitive APIs |

## Resume order

Plan 001 is complete. Resume feature work in this order:

1. Seek official login success evidence and update
   `docs/plans/002-authentication-secure-session.md`; do not write a token
   parser until the response fields are confirmed.
2. If authentication remains blocked, perform only the public-access contract
   verification allowed by `docs/plans/004-releases-background-refresh.md`.
   Do not begin authenticated library or release behavior.
3. Preserve one sequential Room migration history across Plans 003 and 004 if
   the public release slice changes the schema before authentication unblocks.

Revision note (2026-07-14): Added the numbered living ExecPlan index and made
it the execution-level source of truth. No Android implementation changed.
Later the same day, synchronized the roadmap with Plan 001 completion: replaced
the discovery-era SDK/device limitations with the persistent emulator baseline,
recorded the all-green hosted run at
`99f68c9cf73f3422bc3b138928899ea50701cb55`, and made Plan 004's permitted
public-access work the next executable scope while Plan 002 remains blocked.
