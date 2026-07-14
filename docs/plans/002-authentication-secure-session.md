# Implement authentication and secure session handling

This ExecPlan is a living document. Keep `Progress`, `Unexpected discoveries`,
`Decision log`, `Outcomes & Retrospective`, and the revision note current while
work proceeds. Maintain it in accordance with `docs/PLANS.md`.

## Goal

Add production-quality MangaUpdates sign-in, session restoration, expired-
session handling, profile display, and sign-out without retaining the user's
password or storing a bearer token in plaintext. The implementation must use
only the officially documented account endpoints and must stop at the contract
gate below until the successful login response shape is confirmed by official
evidence.

This plan establishes authenticated networking for later plans. It does not add
lists, library mutations, releases, or background synchronization.

## User-visible behavior

A signed-out user can open a localized login screen, enter a username and
password, see inline validation, submit once, and receive an accessible loading
or failure state. On success, Kiroku clears the password immediately, shows the
authenticated account, and survives process death and device restart without
asking for credentials again while the session remains valid.

An invalid login remains signed out and explains that the credentials were not
accepted without displaying a raw server body. A timeout or offline failure is
distinguished from invalid credentials and can be retried. A confirmed 401 on
an authenticated operation transitions the app to an expired-session state and
requires a new login; it never enters an automatic retry loop. Logout attempts
the documented server operation when possible and always removes the local
credential material before returning to signed-out UI.

## Relevant existing code

The repository root is `/home/miguel/Kiroku`. The application is a single
Gradle module with a manual dependency graph in
`app/src/main/java/com/kiroku/app/AppContainer.kt`. `DefaultAppContainer`
creates one public OkHttp client, one Retrofit `MangaUpdatesApi`, Room, the
catalogue repository, and the connectivity monitor.

`app/src/main/java/com/kiroku/app/core/network/NetworkModule.kt` defines the
confirmed base URL, strict `kotlinx.serialization` configuration, finite
timeouts, a User-Agent, and debug-only BASIC logging. Authorization, Cookie,
and Set-Cookie are already redacted, and bodies are never logged.
`MangaUpdatesApi.kt` currently contains only public search and detail methods.
`NetworkErrorMapper.kt` and the surrounding network package already map
connectivity, timeout, authentication, permission, rate-limit, validation,
server, serialization, and unknown failures.

`app/src/main/java/com/kiroku/app/KirokuApp.kt` owns the Navigation 3 back stack.
There is no authentication route, ViewModel, repository, token store, bearer
interceptor, Tink dependency, DataStore, or authenticated API service yet.
`app/src/main/res/xml/backup_rules.xml` and `data_extraction_rules.xml` already
exclude `sharedpref/secure_session.xml` and `files/secure-session/` from backup.

Read `docs/API_NOTES.md`, `docs/SECURITY.md`, `docs/DEPENDENCIES.md`, and the
checked-in `openapi.json` before implementing this plan. Plan 001 must first
provide a trustworthy validation baseline, or this plan must explicitly record
why implementation proceeded without it.

## Confirmed API contracts

The base URL is `https://api.mangaupdates.com/v1/`. The root OpenAPI security
scheme is HTTP bearer with bearer format JWT, and authenticated requests use:

    Authorization: Bearer <token>

The documented account operations are:

- `PUT /account/login` with required JSON strings `username` and `password` in
  `AccountLoginModelV1`. A 200 and a documented 400 use `ApiResponseV1`; a 401
  has no response schema.
- `GET /account/profile` with bearer authentication. A 200 returns
  `UserModelV1`; a 401 returns `ApiResponseV1`; a 404 has no response schema.
- `POST /account/logout` with bearer authentication. A 200 and 401 use
  `ApiResponseV1`.
- `GET /account/refresh` is described as refreshing a session-token cookie. A
  200 uses `ApiResponseV1`, 401 is unauthenticated, and 412 is described as
  refresh throttling. It is not evidence of bearer-token refresh.

`ApiResponseV1` requires string `status` and `reason`, while `context` is an
unrestricted object with `additionalProperties: true`. The official schema
does not identify the successful login member containing the JWT, its expiry,
or other required session metadata. That is a hard blocker. Before writing the
login success DTO or any token parser, obtain one of the following and record
it in `docs/API_NOTES.md` and this plan: an updated official OpenAPI schema, an
official successful response example, or maintainer-provided documentation
that names and types every required field. A real credential must never be
placed in the repository or a transcript. Do not infer fields such as `token`,
`access_token`, `expires`, or `expiration`.

Every `UserModelV1` property is optional in the schema. Relevant account-shell
members include integer `user_id`, string `username`, string `url`, optional
avatar/time metadata, roles, and additional profile/statistics fields. Kiroku
must retain only the minimum display identity it needs; it must not cache email,
signature, location, administrative metadata, or unrelated statistics. A
positive `user_id` is required before plan 003 may create account-partitioned
library rows. If it is absent, fail that domain mapping explicitly and keep
authenticated library behavior unavailable rather than inventing an ID or
using a token fingerprint.

