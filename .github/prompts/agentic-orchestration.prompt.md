---
agent: 'agent'
description: 'IDE-only context guide for understanding the GitlabFlow agent orchestration system using linked repository artifacts'
---

Use this IDE-only prompt file to explain the current GitlabFlow agent orchestration system.
Treat it as a context-collection and explanation aid for the orchestration goal, flow, and artifacts.
Do not treat it as the runtime source of truth for agent execution; the agent files and shared artifacts remain the authoritative workflow contract.

Use this repository context to explain the current GitlabFlow agent orchestration system.
Keep the response concise, structured, and grounded in the linked files instead of repeating long prompt contents.

## Goal

Explain the purpose of the orchestration system for this repository:

- turn a user request into verified delivery for `flow-orchestrator`
- enforce enterprise-oriented planning, implementation, and verification
- keep planning, coding, and quality decisions traceable through repository artifacts

## Current Flow

### List of all custom agents (/.github/agents)

1. `Team Lead` receives the request and coordinates the workflow.
2. `Product Manager` creates the business-facing user story.
3. `Java Architect` creates the slice-based implementation plan.
4. `Java Coder` implements the plan, verifies the change, and updates the implementation report.
5. `Team Lead` performs final acceptance and reports status back to the user.

`Team Lead` is the workflow quality gate owner: it must not advance stages without artifact evidence, and it must not close work without verified post-implementation checks.

### Team Lead

Use [Team Lead Agent](../agents/team-lead.agent.md).

Focus on:

- orchestration of the full delivery flow
- coordination between agents
- handoff quality gates between story, plan, implementation, and acceptance
- final acceptance against the original request
- deviation approval handling and evidence-based verification review

### Product Manager

Use [Product Manager Agent](../agents/product-manager.md) and [Project Overview](../../artifacts/project-overview.md).

Focus on:

- turning the request into a clear business story
- defining business goal, scope, and acceptance criteria
- producing `artifacts/user-stories/<feature-name>.story.md`

### Java Architect

Use [Java Architect Agent](../agents/java-architect.agent.md), [Constitution](../../artifacts/constitution.md), [Code Guidance](../../artifacts/code-guidance.md), and [Implementation Plan Template](../../artifacts/templates/implementation-plan-template.md).

Focus on:

- defining a short, executable implementation plan
- choosing explicit class structure
- splitting work into implementation slices
- defining required verification per slice
- producing `artifacts/implementation-reports/<feature-name>.md`

### Java Coder

Use [Java Coder Agent](../agents/java-coder.agent.md), [Code Guidance](../../artifacts/code-guidance.md), and [Constitution](../../artifacts/constitution.md).

Focus on:

- executing the provided implementation plan exactly
- implementing slice by slice
- adding and running required tests
- running final verification including tests, compile, startup, and `curl` smoke checks
- updating the implementation report with implementation summary, deviations, and verification outcomes

## Handoff Quality Expectations

The orchestration flow depends on artifact-backed handoffs:

- `Product Manager` must produce a business-ready story with concrete acceptance criteria.
- `Java Architect` must produce an executable plan with explicit slices and required verification.
- `Java Coder` must update the implementation report with exact verification commands, outcomes, and approved deviations.
- `Team Lead` must inspect those artifacts, reject incomplete handoffs, and perform evidence-based final acceptance.

## Research Artifacts

Use these files as the primary research set when explaining the current orchestration system:

- [Copilot Instructions](../copilot-instructions.md)
- [Team Lead Agent](../agents/team-lead.agent.md)
- [Product Manager Agent](../agents/product-manager.md)
- [Java Architect Agent](../agents/java-architect.agent.md)
- [Java Coder Agent](../agents/java-coder.agent.md)
- [Project Overview](../../artifacts/project-overview.md)
- [Constitution](../../artifacts/constitution.md)
- [Code Guidance](../../artifacts/code-guidance.md)
- [Implementation Plan Template](../../artifacts/templates/implementation-plan-template.md)


When answering, explain the system at a high level first, then describe each agent clearly, and keep the output traceable to the linked files.
