# Kiroku living ExecPlans

This file defines how the numbered execution plans under `docs/plans/` are
used. It follows OpenAI's current guidance for long-running Codex work: an
ExecPlan is a self-contained, outcome-focused living document that a new agent
can execute with only the repository and that plan. The upstream convention is
the [OpenAI ExecPlan guide](https://developers.openai.com/cookbook/articles/codex_exec_plans).

## How to use these plans

Before implementation, read `AGENTS.md`, this file, the selected numbered plan,
and every repository file named by that plan. Start with the earliest plan whose
prerequisites are satisfied. Do not rely on a previous conversation or on facts
that are absent from the plan and working tree.

Keep the selected plan current while working. At every stopping point, update
its timestamped `Progress` checklist, record unexpected evidence under
`Unexpected discoveries`, record choices and rationale under `Decision log`,
and add an `Outcomes & Retrospective` entry at each major milestone. When the
plan changes, add a revision note at the bottom explaining what changed and why.
The plan must remain self-contained after every revision.

Every milestone must end in observable behavior and proportional verification.
Report only commands that actually ran. A compiled instrumented test is not a
passed instrumented test. Do not make production network calls from automated
tests. Use realistic fixtures, MockWebServer, fake clocks, controlled
dispatchers, temporary databases, and WorkManager's test facilities.

The checked-in `openapi.json` and official MangaUpdates documentation are the
only sources for endpoint and payload contracts. Do not scrape the website,
guess token fields, invent idempotency headers, or transfer assumptions from one
endpoint to another. Use the project-configured Context7 server before adding a
dependency or writing version-sensitive Room, Paging, Navigation, WorkManager,
DataStore, benchmark, or security code. When Context7 is incomplete, consult the
library author's primary documentation and record the fallback.

## Verified baseline

As of 2026-07-14, discovery, foundation work, and the public
search-to-series-details slice are complete on every host-executable gate.
Twenty-eight JVM tests pass, lint has no findings, Room schema version 1 is
exported, and debug, Android-test, and minified unsigned release APKs build. The
three Compose instrumented test classes have not executed because the attached
device is unauthorized and no emulator is installed. The directory is not a
usable Git worktree, so a clean-checkout build is also unverified. The detailed
evidence is embedded in plan 001 and `docs/PROJECT_STATE.md`.

The existing app is one Gradle application module, uses a manual `AppContainer`,
keeps Room as the source of truth, separates DTO/entity/domain/UI models, and
uses stable Navigation 3 plus an explicit adaptive two-pane layout. These
decisions remain in force unless a numbered plan records new evidence.

## Plan order and dependencies

1. `docs/plans/001-reproducible-validation-baseline.md` restores a trustworthy
   source-control/build/device validation baseline and adds CI. Start here.
2. `docs/plans/002-authentication-secure-session.md` implements login, secure
   session restoration, expiry, and logout. It is blocked until the official
   login success token shape is confirmed.
3. `docs/plans/003-library-offline-sync.md` implements lists, progress, the Room
   outbox, and durable WorkManager synchronization. It requires plan 002.
4. `docs/plans/004-releases-background-refresh.md` implements cached releases
   and durable refresh. Its public `GET /releases/days` slice may proceed while
   plan 002 is contract-blocked; authenticated release search waits for plan 002.
5. `docs/plans/005-settings-adaptive-accessibility.md` adds the complete app
   shell, preferences, cache controls, licences, and accessibility/adaptive
   polish. It assumes plans 002 through 004 have established their routes and
   repositories.
6. `docs/plans/006-production-hardening-release.md` performs migration,
   security, performance, baseline-profile, macrobenchmark, release, and
   clean-checkout hardening. Execute it last.

Plans 003 and 004 must not both change the Room version from the same baseline.
If releases are implemented before library work because authentication remains
blocked, the executing agent must update both plans' database-version numbers
and migration chain before editing Room. The invariant is one sequential schema
history with an exported JSON file and migration test for every transition.

## Completion rule

The project is complete only when all six plans have no remaining implementation
items, their acceptance behavior has been demonstrated, all available quality
gates pass from a clean checkout, device tests have actually run on compact and
large layouts, and any unavoidable limitation is explicitly documented rather
than reported as a pass.

Revision note (2026-07-14): Created the living-plan convention and ordered index
from the verified Phase 2 repository state. No application implementation was
changed.
