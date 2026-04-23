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
4. `Architecture Reviewer` evaluates the plan as a severity-based risk gate.
5. `Java Coder` implements approved plan slices and records evidence.
6. `Code Reviewer` validates implementation, tests, and verification evidence.
7. `Team Lead` audits artifacts, reruns key checks, and decides final acceptance.

## Agent Responsibilities

### `Team Lead`

- Workflow owner and final acceptance gate
- Maintains requirement lock and flow-log state (`artifacts/flow-logs/<feature-name>.json`)
- Verifies artifacts before every stage transition via `flow-log status`
- Re-runs only stale or suspect verification checks after coder batches via `flow-log run-check`; the karate script reuses a healthy local app when available or starts it automatically
- Records verification evidence via `flow-log run-check` when TL executes an independent check
- Tracks batches via `flow-log start-batch` / `complete-batch`
- Enforces coder batching, red cards (via `flow-log reset-checks`), and architect escalation

### `Product Manager`

- Converts locked request into a business-facing user story
- Reads context via `flow-log summary`
- Preserves scope, constraints, assumptions, and acceptance criteria
- Seeds story `External Contracts` with compact concrete request / response / error examples when payload shape is easy to misread
- Stays implementation-agnostic

### `Java Architect`

- Produces the implementation plan for `Java Coder`
- Reads context via `flow-log summary`
- Produces a slice-first `v4` plan with shared rules, shared decisions, slice units, done criteria, and final verification expectations
- Uses story `External Contracts` as the single source of truth for external payloads instead of duplicating them in the plan, and tightens that story section with compact concrete examples when prose alone is not safe enough (documentation updates are **not** in plan scope — handled by Code Reviewer)
- Writes Karate `.feature` files directly when the plan adds or changes API endpoints. Coder may only adjust existing Karate tests for small payload or endpoint changes (field names, URL paths, status codes, request/response bodies).
- Defines new ArchUnit rules when the plan introduces new layer interactions or package boundaries not covered by existing rules in `FlowOrchestratorArchitectureTest.java`
- Uses repository rules and external docs when behavior is uncertain
- On plan revision: follows TL-specified revision scope (`LOCAL_CORRECTION` / `SECTION_REWRITE` / `FULL_REPLAN`)

### `Architecture Reviewer`

- Pre-implementation architectural risk gate
- Reads context via `flow-log summary` — including existing risks and Architect's response notes
- Records findings via `flow-log add-risk` with required `--plan-ref` slice / unit / shared-rule IDs; on re-review, resolves or reopens risks via `flow-log resolve-risk` / `reopen-risk`
- Does not assign severity — describes what is violated and consequences; TL classifies via `reclassify-risk` before running the gate
- Verifies that new layer interactions or package boundaries are covered by ArchUnit rules in the plan
- Does not edit any artifact

### Architecture Review Loop

1. TL runs `increment-round`, invokes Architecture Reviewer
2. Reviewer records findings via `add-risk` (all UNCLASSIFIED — describes violation + consequences), returns outcome
3. TL classifies each finding's severity via `reclassify-risk` based on project needs and delivery cost
4. TL runs `architecture-gate`: `PASS` → proceed; `FAIL` → route to Architect; `ESCALATE` → TL escalation decision
5. Architect reads risks from `flow-log summary`, responds via `respond-risk` (ADDRESSED/INVALIDATED with note)
6. TL routes back to Reviewer — Reviewer reads Architect's notes, resolves or reopens each risk
7. Repeat until gate is `PASS` or `ESCALATE` (max 3 rounds)

**On ESCALATE**, TL evaluates each unresolved risk and logs a decision via `add-event --type archEscalationDecision`:
- `PROCEED_TO_CODING` — findings are non-blocking (artifact quality, nice-to-have)
- `FINAL_ADJUSTMENT` — route to Architect for targeted fix, then skip review and proceed to coding
- `ESCALATE_TO_USER` — constitution risk or fundamental blocker, stop and report

### `Code Reviewer`

- Post-implementation validation gate
- Reads context via `flow-log summary`
- Validates flow-log evidence, reviews code quality (naming, model justification, duplication, over-engineering, log context, test quality), and compares implementation to plan
- Records material code findings via `flow-log add-finding`; on re-review, resolves or reopens via `flow-log resolve-finding` / `reopen-finding`
- Classifies findings by severity: Critical, High, Medium, Low. Critical/High block acceptance; Medium/Low are advisory
- Reruns scripts only when flow-log evidence is missing or suspect
- Updates documentation (`capabilities/<capability>.md`, `.http` examples) after reviewing implementation

### Code Review Loop

