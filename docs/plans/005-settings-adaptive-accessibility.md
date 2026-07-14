# Complete settings, adaptive navigation, and accessibility polish

This ExecPlan is a living document. Keep `Progress`, `Unexpected discoveries`,
`Decision log`, `Outcomes & Retrospective`, and the revision note current while
work proceeds. Maintain it in accordance with `docs/PLANS.md`.

## Goal

Turn the completed catalogue, account, library, and releases slices into one
cohesive Material 3 application. Add durable non-sensitive settings, complete
top-level navigation, cache/account controls, open-source notices, and a
systematic accessibility and adaptive-layout pass for phones, tablets, and
foldables.

This plan integrates and polishes existing features. It must not add new
MangaUpdates endpoints, redesign repository business rules, or move secrets
into preferences. Execute it after plans 002 through 004 have established their
routes and state models.

## User-visible behavior

Kiroku launches into a clear top-level shell for Search, Library, Releases, and
Settings. Compact windows use a reachable single-pane navigation pattern;
larger windows use an appropriate navigation rail and retain list/detail
context. Resizing, folding, rotating, or process recreation does not lose the
selected destination or create duplicate routes.

In Settings, the user can select system/light/dark theme, enable dynamic color
when supported, choose comfortable or compact content density, configure
release synchronization options, inspect and clear caches safely, view account
state and log out, read open-source licences, and see an unmistakable unofficial
app notice and MangaUpdates credit. Settings persist across restart.

All screens remain usable with TalkBack, keyboard/D-pad traversal where the
device supports it, 200% font scale, display scaling, high contrast needs, and
touch targets of appropriate size. Loading, errors, sync state, and offline
state are announced meaningfully and never depend on color alone.

## Relevant existing code

The root theme lives under `app/src/main/java/com/kiroku/app/core/designsystem/`
and is applied by the main activity. Inspect the actual files because earlier
plans may have expanded this package. User-facing text lives in Android string
resources and must remain localization-ready.

`KirokuApp.kt` currently owns a Navigation 3 `NavBackStack` and switches from a
compact single pane to an explicit search/detail row using
`currentWindowAdaptiveInfo()` and the medium-width breakpoint. The project
already includes Material 3 Adaptive 1.2.0 and Navigation 3 1.1.4. Plans 002,
003, and 004 should have added account, library, and release routes and may have
changed this shell; treat the working tree, their completed decision logs, and
their device evidence as authoritative.

`AppContainer.kt` remains the intended manual dependency graph. Preferences
DataStore is absent in the Phase 2 baseline, but plan 004 may have added a
minimal periodic-refresh preference. Session secrets belong to the encrypted
store from plan 002; Room contains catalogue/library/release data. Existing
backup exclusions and account-clearing rules are documented in
`docs/SECURITY.md`.

Before implementation, inventory every Compose screen and string. Query
Context7 for the selected stable Material 3 Adaptive, Navigation 3, lifecycle-
aware Compose, DataStore, and any pull-to-refresh APIs. Use version-specific
primary Android documentation when returned examples are experimental, older,
or pre-release. Do not opt into an experimental API merely for visual novelty;
retain an explicit refresh action if no suitable stable pull-to-refresh API is
available.

## Confirmed API contracts

This plan adds no network endpoint or response field. Search, details, account,
lists, and releases must continue to use only the contracts recorded in
`docs/API_NOTES.md` and their completed plans.

Settings may enable or disable work that already exists, but they must not
change undocumented request frequency, construct new filters, invoke account
refresh as bearer refresh, or expose group URLs as reader actions. Cache
controls operate locally. Open-source notice generation reads build dependency
metadata, not MangaUpdates data.

## Architecture

Add one application-scoped Preferences DataStore instance behind a typed
`SettingsRepository`. If plan 004 already introduced it, extend that same
instance; never create one DataStore instance per screen or feature. Expose a
Flow of an immutable domain settings model with these initial values:

- theme mode: system, light, or dark;
- dynamic color enabled where the platform supports it;
- layout density: comfortable or compact;
- periodic release refresh enabled;
- periodic refresh network policy, initially any connected or unmetered;
- a documented cache-retention choice if the product exposes one.

