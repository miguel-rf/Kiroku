# Dependency and Context7 audit

Last reviewed: 2026-07-13

## Purpose

This document records the documentation evidence behind Kiroku's Android and
JVM dependency choices. Context7 was configured after the initial discovery
documents were drafted, so every foundation decision was re-audited before the
first vertical slice continued.

Context7 is used for library APIs and implementation patterns. The checked-in
MangaUpdates OpenAPI contract and the official MangaUpdates documentation
remain authoritative for remote endpoints and payloads; Context7 is not used
to infer API fields.

## Context7 sources and decisions

| Area | Resolved Context7 library | Verified guidance | Kiroku decision |
| --- | --- | --- | --- |
| AndroidX, Room and Paging | `/androidx/androidx` | KSP processing, suspend transactions, exported schemas, `RemoteMediator`, and Room-backed paging are the documented source-of-truth pattern. `RemoteMediator` still carries `ExperimentalPagingApi`. | Use Room 2.8.4, Paging 3.5.0, a narrow paging opt-in, and atomic Room transactions. No broad experimental suppression. |
| Navigation 3 | `/websites/developer_android_guide_navigation_navigation-3` | Route keys implement `NavKey`, are serializable, and are stored in `rememberNavBackStack`; `NavDisplay` renders entries and handles back navigation. | Use stable Navigation 3 1.1.4 with serializable keys and no preview scene APIs. |
| Material 3 Adaptive | `/androidx/androidx` | Window adaptive information and list-detail building blocks are available. Context7 also showed `ListDetailPaneScaffold` and `NavigableListDetailPaneScaffold` annotated with `ExperimentalMaterial3AdaptiveApi` in relevant API surfaces. | Keep stable Adaptive 1.2.0. Use stable window information plus an explicit two-pane layout; do not use the experimental navigable scaffold or alpha `adaptive-navigation3` integration. |
| Coroutines | `/kotlin/kotlinx.coroutines` | `StateFlow`, `debounce`, `flatMapLatest`, atomic updates, and `runTest` virtual time provide the required UDF and cancellation behavior. | ViewModels expose immutable `StateFlow`; tests advance virtual time and never sleep. |
| Serialization | `/kotlin/kotlinx.serialization` | `ignoreUnknownKeys` tolerates additive fields; missing required constructor fields fail; `coerceInputValues` can hide invalid values by applying defaults. | Enable unknown-key tolerance and omit encoded nulls, but keep coercion disabled so invalid required data is not silently replaced. |
| Retrofit | `/square/retrofit` | Suspend service functions may return the body or `Response`; body return types throw `HttpException` for non-2xx responses. The official kotlinx.serialization converter uses `Json.asConverterFactory`. | Return DTO bodies and centralize `HttpException`, error-envelope, I/O, and serialization mapping in the remote data source. |
| OkHttp and MockWebServer | `/websites/square_github_io_okhttp` | Configure finite connect/read/write/call timeouts; OkHttp 5 redacts common sensitive headers in string rendering; MockWebServer 3 is the current test artifact. | Debug logging is BASIC only with explicit header redaction; no bodies are logged. Tests use MockWebServer 3. |
| Coil | `/websites/coil-kt_github_io_coil` | Coil 3 uses `coil-compose` plus `coil-network-okhttp`; `AsyncImage` derives an appropriate request size from layout constraints; platform request options such as `crossfade` are extension APIs. | Use compatible Coil 3.4.0, constrained thumbnail layouts, Compose clipping, explicit loading/error UI, and meaningful cover descriptions. |
| KSP | `/google/ksp` | KSP2 is the supported path for Kotlin 2.3+; KSP1 is deprecated and is not compatible with Kotlin 2.3+. | Pin KSP 2.3.9 and remain on its default KSP2 engine. |
| Gradle | `/websites/gradle_current_userguide` | Central repositories, version catalogs, the wrapper, Java toolchains/targets, and reproducible dependency metadata are the supported build mechanisms. | Use Gradle 8.13, Kotlin DSL, `libs.versions.toml`, repository fail-on-project-repos, Java/JVM 17, and a checked-in wrapper. |
| Spotless | `/diffplug/spotless` | Spotless 8.7 supports the selected JRE/Gradle range and provides cacheable check/apply tasks with ktlint integration. | Pin Spotless 8.7.0, format Kotlin/Kotlin DSL, and run `spotlessCheck` as a quality gate. Do not schedule `clean` concurrently with formatting tasks. |
| Flow tests | `/cashapp/turbine` | `Flow.test` establishes collection eagerly, requires events to be consumed, and integrates with `runTest`; cancellation must be explicit when ignoring remaining events. | Assert every expected state with Turbine and use `cancelAndIgnoreRemainingEvents` only where an infinite flow is intentional. |
| Robolectric | `/robolectric/robolectric` | Local Android-resource tests require `unitTests.isIncludeAndroidResources = true`; Robolectric 4.16.1 supports the selected Android test stack. | Use Robolectric for fast Room/repository tests where it adds value; retain instrumented tests for platform integration and real Compose device behavior. |
| Tink | `/tink-crypto/tink` | AEAD requires associated data and encrypted keyset material; Context7 returned an Android Keystore-backed `AndroidKeysetManager` example but did not resolve its long-term API status clearly enough for a production token store. | Do not add Tink yet. Re-query current Tink Android guidance when the documented login token shape is available, and do not guess around an uncertain security API. |

