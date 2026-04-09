# Flow Log

Minimal workflow state CLI for feature delivery.

This tool keeps one machine-owned JSON state file per feature and leaves human-readable
artifacts such as story and plan as separate Markdown files.

## State File

By default the CLI stores state here:

`artifacts/flow-logs/<feature-name>.json`

You can override that with `--state-path`.

## Goals

- keep workflow bookkeeping out of agent-written JSON reports
- record only delivery facts and approvals
- answer readiness questions deterministically
- stay flow-agnostic: story, plan, reviews, checks, readiness

## Current Commands

Create a log:

```bash
node flow-log/flow-log.mjs create --feature label-events-api
```

Lock requirements:

```bash
node flow-log/flow-log.mjs lock-requirements --feature label-events-api --by TL
```

Register and approve story/plan artifacts:

```bash
node flow-log/flow-log.mjs register-artifact story \
  --feature label-events-api \
  --path artifacts/user-stories/label-events-api.story.md

node flow-log/flow-log.mjs approve-artifact story \
  --feature label-events-api \
  --by TL

node flow-log/flow-log.mjs register-artifact plan \
  --feature label-events-api \
  --path artifacts/implementation-plans/label-events-api.plan.md

node flow-log/flow-log.mjs approve-artifact plan \
  --feature label-events-api \
  --by TL
```

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

### Plan Structure (v2.0)

The plan is a single JSON file at `artifacts/implementation-plans/<feature>.plan.json`.
It replaces the Markdown implementation plan entirely — **there is no separate prose plan**.
The Architect populates the plan using CLI write commands. All agents can read it.

**Access control:**
- **Architect** — full write access (all `add-plan-*`, `set-plan-*`, `init-plan`, `revise-plan`)
- **All agents** — read access (`plan-get`, `plan-summary`, `validate-plan`)

Schema version: `2.0`. Sections: `payloadExamples`, `validationBoundary`, `models`, `classes`, `compositionStrategy`, `sharedInfra`, `slices`, `testingMatrix`, `karate`, `archUnit`.

#### Initialize

```bash
node flow-log/flow-log.mjs init-plan --feature my-feature
node flow-log/flow-log.mjs init-plan --feature my-feature --force   # overwrite existing
```

#### Payload Examples

```bash
node flow-log/flow-log.mjs add-plan-example \
  --feature my-feature \
  --label "Search with audit" \
  --type request \
  --body '{"pagination":{"page":1},"filters":{"audit":["label"]}}'
```

Types: `request`, `success`, `error`, `validation-error`. Body is parsed JSON.

#### Validation Boundary

```bash
node flow-log/flow-log.mjs add-plan-validation \
  --feature my-feature \
  --rule "perPage <= 40" \
  --boundary "IssuesService" \
  --reason "Existing runtime guard"
```

#### Models

```bash
# Record with inline fields
node flow-log/flow-log.mjs add-plan-model \
  --feature my-feature \
  --qualified-name com.example.orchestration.model.Issue \
  --type record \
  --status modified \
  --justification "Orchestration entity per constitution Principle 2" \
  --annotations "@Builder" \
  --fields '[{"name":"id","type":"long"},{"name":"labels","type":"List<String>","nullable":false,"defensiveCopy":true}]' \
  --notes "Defensive copy on labels"

# Enum model
node flow-log/flow-log.mjs add-plan-model \
  --feature my-feature \
  --qualified-name com.example.orchestration.model.AuditType \
  --type enum \
  --status new \
  --justification "Domain concept in orchestration" \
  --values "LABEL" \
  --methods "String value(),static AuditType fromValue(String raw)"

# Add field to existing model incrementally
node flow-log/flow-log.mjs add-plan-model-field \
  --feature my-feature \
  --model com.example.orchestration.model.Issue \
  --name changeSets \
  --type "List<ChangeSet>" \
  --nullable \
  --defensive-copy
```

Model types: `record`, `enum`, `interface`, `sealed-interface`. Status: `new`, `modified`.
Each model must have a `justification` defending its package placement per constitution rules.
Adding a model with the same `--qualified-name` replaces the existing entry.

#### Classes

```bash
node flow-log/flow-log.mjs add-plan-class \
  --feature my-feature \
  --path "src/main/java/com/example/IssuesService.java" \
  --status modified \
  --role "Search with audit"
```

Statuses: `new`, `modified`, `existing`. Adding the same `--path` updates in place.

#### Slices

