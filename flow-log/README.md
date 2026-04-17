# Flow Log

Minimal workflow state CLI for feature delivery.

This tool keeps one machine-owned JSON state file per feature and tracks external
workflow artifacts by path (for example story Markdown and canonical plan JSON).

## State File

By default the CLI stores state here:

`artifacts/flow-logs/<feature-name>.json`

You can override that with `--state-path`.

## Goals

- keep workflow bookkeeping out of agent-written JSON reports
- record only delivery facts and approvals
- answer readiness questions deterministically
- stay flow-agnostic: story, plan, reviews, checks, readiness

## Workflow Quickstart

### 1) Create Flow State

```bash
node flow-log/flow-log.mjs create --feature label-events-api
node flow-log/flow-log.mjs lock-requirements --feature label-events-api --by TL
```

### 2) Story Artifact (Creation + Validation)

Story content is authored outside `flow-log` (for example `artifacts/user-stories/<feature>.story.md`).
`flow-log` validates story existence during registration/approval.

```bash
node flow-log/flow-log.mjs register-artifact story \
  --feature label-events-api \
  --path artifacts/user-stories/label-events-api.story.md

node flow-log/flow-log.mjs approve-artifact story \
  --feature label-events-api \
  --by TL
```

### 3) Implementation Plan Artifact (Creation + Validation)

Canonical artifact: `artifacts/implementation-plans/<feature>.plan.json` (schema `3.0`).

Create and validate plan through the v3 draft lifecycle:

```bash
# create canonical v3 skeleton
node flow-log/flow-log.mjs init-plan --feature label-events-api

# create/resume tool-managed draft
node flow-log/flow-log.mjs plan-create-draft --feature label-events-api

# validate draft (shape + readiness + risk link stability)
node flow-log/flow-log.mjs plan-validate-draft --feature label-events-api

# accept validated draft into canonical plan
node flow-log/flow-log.mjs plan-accept-draft --feature label-events-api
```

Register and approve canonical plan artifact in flow-log:

```bash
node flow-log/flow-log.mjs register-artifact plan \
  --feature label-events-api \
  --path artifacts/implementation-plans/label-events-api.plan.json

node flow-log/flow-log.mjs approve-artifact plan \
  --feature label-events-api \
  --by TL
```

Important: when `plan-accept-draft` changes canonical content, flow-log invalidates prior plan approval metadata (`approved`, `approvedRevision`, `approvedHash`) until TL approves again.

## Additional Commands

Record reviews:

```bash
node flow-log/flow-log.mjs set-review \
  --feature label-events-api \
  --name architectureReview \
  --status PASS \
  --by Reviewer
```

Record checks:

```bash
node flow-log/flow-log.mjs set-check \
  --feature label-events-api \
  --name finalCheck \
  --status PASS \
  --by TL \
  --command scripts/final-check.sh \
  --details "BUILD SUCCESS"
```

Query summary:

```bash
node flow-log/flow-log.mjs summary --feature label-events-api
```

Query short status:

```bash
node flow-log/flow-log.mjs status --feature label-events-api
```

Record a rejection, red card, reroute, or note:

```bash
node flow-log/flow-log.mjs add-event \
  --feature label-events-api \
  --type redCard \
  --by TL \
  --target JavaCoder \
  --related-check finalCheck \
  --reason "Coder claimed ready but final-check failed on component tests"
```

Inspect event history:

```bash
node flow-log/flow-log.mjs history --feature label-events-api --limit 10
```

Check sign-off readiness:

```bash
node flow-log/flow-log.mjs readiness signoff --feature label-events-api
```

Track coder batches:

```bash
node flow-log/flow-log.mjs start-batch \
  --feature label-events-api \
  --slice "slice-1" \
  --slice "slice-2" \
  --by TL

node flow-log/flow-log.mjs complete-batch \
  --feature label-events-api \
  --status complete
```

Reset checks on red card (clears all checks to NOT_RUN and records a redCard event):

