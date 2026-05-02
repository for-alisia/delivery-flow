# Agentic Flow Overview

## Purpose

- Turn a user request into verified delivery for `flow-orchestrator`
- Separate planning, implementation, and independent review
- Keep decisions and evidence traceable through repository artifacts
- Prevent weak handoffs, false-positive verification, and context drift

## End-To-End Flow

1. `Team Lead` locks requirements and owns workflow state.
2. `Product Manager` writes the business-facing user story.
3. `Team Lead` records the E2E mode: `SCENARIOS_REQUIRED` for API/smoke-coverage changes, `REUSE_EXISTING` for non-API work that can reuse the existing Karate suite.
4. `E2E Tester` writes the approved E2E scenario artifact only when mode is `SCENARIOS_REQUIRED`.
5. `Java Architect` writes the executable implementation plan.
6. `Architecture Reviewer` evaluates the plan as a severity-based risk gate.
7. `Java Coder` implements approved plan slices and records evidence.
8. Smoke verification runs through `E2E Tester` when mode is `SCENARIOS_REQUIRED`, or through `Team Lead` when mode is `REUSE_EXISTING`.
9. `Code Reviewer` validates implementation, tests, and verification evidence.
10. `Team Lead` audits artifacts, reruns key checks, and decides final acceptance.

## Agent Responsibilities

### `Team Lead`

- Workflow owner and final acceptance gate
- Maintains requirement lock and flow-log state (`artifacts/flow-logs/<feature-name>.json`)
- Verifies artifacts before every stage transition via `flow-log status`
- Decides `set-e2e-mode --mode <REUSE_EXISTING|SCENARIOS_REQUIRED>` after the story gate
- Re-runs only stale or suspect verification checks after coder slice-runs or smoke reruns via `flow-log run-check`; the karate script reuses a healthy local app when available or starts it automatically
- Records verification evidence via `flow-log run-check` when TL executes an independent check
- Tracks slice-runs via `flow-log start-slice-run --type <intermediate|final>` / `complete-slice-run`
- Tracks the approved E2E scenario artifact via `register-artifact e2e` / `approve-artifact e2e` when `e2eMode = SCENARIOS_REQUIRED`
- Enforces one-slice coder runs, red cards (via `flow-log reset-checks`), and architect escalation

### `Product Manager`

- Converts locked request into a business-facing user story
- Reads context via `flow-log summary`
- Preserves scope, constraints, assumptions, and acceptance criteria
- Seeds story `External Contracts` with compact concrete request / response / error examples when payload shape is easy to misread
- Stays implementation-agnostic

### `E2E Tester`

- Owns runtime smoke coverage in two passes when `e2eMode = SCENARIOS_REQUIRED`: scenario-design before planning, and Karate materialization/execution after implementation
- Reads context via `flow-log summary`
- Pass 1 saves `artifacts/e2e-scenarios/<feature>.e2e.md`, covering happy path, error path, and repeatability rules for mutable resources
- Pass 2 updates Karate `.feature` files and runners, records changed files via `add-change`, and records the smoke verdict via `run-check --name karate`
- Owns repeatability rules: unique data for mutable write paths, cleanup when the API supports it, and explicit diagnostics when provider state makes a scenario fail
- Does not change production code, story, or plan; if the approved E2E artifact must change, TL must re-approve it

### `Java Architect`

- Produces the implementation plan for `Java Coder`
- Reads context via `flow-log summary`
- Produces a slice-first `v4` plan with shared rules, shared decisions, slice units, done criteria, and final verification expectations
- Uses story `External Contracts` as the single source of truth for external payloads instead of duplicating them in the plan, and tightens that story section with compact concrete examples when prose alone is not safe enough (documentation updates are **not** in plan scope — handled by Code Reviewer)
- Reads the approved E2E scenario artifact and references scenario IDs in plan notes when runtime smoke coverage matters. Does not write Karate `.feature` files or runners.
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
- Acts as the interim documentation owner for `v2.5.2`; a dedicated documentation agent remains future workflow work

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
- Uses `summary.sliceRuns.current.slice` and `summary.sliceRuns.current.type` as the default intake path, reads the active slice via `plan-get --slice`, uses `summary.sliceRuns.current.changedFiles` as the owned handoff file list, and loads story contracts only through `story-get --section external-contracts` when needed, including compact concrete examples when present
- Adds required tests and runs verification via `flow-log verify --profile slice` after the owned slice work (`verifyQuick` → `finalCheck`)
- Runs `scripts/coder-handoff-check.sh <feature-name>` before returning to TL — fixes any failures first
- Runs `mvn -q spotless:apply` in `flow-orchestrator/` when formatting violations are reported
- Records checks via `flow-log run-check` (runs script + records result) and changed files via `flow-log add-change`
- When re-invoked to fix code review findings: reads OPEN/REOPENED findings from `flow-log summary`, responds via `flow-log respond-finding` (FIXED/DISPUTED with note)
- Does not write or modify Karate `.feature` files or runners — those belong to `E2E Tester`
- Returns changed files, status, deviations, and blockers after every slice-run
- Does not invent extra scope or self-certify weak evidence

