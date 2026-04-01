---
agent: 'agent'
description: 'Analyze and assess the effectiveness of the GitlabFlow agent orchestration system, identify gaps, and suggest improvements.'
---

Analyze the context of my agent orchestration of the project. Feel free to search github documentation to get up to date information about agents in copilot (you can use 'github_repo' if needed). Your goal is to asses effectiveness of such agents team, understand the flow and identify possible gaps and strong skills, consider best prompting techniques and suggest how to increase my agent's productivity. Main goal to thave robust solution written exactly based on my prompt and following all guidance provided for agents. Take your time and make deep research - provire clear and structured analisys plus improvement plan. Use diagrams where needed and explain concise and human-readable, not like scientific article - I should be able to understand your report from the first glance.

Keep attention to "context rotting" issues - it can be a blocker via unhandled context compacting

Use this repository context to base your analysis of the current GitlabFlow agent orchestration system.
Keep the response concise, structured, and grounded in the linked files.

Check previous iterations of this agentic-setup here: /.github/agentic-flow/logs - there you canfind notes and analysis of previous runs, which can be useful for understanding the evolution of the system and identifying persistent issues or improvements.

You response should include: Current gaps and issues, string sides and Improvements - how and what to fix. Keep in mind that it's evolving project where the goal of each next iteration to be a bit better than previous one.

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
`Team Lead` also owns coder verification rechecks and may issue a `Java Coder` red card when coder-claimed successful verification is contradicted by Team Lead rerun evidence.
`Team Lead` must independently run `mvn test` after coder-claimed test success before Reviewer Phase 2.
`Team Lead` must limit `Java Coder` to at most 2 slices per invocation and verify each batch before continuing.
If the same feature receives 2 `Java Coder` false-positive red cards, `Team Lead` must route the work back to `Java Architect` for plan revision instead of retrying the coder on the same plan.
`Team Lead` must maintain a shared checkpoint at `/memories/session/<feature-name>-checkpoint.json`, refresh it after each major stage, and invoke every agent using only that checkpoint plus the agent's own instructions.

### Team Lead

Use [Team Lead Agent](../agents/team-lead.agent.md).

### Product Manager

Use [Product Manager Agent](../agents/product-manager.md) and [Project Overview](../../artifacts/project-overview.md).

### Java Architect

Use [Java Architect Agent](../agents/java-architect.agent.md)

### Reviewer

Use [Reviewer Agent](../agents/reviewer.agent.md)

### Java Coder

Use [Java Coder Agent](../agents/java-coder.agent.md)

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
- [Context Checkpoint Template](../../artifacts/templates/context-checkpoint-template.json)

When answering, explain the system at a high level first, then describe each agent clearly, and keep the output traceable to the linked files.