The refresh endpoint must not be called for bearer sessions unless official
evidence confirms that behavior. With the current contract, a bearer 401 is a
terminal expired-session result and is not retried automatically.

## Architecture

Keep the manual `AppContainer`. Add narrowly scoped session dependencies to the
container and reassess Hilt only if later Worker injection or test replacement
becomes materially difficult. Do not introduce a DI framework in this plan.

Separate public and authenticated request concerns. Login must use a client or
service path that never adds an old bearer token. Authenticated requests use an
OkHttp interceptor that reads an in-memory immutable session snapshot and adds
exactly one Authorization header. It must not perform disk I/O, refresh, block
on a Flow, log, or retry. Use a repository-level authenticated request executor
to map a confirmed 401 into one session-expiration transition shared by
concurrent callers. Do not use `OkHttp Authenticator` while refresh behavior is
unconfirmed.

Introduce clear responsibilities, using names consistent with the codebase:

- API DTOs and account service methods stay under `core/network/`.
- `core/security/` owns a `SessionEnvelope`, authenticated encryption, and the
  encrypted session store.
- `data/repository/SessionRepository` owns login, restoration, profile
  verification, expiration, and logout rules.
- `feature/authentication/` owns immutable login/account UI state, ViewModels,
  validation, and Compose screens.

Expose a process-wide `StateFlow<AuthState>` with explicit restoring, signed-
out, signed-in, and expired states. The first frame must not briefly expose
authenticated navigation before encrypted state has been restored. UI state
must not contain the password after submission finishes; it must never be
stored in `SavedStateHandle` or a persistent collection.

Use a high-level AEAD implementation with Android Keystore-protected key
material. Tink remains the preferred candidate, but before adding it resolve
the current stable library/version through Context7 and verify the exact Android
Keystore/keyset lifecycle against current primary Tink and Android security
documentation. The 2026-07-14 Context7 query confirmed AEAD concepts but
returned mixed-era examples and did not by itself establish a production-safe
current API. Record the selected API, key alias, keyset location, associated
data, failure behavior, and rotation path in `docs/SECURITY.md`.

Encrypt one versioned session envelope containing the bearer token and only the
account metadata needed to restore identity. Store its ciphertext under the
already excluded `files/secure-session/` path. Bind ciphertext to a stable,
non-secret associated-data string containing the app/session-record version.
Treat missing, truncated, undecryptable, or version-unsupported ciphertext as
signed out, delete it safely, and expose a non-sensitive diagnostic category in
debug builds. Never fall back to plaintext.

Logout first snapshots the current token, makes at most one documented remote
logout attempt when connected, then clears local token material and in-memory
state regardless of remote outcome. Once local logout completes, no durable
retry may retain the bearer token. Document that an unreachable server-side
session may remain valid until its own expiry.

## Database changes

No Room schema change is required. Session secrets do not belong in Room, and
non-sensitive profile display does not justify a new table in this milestone.
Keep `KirokuDatabase` at version 1 and preserve its exported identity hash.

The encrypted session file format needs its own explicit envelope version, but
that is not a Room migration. Tests must demonstrate first creation, replace,
read, clear, corruption recovery, and an unsupported-version failure. Backup
exclusions must be verified against the final storage path in both backup rule
files and the packaged manifest configuration.

## Implementation milestones

Milestone 1 closes the contract gate. Compare `openapi.json` with the official
live documentation and obtain sanctioned evidence for the login success
context. Update `docs/API_NOTES.md` with the exact names, JSON types,
nullability, expiry semantics, and a redacted realistic fixture. If this cannot
be obtained, update `Progress` and `Remaining work` and stop this plan without
scaffolding a guessed parser.

Milestone 2 builds and tests secure storage independently. Audit the current
stable Tink and Android Keystore APIs via Context7 and primary documentation,
add the minimal dependency through `gradle/libs.versions.toml`, implement the
versioned AEAD store, confirm backup exclusion, and add deterministic unit tests
plus a real Android Keystore instrumented test. No sample or test secret may
resemble a production credential.

Milestone 3 adds the authenticated network and repository state machine. Add
the confirmed DTOs and account service, split public/login/authenticated client
construction, implement one-shot expiration handling, restore and verify the
profile, and test all HTTP and serialization outcomes with MockWebServer. Run
concurrency tests proving multiple 401 responses produce one stable expiration
transition and no retry loop.

Milestone 4 delivers the UI. Add localized login, account, logout-confirmation,
offline, loading, invalid-credentials, and expired-session states to the
Navigation 3 shell. Use lifecycle-aware StateFlow collection, accessible error
announcements, password semantics, suitable IME actions, large touch targets,
and disabled duplicate submission. Clear the password on success, failure,
navigation away, and ViewModel clearance.