```bash
# Full slice with inline tests and logging
node flow-log/flow-log.mjs add-plan-slice \
  --feature my-feature \
  --id 1 \
  --title "Orchestration models" \
  --goal "Add SearchIssuesInput and wire service" \
  --files "SearchIssuesInput.java,IssuesService.java" \
  --unit-test "IssuesServiceTest: search with audit" \
  --unit-test "IssuesServiceTest: search without audit" \
  --component-test "IssuesApiComponentTest: audit returns changeSets" \
  --info-log "IssuesService logs audit types" \
  --error-log "None"

# Add test incrementally
node flow-log/flow-log.mjs add-plan-slice-test \
  --feature my-feature --slice 1 --level unit \
  --test "IssuesServiceTest: failure propagation"

# Update logging
node flow-log/flow-log.mjs set-plan-slice-logging \
  --feature my-feature --slice 1 \
  --error "IssuesService logs enrichment failure"
```

Test levels: `unit`, `integration`, `component`. Repeated `--unit-test`, `--integration-test`, `--component-test` flags supported.

#### Composition Strategy

```bash
node flow-log/flow-log.mjs set-plan-composition \
  --feature my-feature \
  --approach "dependent-then-parallel" \
  --description "Search first, then parallel enrichment"
```

#### Shared Infrastructure

```bash
node flow-log/flow-log.mjs set-plan-infra \
  --feature my-feature \
  --reused "AsyncComposer,GitLabExceptionMapper"
```

#### Testing Matrix

```bash
node flow-log/flow-log.mjs add-plan-test \
  --feature my-feature \
  --level Unit --required --coverage "IssuesServiceTest"

node flow-log/flow-log.mjs add-plan-test \
  --feature my-feature \
  --level Component --required --coverage "IssuesApiComponentTest"
```

#### Karate

```bash
node flow-log/flow-log.mjs set-plan-karate \
  --feature my-feature \
  --feature-file "src/test/karate/resources/issues/search-audit.feature" \
  --scenario "Search with label audit" \
  --scenario "Search without audit" \
  --smoke-tagged
```

#### ArchUnit

```bash
node flow-log/flow-log.mjs set-plan-archunit \
  --feature my-feature \
  --existing-reviewed
```

#### Revision and Validation

```bash
# Bump revision (clears ALL sections for re-population)
node flow-log/flow-log.mjs revise-plan --feature my-feature

# Validate completeness
node flow-log/flow-log.mjs validate-plan --feature my-feature
```

Validation checks: models exist with justification, records have fields, classes registered, slices registered, Java-file slices have tests.

#### Read Commands (all agents)

```bash
# Full plan
node flow-log/flow-log.mjs plan-get --feature my-feature

# Specific section
node flow-log/flow-log.mjs plan-get --feature my-feature --section models
node flow-log/flow-log.mjs plan-get --feature my-feature --section slices

# Compact summary
node flow-log/flow-log.mjs plan-summary --feature my-feature
```

Available sections: `payloadExamples`, `validationBoundary`, `models`, `classes`, `compositionStrategy`, `sharedInfra`, `slices`, `testingMatrix`, `karate`, `archUnit`.
- `get` returns the full raw state when detailed inspection is needed.
- Risk lifecycle: `OPEN` → `ADDRESSED`/`INVALIDATED` (by Architect) → `RESOLVED` (by Reviewer) or `REOPENED` (by Reviewer). Only Reviewer can resolve or reopen.
- `architecture-gate` returns `PASS` when no unresolved Critical/High risks remain, `FAIL` when they exist, `ESCALATE` after 5 rounds with unresolved Critical/High. On `ESCALATE`, TL uses `add-event --type archEscalationDecision --decision <PROCEED_TO_CODING|FINAL_ADJUSTMENT|ESCALATE_TO_USER>` to log the decision.
- `summary` includes full `architecturalRisks` section with per-risk detail, severity/status counts, and round number.
- Finding lifecycle: `OPEN` → `FIXED`/`DISPUTED` (by Coder) → `RESOLVED` (by Code Reviewer) or `REOPENED` (by Code Reviewer). Only Code Reviewer can resolve or reopen.
- `code-review-gate` returns `PASS` when no unresolved Critical/High findings remain, `FAIL` when they exist, `ESCALATE` after 3 rounds with unresolved Critical/High.
- `summary` includes full `codeFindings` section with per-finding detail (including file path), severity/status counts, and round number.
