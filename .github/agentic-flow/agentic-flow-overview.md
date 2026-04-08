# Agentic Flow Overview

## Purpose

- Turn a user request into verified delivery for `flow-orchestrator`
- Separate planning, implementation, and independent review
- Keep decisions and evidence traceable through repository artifacts
- Prevent weak handoffs, false-positive verification, and context drift

## End-To-End Flow

1. `Team Lead` locks requirements and owns workflow state.
2. `Product Manager` writes the business-facing user story.
3. `Java Architect` writes the executable implementation plan.
4. `Reviewer` runs Phase 1 validation on request, story, and plan.
5. `Java Coder` implements approved plan slices and records evidence.
6. `Reviewer` runs Phase 2 validation on code, tests, and runtime evidence.
7. `Team Lead` audits artifacts, reruns key checks, and decides final acceptance.

## Agent Responsibilities

### `Team Lead`

- Workflow owner and final acceptance gate
- Maintains requirement lock and shared checkpoint
- Verifies artifacts before every stage transition
- Runs `final-check.sh`, app startup, and Karate smoke tests (`karate-test.sh`) after coder batches
- Owns verification log and implementation report evidence writing
- Enforces coder batching, red cards, and architect escalation

### `Product Manager`

- Converts locked request into a business-facing user story
- Preserves scope, constraints, assumptions, and acceptance criteria
- Stays implementation-agnostic

### `Java Architect`

- Produces the implementation plan for `Java Coder`
- Defines class structure, payload examples, validation boundaries, slices, tests, and documentation updates
- Writes Karate `.feature` files directly when the plan adds or changes API endpoints
- Uses repository rules and external docs when behavior is uncertain

### `Reviewer`

- Independent validation gate
- Phase 1: validates prompt, story, plan, and rule compliance before coding
- Phase 2: validates TL evidence, reviews code quality (naming, model justification, duplication, over-engineering, log context, test quality), and compares implementation to plan
- Reruns scripts only when TL evidence is missing or suspect
- Writes the review report and fails unverifiable claims

### `Java Coder`

- Implements the plan exactly, slice by slice
- Adds required tests and runs verification (`verify-quick.sh` per slice, `final-check.sh` before handoff)
- Does NOT write Karate `.feature` files or run Karate tests — that belongs to Architect and Team Lead respectively
- Returns changed files, status, deviations, and blockers after every batch
- Does not invent extra scope or self-certify weak evidence

## Shared Control Rules

- Use `/memories/session/<feature-name>-checkpoint.json` as the shared context source
- Invoke agents from checkpoint-based fresh context, not accumulated chat state
- Do not advance on chat claims; validate artifacts on disk
- `Reviewer` is the primary technical gate; `Team Lead` performs audit-style spot checks
- `Java Coder` is limited to 2 slices per invocation
- Repeated coder false positives trigger architect plan revision instead of endless retries

## Main Artifacts

- Requirement checkpoint: `/memories/session/<feature-name>-checkpoint.json`
- Story: `artifacts/user-stories/<feature-name>.story.md`
- Plan: `artifacts/implementation-plans/<feature-name>.plan.md`
- Implementation report: `artifacts/implementation-reports/<feature-name>.report.json`
- Verification log: `artifacts/implementation-reports/<feature-name>-verification.log`
- Review report: `artifacts/review-reports/<feature-name>.review.json`
- Final sign-off: `artifacts/implementation-signoffs/<feature-name>.signoff.json`

## Verification Expectations

- `Java Coder` runs `verify-quick.sh` per slice and `final-check.sh` before handoff
- `Team Lead` runs `final-check.sh` as independent recheck, starts the app, runs `karate-test.sh`, and owns verification log and implementation report evidence
- `Reviewer` validates code quality, reviews evidence, and reruns checks only when TL evidence is suspect

## Source Files

- [Team Lead Agent](../agents/team-lead.agent.md)
- [Product Manager Agent](../agents/product-manager.agent.md)
- [Java Architect Agent](../agents/java-architect.agent.md)
- [Reviewer Agent](../agents/reviewer.agent.md)
- [Java Coder Agent](../agents/java-coder.agent.md)
