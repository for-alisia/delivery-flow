# GitlabFlow Project Overview

## Purpose

GitlabFlow builds a flow orchestration capability for enterprise Agile delivery teams.
It helps teams understand delivery state, identify bottlenecks, and improve release predictability.

GitLab is the MVP integration provider. The architecture is integration-agnostic so additional providers can be added later.

## Product Components

- `flow-orchestrator/`: Java Spring Boot application — orchestration logic and business behavior.
- `mcp-server/`: TypeScript MCP server — thin consumer of `flow-orchestrator`, no business logic.

## Core Business Concepts

- **Work item**: GitLab issue (MVP).
- **Delivery cycle**: GitLab milestone (MVP).
- **Workflow state source**: GitLab status labels (MVP).
- **Default phases**: planning, development, done.

## Capability Roadmap

Current and planned capabilities, ordered by implementation priority.
The Architect uses this to anticipate reuse opportunities across capabilities.

| # | Capability | Status | GitLab Resources Used |
|---|-----------|--------|----------------------|
| 1 | **Issues** — search, create, delete, get-single, label-event history | Implemented | `issues`, `resource_label_events` |
| 2 | **Milestones** — list, get-single, milestone-scoped issues | Planned | `milestones`, `issues?milestone=` |
| 3 | **Aging and flow metrics** — age calculation, bottleneck detection, label-based phase tracking | Planned | `issues`, `resource_label_events`, `milestones` |
| 4 | **Merge requests** — list, status, review state | Planned | `merge_requests` |
| 5 | **Release notes** — generate from milestone + issues + MR data | Planned | `milestones`, `issues`, `merge_requests` |
| 6 | **Pipelines** — status, failure tracking | Future | `pipelines`, `jobs` |

Each new capability follows the package structure in `code-guidance.md` and reuses shared GitLab infrastructure from `context-map.md` → "Shared Infrastructure".

## Product Constraints

- The orchestration application is the source of business truth.
- The MCP server must not re-implement business orchestration.
- All other architectural constraints live in `constitution.md`.

## Related Documentation

- Architecture rules: `documentation/constitution.md`
- Structural decisions: `documentation/architecture-guidance.md`
- Coding standards: `documentation/code-guidance.md`
- Codebase map: `documentation/context-map.md`