1. TL runs `increment-code-review-round`, invokes Code Reviewer
2. Reviewer records findings via `add-finding`, returns outcome
3. TL runs `code-review-gate`: `PASS` → proceed to audit; `FAIL` → route to Coder; `ESCALATE` → stop, report to user
4. Coder reads findings from `flow-log summary`, responds via `respond-finding` (FIXED/DISPUTED with note)
5. TL routes back to Reviewer — Reviewer reads Coder's notes, resolves or reopens each finding
6. Repeat until gate is `PASS` or `ESCALATE` (max 3 rounds)

### `Java Coder`

- Implements the plan exactly, slice by slice
- Reads context via `flow-log summary`
- Uses `summary.batches.current.slices` as the default intake path, reads each active slice via `plan-get --slice`, uses `summary.batches.current.changedFiles` as the owned handoff file list, and loads story contracts only through `story-get --section external-contracts` when needed, including compact concrete examples when present
- Adds required tests and runs verification via `flow-log verify --profile batch` per slice (`verifyQuick` → `finalCheck`), then `verify --profile full` before handoff (adds `karate`)
- Runs `scripts/coder-handoff-check.sh <feature-name>` before returning to TL — fixes any failures first
- Runs `mvn -q spotless:apply` in `flow-orchestrator/` when formatting violations are reported
- Records checks via `flow-log run-check` (runs script + records result) and changed files via `flow-log add-change`
- When re-invoked to fix code review findings: reads OPEN/REOPENED findings from `flow-log summary`, responds via `flow-log respond-finding` (FIXED/DISPUTED with note)
- May adjust existing Karate `.feature` files for small payload or endpoint changes only (field names, URL paths, status codes, request/response bodies). Does not add scenarios, remove scenarios, or change test logic — those belong to Architect
- Returns changed files, status, deviations, and blockers after every batch
- Does not invent extra scope or self-certify weak evidence

## Shared Control Rules

- Use `flow-log` CLI (`scripts/flow-log.sh`) as the shared state source. The state file at `artifacts/flow-logs/<feature-name>.json` is the single source of truth for delivery state and gate readiness
- Plans use a single slice-first schema version (`4.0`). Archived schemas are not part of the active workflow.
- Story `External Contracts` is the single source of truth for external request / response details; when contract shape is easy to misread it should include compact concrete request / response / error examples, and downstream agents load it via `story-get --section external-contracts`.
- Architectural risks are durable state in flow-log — Reviewer records them, Architect responds to them, Reviewer resolves or reopens them
- TL uses `architecture-gate` to determine readiness: `PASS`, `FAIL`, or `ESCALATE` (3 unresolved rounds hard cap)
- Code findings are durable state in flow-log — Code Reviewer records them, Coder responds to them, Code Reviewer resolves or reopens them
- TL uses `code-review-gate` to determine readiness: `PASS`, `FAIL`, or `ESCALATE` (3 unresolved rounds)
- Invoke agents with feature name; agents query `flow-log summary` to load context
- TL passes active slice IDs when invoking `Java Coder` or slice-scoped review work
- Do not advance on chat claims; validate artifacts on disk and flow-log state
- `Architecture Reviewer` and `Code Reviewer` are the technical gates; `Team Lead` performs audit-style spot checks
- `Java Coder` is limited to 2 slices per invocation
- Repeated coder false positives trigger architect plan revision instead of endless retries

## Main Artifacts

- Flow-log state: `artifacts/flow-logs/<feature-name>.json`
- Story: `artifacts/user-stories/<feature-name>.story.md`
- Plan: `artifacts/implementation-plans/<feature-name>.plan.json`

## Verification Expectations

- `Java Coder` runs `verify --profile batch` per slice (`verifyQuick` → `finalCheck`), then `verify --profile full` (adds `karate`) before handoff, and `coder-handoff-check.sh` as pre-handoff gate
- `Team Lead` checks `storyStale`, `planStale`, and `*Stale` fields in `flow-log status`. If `finalCheckStale` or `karateStale` is true, re-runs only the stale check. If only `verifyQuickStale` is true but `finalCheck` is PASS and not stale, staleness is non-blocking. Fresh coder evidence is trusted by default instead of triggering automatic TL reruns.
- `Code Reviewer` validates code quality, reviews flow-log evidence including staleness flags, and reruns checks only when evidence is suspect or stale
- ArchUnit tests run as part of `mvn test` (verify-quick) — architecture boundary violations break the build deterministically without requiring manual review

## Source Files

- [Team Lead Agent](../agents/team-lead.agent.md)
- [Product Manager Agent](../agents/product-manager.agent.md)
- [Java Architect Agent](../agents/java-architect.agent.md)
- [Architecture Reviewer Agent](../agents/architecture-reviewer.agent.md)
- [Code Reviewer Agent](../agents/reviewer.agent.md)
- [Java Coder Agent](../agents/java-coder.agent.md)