Use stable string enum representations and validate unknown/corrupt values back
to safe defaults. Resolve stable DataStore creation, edit, corruption, and
migration behavior through Context7 and primary docs before coding. No password,
token, Authorization value, private response body, or complete library payload
belongs in preferences.

Keep one top-level route model and one source of navigation truth. Use Material
3 navigation components appropriate to the window width: compact destinations
must fit without truncation; larger layouts may use a rail. Preserve the
existing list/detail behavior for search, library, and series instead of
rendering two independent back stacks. If migrating to a Material 3 Adaptive
list-detail scaffold, first verify a stable API compatible with Navigation 3
and document state/restoration behavior. A measured, accessible custom pane
layout is preferable to an experimental dependency.

Settings ViewModels coordinate repositories and WorkManager-facing schedulers;
Composables render immutable state and send explicit events. Cache operations
belong in repositories/DAOs and run off the main thread. Define separate,
clearly worded actions:

- Clear public search/detail/release caches while preserving account session,
  library-confirmed state, local overrides, and pending outbox operations.
- Remove downloaded image cache through Coil's supported cache API only after a
  current API check.
- Clear account data only through an explicit destructive confirmation that
  explains pending-change consequences and follows plan 003's logout policy.

Choose an open-source licence solution only after checking its current stable
Gradle/Android support through Context7 or primary project documentation. Prefer
a build-generated, reviewable notice asset and a small in-app viewer. Do not add
network access or a large runtime framework just to show notices. Include
Kiroku's own licence if one exists and every shipped dependency's required
notice.

Perform accessibility at the semantic-state level. Decorative images have no
description; cover art has a concise localized description when meaningful.
Headings, pane titles, selected tabs, validation errors, progress, pending sync,
and retry results expose appropriate roles/state descriptions and live-region
behavior without duplicate speech. Keep 48 dp touch targets, logical traversal,
visible focus, contrast, and text reflow. Never concatenate English fragments
in Composables.

## Database changes

No Room schema change is expected. Theme, density, and sync preferences belong
in DataStore. Cache clearing uses existing DAO transactions and must preserve
library/outbox invariants.

If implementation reveals a genuine need for a persisted local-only setting
that participates in relational queries, update this plan before changing Room,
add the next sequential explicit migration, export its schema, and add migration
tests. Convenience alone is not sufficient evidence.

DataStore preferences need a documented compatibility policy. Unknown enum
strings fall back safely; renamed keys use an explicit one-time migration; old
keys are removed only after the new value commits. Tests must cover a fresh
store, every setting, corruption/default behavior, and any migration.

## Implementation milestones

Milestone 1 inventories the integrated app. List every route, state, screen,
cache, worker preference, string, and accessibility gap. Record phone, tablet,
foldable, large-font, dark-mode, and TalkBack observations. Resolve current
stable APIs through Context7 before changing dependencies or navigation.

Milestone 2 implements the typed settings repository and theme integration.
Add one DataStore instance, safe defaults/migrations, system/light/dark mode,
dynamic color gating, and comfortable/compact density tokens. Apply settings at
the app root without restarting the activity or leaking platform types into
domain/UI state.

Milestone 3 completes the top-level adaptive shell. Integrate Search, Library,
Releases, and Settings with stable compact/expanded navigation, correct back and
deep-link behavior if deep links already exist, state restoration, and list-
detail layouts. Verify resize and fold changes rather than relying only on a
width helper unit test.

Milestone 4 delivers Settings. Add sync controls wired to the existing scheduler,
cache-size summaries and safe clear actions, account/logout controls, open-
source notices, app version, unofficial status, MangaUpdates credit, and links
only where clearly appropriate. Every destructive action uses confirmation and
reports success/failure accessibly.

Milestone 5 performs screen-by-screen accessibility and visual polish. Correct
semantics, traversal, focus, contrast, touch targets, large text clipping,
loading/error announcements, image descriptions, stable list keys, image
sizing, recomposition hotspots found through inspection, and landscape/foldable
behavior. Do not introduce custom optimizations without measurement.

Milestone 6 validates and documents. Inspect the complete diff; run host and
device gates; execute a manual TalkBack, keyboard/D-pad, 200% text, dark/light,
dynamic-color, rotation, resize, offline, cache-clear, and process-recreation
matrix; and update README, architecture, security, testing, project-state, and
this plan with evidence.

## Testing requirements

