# GitlabFlow Project Overview

## Purpose

GitlabFlow builds a flow orchestration capability for enterprise Agile delivery teams.
Its main objective is to help teams understand delivery state, identify bottlenecks, and improve release predictability.

For the MVP, GitLab is the primary external system. The long-term product direction is integration-agnostic orchestration that can support additional providers in the future.

## Product Components

- `flow-orchestrator/`: Java Spring Boot application that owns orchestration logic and business behavior.
- `mcp-server/`: TypeScript MCP server that acts as a thin consumer of `flow-orchestrator`.

## Problem We Solve

Enterprise teams often lack a single, reliable view of delivery flow health across work items and release cycles.
Manual status checks are slow, inconsistent, and reactive. GitlabFlow provides structured insights and automation to improve visibility and decision making.

## Target Users and Stakeholders

- Delivery managers and engineering managers who need flow visibility and bottleneck detection.
- Agile teams who need clear aging and release-related insights.
- Platform or enablement teams who need consistent, repeatable release intelligence.

## Product Goals

- Provide reliable visibility into open and aging work items.
- Highlight flow inefficiencies and probable bottlenecks.
- Support release planning, release tracking, and release documentation generation.
- Keep orchestration logic portable so additional integration providers can be added later.

## Functional Scope

### Current and Near-Term Capabilities

- Return open work items to clients.
- Calculate aging metrics and identify oldest work items.
- Support age-based labeling recommendations or automation.
- Generate flow insights and bottleneck indicators.
- Build release notes from work items included in a release cycle.
- Support release planning and progress updates.
- Generate release documentation from release-cycle and work-item data.

### Core Business Concepts

- **Work item (MVP)**: GitLab issue.
- **Delivery cycle item (MVP)**: GitLab milestone.
- **Workflow state source (MVP)**: GitLab status labels.
- **Default high-level phases**: planning, development, done.

These choices are MVP defaults, not hard platform limits.

## Product Constraints and Principles

- The orchestration application is the source of business truth.
- The MCP server must stay thin and must not re-implement business orchestration.
- Domain and orchestration must remain provider-agnostic over time.
- Security, validation, error sanitization, and boundary mapping rules are mandatory.

For non-negotiable architecture rules, see `documentation/constitution.md`.

## Guidance for Product Manager Agent

When creating a user story in `artifacts/user-stories/<feature-name>.story.md`:

- Start from business outcome first, not implementation shape.
- State the actor, problem, and measurable value clearly.
- Write explicit acceptance criteria with observable outcomes.
- Separate in-scope and out-of-scope behavior to avoid scope drift.
- Flag assumptions and open questions instead of guessing.
- Preserve integration-agnostic product intent even when GitLab-specific details are used for MVP examples.

## Recommended User Story Quality Bar

- Business goal is specific and tied to delivery-flow improvement.
- Acceptance criteria are testable and unambiguous.
- Success path and key validation/error outcomes are both covered.
- Story language is understandable by both technical and non-technical stakeholders.

## Related Documentation

- `README.md` for run and module basics.
- `documentation/constitution.md` for hard architecture and quality rules.
- Official GitLab REST API docs for GitLab endpoint, parameter, and authentication details:
  - `https://docs.gitlab.com/api/api_resources/`
  - `https://docs.gitlab.com/api/rest/`