```bash
node flow-log/flow-log.mjs reset-checks \
  --feature label-events-api \
  --reason "Component test failure found by TL" \
  --by TL \
  --target JavaCoder
```

Complete the flow (records end time and calculates duration):

```bash
node flow-log/flow-log.mjs complete --feature label-events-api
```

### Architectural Risks

Record an architectural risk finding:

```bash
node flow-log/flow-log.mjs add-risk \
  --feature label-events-api \
  --severity HIGH \
  --description "Wrong composition strategy — data dependency requires sequential" \
  --by ArchitectureReviewer
```

Architect responds to a risk (ADDRESSED or INVALIDATED with a note):

```bash
node flow-log/flow-log.mjs respond-risk \
  --feature label-events-api \
  --id 1 \
  --status ADDRESSED \
  --note "Switched to sequential with justification in plan section 6" \
  --by JavaArchitect
```

Reviewer resolves a risk (accepts Architect's response):

```bash
node flow-log/flow-log.mjs resolve-risk \
  --feature label-events-api \
  --id 1 \
  --by ArchitectureReviewer
```

Reviewer reopens a risk (not satisfied with response):

```bash
node flow-log/flow-log.mjs reopen-risk \
  --feature label-events-api \
  --id 1 \
  --reason "Sequential still wrong — bidirectional dependency" \
  --by ArchitectureReviewer
```

Increment the review round (TL does this before each review):

```bash
node flow-log/flow-log.mjs increment-round --feature label-events-api
```

Check the architecture gate (computed from risk state):

```bash
node flow-log/flow-log.mjs architecture-gate --feature label-events-api
# Returns: PASS (no unresolved Critical/High), FAIL (unresolved remain), or ESCALATE (5+ rounds — TL decides)
```

When `ESCALATE` is returned, the gate includes `unresolvedRisks` (array of id/severity/description/status). TL must log its decision:

```bash
node flow-log/flow-log.mjs add-event \
  --feature label-events-api \
  --type archEscalationDecision \
  --decision PROCEED_TO_CODING \
  --reason "Risk #1 is artifact naming only, not a correctness blocker" \
  --by TL
```

Decisions: `PROCEED_TO_CODING` (non-blocking findings), `FINAL_ADJUSTMENT` (targeted fix then skip review), `ESCALATE_TO_USER` (real blocker).

### Code Findings

Record a code review finding:

```bash
node flow-log/flow-log.mjs add-finding \
  --feature label-events-api \
  --severity HIGH \
  --description "Null check missing on mapper input" \
  --file "src/main/java/Mapper.java" \
  --by CodeReviewer
```

Coder responds to a finding (FIXED or DISPUTED with a note):

```bash
node flow-log/flow-log.mjs respond-finding \
  --feature label-events-api \
  --id 1 \
  --status FIXED \
  --note "Added Objects.requireNonNull with descriptive message" \
  --by JavaCoder
```

Reviewer resolves a finding (accepts Coder's response):

```bash
node flow-log/flow-log.mjs resolve-finding \
  --feature label-events-api \
  --id 1 \
  --by CodeReviewer
```

Reviewer reopens a finding (fix is wrong or incomplete):

```bash
node flow-log/flow-log.mjs reopen-finding \
  --feature label-events-api \
  --id 1 \
  --reason "Only fixed one of three call sites" \
  --by CodeReviewer
```

Increment the code review round (TL does this before each review):

```bash
node flow-log/flow-log.mjs increment-code-review-round --feature label-events-api
```

Check the code review gate (computed from finding state):

```bash
node flow-log/flow-log.mjs code-review-gate --feature label-events-api
# Returns: PASS (no unresolved Critical/High), FAIL (unresolved remain), or ESCALATE (3+ rounds)
```

## Sign-off Readiness Rules

`readiness signoff` returns `ready: true` only when all of these are true:

- requirements are locked
- story path is registered, exists, and is approved
- plan path is registered, exists, and is approved
- `architectureReview` is `PASS`
- `codeReview` is `PASS`
- `finalCheck` is `PASS`
- `karate` is `PASS`

## Notes

- The tool stores paths to story/plan instead of copying their content.
- `verifyQuick` is recorded but is not part of sign-off readiness.
- Output is JSON only, which makes it easier for agents or scripts to consume.
- `set-review` uses named review gates instead of numbered phases. Legacy aliases `phase1` -> `architectureReview` and `phase2` -> `codeReview` are still accepted.
- Rejections and red cards are tracked as events, not as separate artifact files.
- `reset-checks` automatically records a `redCard` event — use it when TL returns work to Coder.
- `start-batch` / `complete-batch` track coder batch lifecycle; `status` and `summary` show current batch.
- `create` records `timing.startedAt`; `complete` records `timing.completedAt` and calculates `durationMinutes`.
- `status` is the short default command for frequent TL checks.
- `summary` is a medium-detail snapshot.
- `history` is the explicit retry and red-card trail.

### Plan Structure (v3.0)

The canonical plan artifact is:

`artifacts/implementation-plans/<feature>.plan.json`

The tool now uses a draft lifecycle. Architect edits a tool-managed draft, then `flow-log` validates and accepts it into canonical JSON.

#### Draft Lifecycle (recommended)

```bash
# create canonical v3 skeleton
node flow-log/flow-log.mjs init-plan --feature my-feature

# create or resume tool-managed draft
node flow-log/flow-log.mjs plan-create-draft --feature my-feature

# validate draft (shape + readiness + enforced risk link stability)
node flow-log/flow-log.mjs plan-validate-draft --feature my-feature

# accept validated draft into canonical plan
node flow-log/flow-log.mjs plan-accept-draft --feature my-feature

# inspect or discard draft
node flow-log/flow-log.mjs plan-draft-status --feature my-feature
node flow-log/flow-log.mjs plan-discard-draft --feature my-feature
```

Draft path:

`/tmp/flow-log-plan-drafts/<feature>.draft.json`

`plan-accept-draft` behavior:
- validates before write
- exits non-zero if validation fails
- returns `changed: false` on no-op
- bumps canonical `revision` and recomputes `hash` on change
- invalidates flow-log plan approval metadata (`approved`, `approvedRevision`, `approvedHash`) when canonical content changes

`plan-create-draft` behavior:
- creates a new draft from canonical when no draft exists
- returns the existing draft when it already matches canonical
- refuses to silently reuse a draft that has unapplied changes; inspect it with `plan-draft-status` or reset it with `plan-discard-draft`

#### Read/Validate Commands (all agents)

```bash
node flow-log/flow-log.mjs validate-plan --feature my-feature
node flow-log/flow-log.mjs plan-summary --feature my-feature
node flow-log/flow-log.mjs plan-get --feature my-feature
node flow-log/flow-log.mjs plan-get --feature my-feature --section slices
```

v3 sections:
- `scope`
- `implementationFlow`
- `contractExamples`
- `validationRules`
- `designDecisions`
- `models`
- `classes`
- `slices`
- `verification`

#### Reviewer Risk References

`add-risk` now supports plan-aware references:

```bash
node flow-log/flow-log.mjs add-risk \
  --feature my-feature \
  --severity HIGH \
  --description "Flow step ownership is unclear" \
  --plan-ref F3 \
  --connected-area S2 \
  --connected-area M4 \
  --suggested-fix "Split flow step and rebind slice ownership"
```

Fields:
- `planRefs`: primary directly affected plan IDs
- `connectedAreas`: secondary related plan IDs
- `suggestedFix`: reviewer-proposed remediation

Risk link enforcement during validation and acceptance applies to `OPEN`, `REOPENED`, and `ADDRESSED` risks.

#### Legacy v2 Commands (transition only)

Legacy `add-plan-*`, `set-plan-*`, and `revise-plan` commands are still available for existing `schemaVersion: "2.0"` plans.
They are marked as legacy in CLI help and should not be used for new plans.
