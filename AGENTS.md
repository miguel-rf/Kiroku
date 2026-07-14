# Project agent instructions

## Context7

Use the project-configured Context7 MCP server when current third-party
documentation is material, especially when:

- adding or upgrading a dependency;
- configuring Gradle, KSP, Room, Paging, Navigation 3, Material 3 Adaptive,
  WorkManager, or security behavior;
- calling a library API not already established in this repository;
- resolving a compilation error related to API or version drift.

Resolve the library ID before querying its documentation. Prefer version-
specific, primary library documentation returned by Context7. If Context7 is
temporarily unavailable, use the library author's primary documentation and
record the fallback rather than guessing.

Never put a Context7 API key in this repository. The optional
CONTEXT7_API_KEY environment variable is forwarded by the MCP configuration.

## Living ExecPlans

Long-running work is governed by `docs/PLANS.md` and the numbered plans under
`docs/plans/`. Before implementation, read the plan index, the selected plan,
`docs/PROJECT_STATE.md`, and every repository file named by that plan. A plan
must be usable without access to an earlier conversation.

Keep the active plan current while working. Update its timestamped `Progress`,
`Decision log`, `Unexpected discoveries`, `Outcomes & Retrospective`,
`Remaining work`, and bottom revision note whenever evidence or scope changes.
Report only commands and tests that actually ran. If implementation diverges
from a planned API contract, schema version, dependency, or architecture, revise
the plan before or with the code and explain the evidence.

## Terminal output

Prefer RTK commands for output that will be read by an agent. Use raw commands
when exact output is required or the wrapper would break the workflow.
