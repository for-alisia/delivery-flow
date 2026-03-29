---
agent: 'agent'
description: 'Analyze and assess the effectiveness of the GitlabFlow agent orchestration system, identify gaps, and suggest improvements.'
---

Analyze the context of my agent orchestration of the project. Feel free to search github documentation to get up to date information about agents in copilot (you can use 'github_repo' if needed). Your goal is to asses effectiveness of such agents team, understand the flow and identify possible gaps and strong skills, consider best prompting techniques and suggest how to increase my agent's productivity. Main goal to thave robust solution written exactly based on my prompt and following all guidance provided for agents. Take your time and make deep research - provire clear and structured analisys plus improvement plan. Use diagrams where needed and explain concise and human-readable, not like scientific article - I should be able to understand your report from the first glance.

Keep attention to "context rotting" issues - it can be a blocker via unhandled context compacting

Use this repository context to base your analysis of the current GitlabFlow agent orchestration system.
Keep the response concise, structured, and grounded in the linked files.

## Goal

The system architecture goal is to:

- turn a user request into verified delivery for `flow-orchestrator`
- enforce enterprise-oriented planning, implementation, and independent review
- keep planning, coding, and quality decisions traceable through repository artifacts
- ensure unfalsifiable evidence of verification via terminal logs
- enforce circuit breakers to prevent infinite looping during task handoffs

## Current Flow

### List of all custom agents (/.github/agents)

1. `Team Lead` receives the request, creates the requirement lock, and coordinates the workflow.
2. `Product Manager` creates the business-facing user story.
3. `Java Architect` creates the slice-based implementation plan.
4. `Reviewer` performs Phase 1 review of the original prompt, story, plan, and constitution before coding starts.
5. `Java Coder` implements the plan, verifies the change, and updates the implementation report.
6. `Reviewer` performs Phase 2 review of code, tests, verification commands, and runtime behavior.
7. `Team Lead` performs artifact audit, reviewer spot checks, final acceptance, and reports status back to the user.

`Team Lead` is the workflow owner and final acceptance gate, but `Reviewer` is the primary technical validation gate. `Team Lead` must not advance stages without artifact evidence, and must not close work while any reviewer item remains failed or blocked.

### Team Lead

Use [Team Lead Agent](../agents/team-lead.agent.md).

Focus on:

- orchestration of the full delivery flow
- requirement lock and source-of-truth preservation
- coordination between agents
- handoff quality gates between story, plan, review, implementation, and acceptance
- artifact audit and reviewer spot checks
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
- bundling all Acceptance Criteria from the story into the bottom of the plan
- choosing explicit class structure
- splitting work into implementation slices
- defining required verification per slice
- preserving the locked request constraints from the original prompt
- producing `artifacts/implementation-reports/<feature-name>.md`

### Reviewer

Use [Reviewer Agent](../agents/reviewer.agent.md), [Constitution](../../artifacts/constitution.md), [Code Guidance](../../artifacts/code-guidance.md), and [Review Report Template](../../artifacts/templates/review-report-template.md).

Focus on:

- validating prompt-to-story-to-plan alignment before coding
- validating code, Sonar results, tests, verification evidence (via `<feature-name>-verification.log`), and runtime behavior
- using circuit breakers (emitting `<<ESCALATE_TO_ARCHITECT>>`) if Phase 2 is rejected 3 times
- producing `artifacts/review-reports/<feature-name>.review.md`
- blocking the workflow when requirements drift, quality gates fail, or verification evidence is incomplete

### Java Coder

Use [Java Coder Agent](../agents/java-coder.agent.md), [Code Guidance](../../artifacts/code-guidance.md), and [Constitution](../../artifacts/constitution.md).

Focus on:

- executing the provided implementation plan exactly (interpreting bundled Acceptance Criteria from the plan)
- implementing slice by slice
- adding and running required tests
- using `run_in_terminal` to record unfalsifiable verification evidence into `artifacts/implementation-reports/<feature-name>-verification.log` (tests, Sonar, compile, smoke checks)
- updating the implementation report with implementation summary, deviations, and verification outcomes

## Handoff Quality Expectations

The orchestration flow depends on artifact-backed handoffs:

- `Team Lead` must create and preserve a requirement lock so non-negotiable request constraints are not silently normalized away.
- `Product Manager` must produce a business-ready story with concrete acceptance criteria.
- `Java Architect` must produce an executable plan with explicit slices, bundled Acceptance Criteria, and required verification.
- `Java Coder` must update the implementation report and capture unfalsifiable terminal execution evidence in `artifacts/implementation-reports/<feature-name>-verification.log`.
- verification artifacts must identify the reviewed revision, redact secrets, and be backed by file-based terminal logs.
- `Reviewer` must produce a checklist-driven review report, validate against the log evidence, and trigger circuit breakers (`<<ESCALATE_TO_ARCHITECT>>`) on repeated failures.
- `Team Lead` must inspect those artifacts, reject incomplete handoffs, audit reviewer output, and perform evidence-based final acceptance.

## Research Artifacts

Use these files as the primary research set when explaining the current orchestration system:

- [Copilot Instructions](../copilot-instructions.md)
- [Team Lead Agent](../agents/team-lead.agent.md)
- [Product Manager Agent](../agents/product-manager.md)
- [Java Architect Agent](../agents/java-architect.agent.md)
- [Reviewer Agent](../agents/reviewer.agent.md)
- [Java Coder Agent](../agents/java-coder.agent.md)
- [Project Overview](../../artifacts/project-overview.md)
- [Constitution](../../artifacts/constitution.md)
- [Code Guidance](../../artifacts/code-guidance.md)
- [Implementation Plan Template](../../artifacts/templates/implementation-plan-template.md)
- [Review Report Template](../../artifacts/templates/review-report-template.md)
- [Implementation Sign-off Template](../../artifacts/templates/implementation-signoff-template.md)


When answering, explain the system at a high level first, then describe each agent clearly, and keep the output traceable to the linked files.