## Stable-version checks

Context7 provides current API guidance, but exact release-channel and toolchain
compatibility were cross-checked with primary release pages when its result was
incomplete:

- Android Gradle Plugin 8.13.2 supports Kotlin 2.3, requires Gradle 8.13 and
  JDK 17, and supports API 36.1.
- Room 2.8.4 is stable, requires minimum SDK 23, recommends KSP2, and provides
  the `androidx.room` Gradle plugin for cacheable schema export.
- Paging 3.5.0, Lifecycle 2.10.0, Activity 1.13.0, Core 1.18.0, Navigation 3
  1.1.4, and Material 3 Adaptive 1.2.0 are stable AndroidX releases. Core and
  Lifecycle deliberately remain one stable line below their newest releases
  because the newer artifacts require API 37/AGP 9.1.
- Compose BOM 2026.06.00 is the stable BOM documented by Android Developers.
- Kotlin 2.3.21 is intentionally retained instead of Kotlin 2.4.0 because AGP
  8.13.2 explicitly adds Kotlin 2.3 bytecode support and the selected stack is
  already mutually compatible.
- Retrofit 3.0.0, OkHttp 5.4.0, Coil 3.4.0, coroutines 1.11.0, and
  kotlinx.serialization 1.11.0 are stable releases. Coil 3.5.0 was evaluated
  but selected Kotlin stdlib 2.4.0 and produced an R8 compatibility warning
  under AGP 8.13.2; 3.4.0 keeps the verified Kotlin 2.3 toolchain coherent.

Primary fallback pages:

- https://developer.android.com/build/releases/agp-8-13-0-release-notes
- https://developer.android.com/jetpack/androidx/versions
- https://developer.android.com/jetpack/androidx/releases/room
- https://developer.android.com/develop/ui/compose/bom
- https://kotlinlang.org/docs/releases.html
- https://square.github.io/okhttp/changelogs/changelog/

## Experimental API policy

No alpha, beta, or release-candidate dependency is selected. The vertical slice
has two narrow experimental API opt-ins: Paging's `RemoteMediator`, because
durable network-to-Room pagination is the documented mechanism for the large
offline result set, and Flow's `flatMapLatest`, because stale query streams must
be cancelled. Each opt-in is placed at its use site and covered by mediator or
ViewModel tests.

Experimental adaptive scaffolds, Navigation 3 scene integrations, and
experimental serialization exception APIs are deliberately avoided.