Milestone 5 validates and documents the completed slice. Inspect the complete
diff; run formatting, unit, lint, build, and device tests; manually inspect logs
and app storage for token/password leakage; restart the process and device; and
update `docs/PROJECT_STATE.md`, `docs/API_NOTES.md`, `docs/ARCHITECTURE.md`,
`docs/SECURITY.md`, `docs/TESTING.md`, and this plan with actual evidence.

## Testing requirements

Add realistic DTO deserialization tests for the confirmed login envelope and
profile, including missing required fields, null optional values, unknown keys,
and a malformed context. Add MockWebServer tests for successful login, invalid
credentials, bodyless 401, validation 400, timeout, offline I/O, malformed JSON,
profile restoration, logout success/failure, and an authenticated request with
exactly one correctly formed bearer header. Recorded requests must be inspected
without printing the fake token.

Test `SessionRepository` with fake clocks, fake storage, controlled dispatchers,
and Turbine. Cover cold restoration, signed-out launch, successful login,
failure with immediate password disposal, corrupted ciphertext, expired 401,
concurrent 401s, local logout after remote failure, and process recreation. Add
token-store unit tests around the envelope and Android instrumented tests around
actual Keystore-backed encrypt/decrypt/clear behavior.

Compose tests must cover empty/invalid form validation, password masking,
loading and duplicate-submit prevention, successful navigation, invalid login,
offline retry, expired-session messaging, logout confirmation, large fonts,
TalkBack semantics, and restoration. They must use fake repositories and never
send a real credential or production request.

Run the repository's complete host gate from plan 001, then
`connectedDebugAndroidTest` on the validated compact and large configurations.
If a new security dependency changes release shrinking, also inspect and run
the minified release build explicitly. Record command output and test counts;
compilation of instrumented tests is not a passed device test.

## Progress

- [x] (2026-07-14 07:24Z) Inspected the checked-in account contracts and
  documented the unresolved successful-login context.
- [x] (2026-07-14 07:24Z) Verified through the configured Context7 server that
  AndroidX and Tink documentation can be queried; noted that the returned Tink
  examples are not sufficient to select the final stable Android API.
- [ ] Obtain official evidence for the bearer token, expiry, and session
  metadata response fields and update this plan before coding.
- [ ] Audit and implement Keystore-backed AEAD session storage.
- [ ] Implement the account service, authenticated client, and repository state
  machine.
- [ ] Implement and test login, restoration, expiry, account, and logout UI.
- [ ] Run all host and device quality gates and update the handoff.

## Decision log

- Decision: Treat the login response shape as an implementation blocker.
  Rationale: `ApiResponseV1.context` is untyped; accepting a guessed member
  would violate the documented-endpoint rule and could mishandle credentials.
  Date/Author: 2026-07-14, Codex.
- Decision: Do not use the cookie refresh operation for bearer sessions without
  new official evidence.
  Rationale: Its description is cookie-specific and no bearer refresh contract
  is defined.
  Date/Author: 2026-07-14, Codex.
- Decision: Keep public/login and authenticated request paths distinct and put
  401 state transitions above OkHttp.
  Rationale: This prevents an old token on login and makes the no-retry rule
  observable and deterministic.
  Date/Author: 2026-07-14, Codex.
- Decision: Retain manual dependency injection for this milestone.
  Rationale: The graph remains small and constructor-based replacement is
  adequate; framework cost is not yet justified.
  Date/Author: 2026-07-14, Codex.

## Unexpected discoveries

- Observation: The schema advertises JWT bearer authentication but does not
  describe the JSON member that returns the JWT.
  Evidence: `PUT /account/login` returns the generic `ApiResponseV1`, whose
  `context` allows arbitrary properties.
- Observation: The only documented refresh endpoint is explicitly
  cookie-oriented.
  Evidence: `GET /account/refresh` is described as refreshing a session-token
  cookie and supplies no bearer-token request/response contract.
- Observation: A broad Context7 query can return examples from different Tink
  and AndroidX eras.
  Evidence: the 2026-07-14 results included current concepts alongside older or
  pre-release snippets. Implementation must pin a stable version and verify its
  matching primary API rather than copying the first result.

## Outcomes & Retrospective

No implementation milestone has completed. Authentication is intentionally
contract-blocked; the absence of a speculative token parser is the correct
current result. Replace this section with security, behavior, and gate outcomes
after the official contract is confirmed and implementation finishes.

## Remaining work

The immediate next action is obtaining official successful-login response
evidence. Every coding milestone remains pending behind that gate. If evidence
cannot be obtained, leave authentication unavailable, keep public catalogue
features functional, and record the blocker rather than weakening token
handling or making a live credential experiment.

Revision note (2026-07-14): Initial self-contained authentication plan created
from the Phase 2 handoff and checked-in OpenAPI contract. No application code or
dependency was changed.
