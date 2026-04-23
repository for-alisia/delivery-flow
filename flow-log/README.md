# Flow Log

Minimal workflow state CLI for feature delivery.

One machine-owned JSON state file per feature tracks artifacts, reviews, checks, risks, findings, batches, and timing.

## State File

`artifacts/flow-logs/<feature-name>.json` (override with `--state-path`)

## Quickstart

### 1) Create and lock

```bash
scripts/flow-log.sh create --feature my-feature
scripts/flow-log.sh lock-requirements --feature my-feature --by TL --request-source <path>
```

### 2) Story

```bash
scripts/flow-log.sh register-artifact story --feature my-feature --path artifacts/user-stories/my-feature.story.md
scripts/flow-log.sh approve-artifact story --feature my-feature --by TL
scripts/flow-log.sh story-get --feature my-feature --section external-contracts
```

`story-get` reads one section from the approved story artifact. Keep compact concrete request / response / error examples in that section when field names, nullability, omitted-body behavior, or error payload shape are easy to misread. If the story changed after approval, the command fails until Team Lead re-approves it.

### 3) Plan (v4)

The canonical plan artifact is `artifacts/implementation-plans/<feature>.plan.json`.
Use [artifacts/templates/plan-v4.example.json](../artifacts/templates/plan-v4.example.json) as the field-level reference example.

```bash
scripts/flow-log.sh plan-init-draft --feature my-feature
# edit /tmp/flow-log-plan-drafts/<feature>.draft.json
scripts/flow-log.sh plan-validate-draft --feature my-feature
scripts/flow-log.sh plan-accept-draft --feature my-feature
scripts/flow-log.sh register-artifact plan --feature my-feature --path artifacts/implementation-plans/my-feature.plan.json
scripts/flow-log.sh approve-artifact plan --feature my-feature --by TL
```

Read plans surgically whenever possible:

```bash
scripts/flow-log.sh plan-summary --feature my-feature
scripts/flow-log.sh plan-get --feature my-feature --slice S1
scripts/flow-log.sh plan-get --feature my-feature --section sharedDecisions
```

### 4) Batches, reviews, and checks

```bash
scripts/flow-log.sh start-batch --feature my-feature --slice S1 --slice S2 --by TL
scripts/flow-log.sh summary --feature my-feature
scripts/flow-log.sh verify --feature my-feature --profile batch --by JavaCoder
scripts/flow-log.sh verify --feature my-feature --profile full --by JavaCoder
scripts/flow-log.sh complete-batch --feature my-feature --status complete
```

`start-batch` requires at least one approved slice ID from the registered v4 plan. The `summary` output includes the current batch number, active slice IDs, and batch-owned changed files so downstream agents can stay slice-scoped.

### 5) Architecture risks and code findings

```bash
scripts/flow-log.sh add-risk \
  --feature my-feature \
  --description "Wrong slice ownership" \
  --plan-ref S1-U2 \
  --suggested-fix "Move the unit to the correct slice" \
  --by ArchitectureReviewer

scripts/flow-log.sh add-finding \
  --feature my-feature \
  --severity HIGH \
  --description "Null check missing on mapper input" \
  --file src/main/java/Mapper.java \
  --by CodeReviewer
```

`--plan-ref` is required for every architecture risk. Use slice IDs (`S1`), unit IDs (`S1-U2`), or shared rule / decision IDs (`R1`, `D1`).

## Notes

- Output is JSON only.
- `verifyQuick` is recorded but not part of sign-off readiness.
- Sign-off blocks stale story or plan approvals and stale `finalCheck` / `karate` evidence.
- Run commands from the repository root directory. The canonical wrapper is `scripts/flow-log.sh`.
- `status` is the compact check, `summary` is the medium-detail handoff view, and `history` is the event trail.

## Detailed Docs

- [docs/plan-management.md](docs/plan-management.md)
- [docs/review-commands.md](docs/review-commands.md)
