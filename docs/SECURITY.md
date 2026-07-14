# Security

## Threat model

Kiroku handles a MangaUpdates username, password during login, bearer session
token, account metadata, and private reading-list state. The primary risks are
credential retention, token disclosure through storage or logs, request
interception on compromised networks, accidental backup, and sensitive data
appearing in diagnostics.

The app does not handle manga files, payments, or its own server credentials.

## Implementation checkpoint

The verified search/detail build disables cleartext in the application manifest
for the complete API 23+ range. OkHttp uses injectable finite timeouts,
release builds omit HTTP logging, and debug builds use BASIC metadata logging
with Authorization, Cookie, and Set-Cookie redaction. Request and response
bodies are never logged. Debug and minified release variants compile, network
failure tests pass, lint reports no issues, and `aapt2` inspection confirms
`usesCleartextTraffic=false` in the packaged manifest.

No login UI, bearer interceptor, token store, Tink dependency, account cache,
or authenticated request code exists. Consequently no MangaUpdates password or
token is accepted or persisted by the current application.

Authentication remains blocked because the official login response does not
define the token field or expiry metadata. Kiroku will not create a speculative
credential parser or plaintext placeholder while waiting for that evidence.

## Network

- All production API and image URLs must use HTTPS.
- Cleartext traffic is disabled directly on the application manifest, which is
  effective across Kiroku's complete minimum-SDK range.
- OkHttp uses finite connect, read, write, and call timeouts.
- Release builds have no HTTP logging interceptor.
- Debug logging is BASIC metadata only. Authorization, Cookie, and Set-Cookie
  headers are explicitly redacted; request and response bodies are never logged.
- Interceptors never log or stringify token-store values.
- A 401 is retried at most once only if a documented refresh mechanism is later
  confirmed. Concurrent refresh would be serialized.

Certificate pinning is not planned initially. Pinning without an operational
backup/rotation channel can create an avoidable outage and is not required by
the API documentation.

## Credentials and token lifecycle

The login password will be held only in form state and the active request. It is
never written to Room, DataStore, SavedStateHandle, BuildConfig, logs,
analytics, crash reports, screenshots, or tests. Login state clears the
password immediately after the request finishes or the screen leaves.

The planned token store will:

1. create authenticated-encryption key material;
2. protect that material with Android Keystore;
3. encrypt the token with associated data binding it to Kiroku's token record;
4. store only ciphertext and required non-sensitive metadata;
5. clear ciphertext on logout or confirmed revocation;
6. report corruption as signed-out state without exposing plaintext.

Tink is the preferred high-level implementation, but it will be added only when
the official login token shape is confirmed. The Context7 audit confirmed AEAD
and Android Keystore-backed keyset patterns but did not establish the long-term
status of the returned Android keyset API clearly enough for a production
choice. Tink will be re-audited against Context7 and primary security
documentation immediately before implementation; no uncertain crypto API is
being scaffolded early.

## Local data

Reading history and cached account data are private application data. Android
backup rules will exclude encrypted token material and, before authenticated
features ship, will explicitly decide whether list caches are excluded.
Database files are sandboxed but not claimed to be encrypted at rest; the
session token receives stronger authenticated encryption because it grants
remote access.

## Diagnostics

Development errors retain exception causes in memory. User-visible messages are
localized categories, never raw server bodies or exception messages. Tests use
obviously fake credentials and assert that loggable request representations do
not contain them.

Security issues should be reported privately to the maintainers. Real
credentials, tokens, production response dumps containing account data, and
local keystore files must never be committed.

## Development documentation tooling

The repository pins Context7 MCP 3.2.3 in .codex/config.toml rather than
executing an unbounded latest package. The server exposes documentation lookup
tools only and defaults to automatic read-only tool approval. An optional
CONTEXT7_API_KEY is accepted only from the host environment and is never stored
in project files. Context7 is not part of the Android application or its
runtime dependency graph.

The complete Context7 security and dependency audit, including the deferred
Tink decision, is recorded in DEPENDENCIES.md.