## Shared Control Rules

- Use `flow-log` CLI (`scripts/flow-log.sh`) as the shared state source. The state file at `artifacts/flow-logs/<feature-name>.json` is the single source of truth for delivery state and gate readiness
- Plans use a single slice-first schema version (`4.0`). Archived schemas are not part of the active workflow.
- Story `External Contracts` is the single source of truth for external request / response details; when contract shape is easy to misread it should include compact concrete request / response / error examples, and downstream agents load it via `story-get --section external-contracts`.
- The E2E scenario artifact is approval-tracked in flow-log only when `e2eMode = SCENARIOS_REQUIRED`; if it changes later, TL re-approves it before advancing.
- Architectural risks are durable state in flow-log — Reviewer records them, Architect responds to them, Reviewer resolves or reopens them
- TL uses `architecture-gate` to determine readiness: `PASS`, `FAIL`, or `ESCALATE` (3 unresolved rounds hard cap)
- Code findings are durable state in flow-log — Code Reviewer records them, Coder responds to them, Code Reviewer resolves or reopens them
- Team Lead records non-blocking review debt explicitly via `decide-risk --status <ACCEPTED|DEFERRED>` and `decide-finding --status <ACCEPTED|DEFERRED>` instead of leaving Medium/Low items open by implication
- TL uses `code-review-gate` to determine readiness: `PASS`, `FAIL`, or `ESCALATE` (3 unresolved rounds)
- Invoke agents with feature name; agents query `flow-log summary` to load context
- TL passes one active slice ID and the state-backed slice-run type when invoking `Java Coder` or slice-scoped review work
- Default one story to one sequential slice-run loop; if more than one slice-run is needed, TL records a one-line justification before coding starts
- Do not advance on chat claims; validate artifacts on disk and flow-log state
- `Architecture Reviewer` and `Code Reviewer` are the technical gates; `Team Lead` performs audit-style spot checks
- `Java Coder` is limited to 1 slice per invocation
- `E2E Tester` owns the approved E2E scenario artifact and the `karate` check only when `e2eMode = SCENARIOS_REQUIRED`
- When `e2eMode = REUSE_EXISTING`, Team Lead runs `karate` directly against the existing smoke suite and no E2E artifact is required
- TL uses `plan-summary` and `plan-get --slice` to reject oversized slice-runs early instead of asking Coder to absorb multi-boundary work in one pass
- Repeated coder false positives trigger architect plan revision instead of endless retries

## Main Artifacts

- Flow-log state: `artifacts/flow-logs/<feature-name>.json`
- Story: `artifacts/user-stories/<feature-name>.story.md`
- E2E scenarios: `artifacts/e2e-scenarios/<feature-name>.e2e.md`
- Plan: `artifacts/implementation-plans/<feature-name>.plan.json`

## Verification Expectations

- `Java Coder` runs `verify --profile slice` for every slice-run (`verifyQuick` → `finalCheck`). `coder-handoff-check.sh` remains the pre-handoff gate.
- `E2E Tester` runs `run-check --name karate` after the final coder slice-run and after any later code change that makes `karateStale` true when `e2eMode = SCENARIOS_REQUIRED`.
- `Team Lead` runs `run-check --name karate` after the final coder slice-run and after any later code change that makes `karateStale` true when `e2eMode = REUSE_EXISTING`.
- When `run-check` or `verify` fails, the owning agent inspects the persisted redacted log via `scripts/flow-log.sh check-log --feature <feature-name> --name <check> --lines 80` before rerunning blindly or routing the issue onward.
- `Team Lead` checks `storyStale`, `planStale`, the recorded `e2eMode`, and `*Stale` fields in `flow-log status`. When `e2eMode = SCENARIOS_REQUIRED`, TL also checks `e2eStale`. If `finalCheckStale` or `karateStale` is true, re-runs only the stale check. If only `verifyQuickStale` is true but `finalCheck` is PASS and not stale, staleness is non-blocking. Fresh coder evidence is trusted by default instead of triggering automatic TL reruns.
- `Code Reviewer` validates code quality, reviews flow-log evidence including staleness flags, and reruns checks only when evidence is suspect or stale
- `readiness signoff` blocks if non-blocking architecture risks or code findings remain undecided; carried debt must be recorded as `ACCEPTED` or `DEFERRED`
- ArchUnit tests run as part of `mvn test` (verify-quick) — architecture boundary violations break the build deterministically without requiring manual review

## Source Files

- [Team Lead Agent](../agents/team-lead.agent.md)
- [Product Manager Agent](../agents/product-manager.agent.md)
- [E2E Tester Agent](../agents/e2e-tester.agent.md)
- [Java Architect Agent](../agents/java-architect.agent.md)
- [Architecture Reviewer Agent](../agents/architecture-reviewer.agent.md)
- [Code Reviewer Agent](../agents/reviewer.agent.md)
- [Java Coder Agent](../agents/java-coder.agent.md)