Unit-test settings serialization/defaults, every update, concurrent edits,
unknown enum values, corruption policy, migrations, and Flow emissions with
temporary stores and Turbine. Test theme/density mapping and scheduler reactions
with fake clocks and fake work schedulers. Verify disabling periodic refresh
cancels only release work and does not touch library outbox work.

Repository/DAO tests cover each cache-clear boundary, transaction rollback,
preservation of account/session/outbox state, account-data confirmation paths,
and accurate post-clear summaries. Licence generation should have a build-time
verification that required metadata is present and the generated notice is
packaged.

Compose tests cover top-level navigation and back behavior, compact bottom
navigation, expanded rail, selected semantics, route restoration, theme and
density changes, sync controls, cache confirmation/result, account/logout,
licences, unofficial notice, and strings at 200% font scale. Add targeted
semantics assertions for meaningful images, decorative images, headings,
validation errors, progress, sync failure, live-region announcements, touch
targets, and traversal.

Run the complete host gate from plan 001 and actual device tests on at least one
compact and one 600 dp-or-wider configuration. Exercise resize/fold behavior on
a resizable emulator or foldable profile. Manual evidence must cover TalkBack,
external keyboard or D-pad where available, high text/display scale, light/dark
and dynamic color, offline states, cache clearing, and restart restoration.
Record unavailable hardware separately; do not report compilation as execution.

## Progress

- [x] (2026-07-14 07:24Z) Confirmed the Phase 2 app already uses Material 3
  Adaptive window information, Navigation 3, localized resources, and an
  explicit 600 dp list/detail layout.
- [x] (2026-07-14 07:24Z) Queried configured Context7 for DataStore concepts and
  recorded the need for a version-specific stable implementation audit.
- [ ] Inventory the integrated post-plans-002-through-004 routes, settings,
  caches, strings, adaptive behavior, and accessibility gaps.
- [ ] Implement typed DataStore settings and root theme/density behavior.
- [ ] Complete compact/expanded top-level navigation and state restoration.
- [ ] Implement sync, cache, account, licence, credit, and unofficial settings.
- [ ] Complete the accessibility/adaptive review and all host/device gates.

## Decision log

- Decision: Keep secrets out of Preferences DataStore.
  Rationale: Plan 002 provides authenticated encryption; preferences are for
  non-sensitive user choices only.
  Date/Author: 2026-07-14, Codex.
- Decision: Use one application-scoped settings store and typed repository.
  Rationale: Multiple DataStore instances for one file are unsafe and would
  spread serialization/default rules through UI code.
  Date/Author: 2026-07-14, Codex.
- Decision: Preserve local library intent during ordinary cache clearing.
  Rationale: A cache maintenance action must not silently delete pending user
  changes or log out the account.
  Date/Author: 2026-07-14, Codex.
- Decision: Require stable Navigation/Adaptive APIs for the production shell.
  Rationale: The existing explicit layout works; adopting an experimental API
  without a concrete benefit would add avoidable migration risk.
  Date/Author: 2026-07-14, Codex.

## Unexpected discoveries

- Observation: Material 3 Adaptive is already present, but the first slice uses
  only window classification plus an explicit two-pane `Row`.
  Evidence: `KirokuApp.kt` calls `currentWindowAdaptiveInfo()` and constructs
  its own weighted panes.
- Observation: The repository has no general preferences store yet.
  Evidence: the Phase 2 dependency catalog and source inventory contain no
  DataStore artifact or settings repository.
- Observation: Context7 DataStore results may include beta or current-main
  snippets alongside stable concepts.
  Evidence: the 2026-07-14 query was not sufficiently version-constrained;
  implementation must resolve the selected stable artifact again.

## Outcomes & Retrospective

No implementation milestone has completed. The integration, persistence, and
accessibility boundaries are defined; replace this section with observed phone,
large-screen, settings, accessibility, and gate results after completion.

## Remaining work

All implementation remains and depends on the actual routes/repositories
delivered by plans 002 through 004. Do not pre-scaffold settings for unfinished
features. Begin by auditing the integrated UI and current stable documentation,
then keep this plan updated as concrete gaps replace the baseline assumptions.

Revision note (2026-07-14): Initial self-contained settings, adaptive-shell,
and accessibility plan created from the Phase 2 app. No application
implementation was changed.
