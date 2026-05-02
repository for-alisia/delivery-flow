# Flow Log

Minimal workflow state CLI for feature delivery.

One machine-owned JSON state file per feature tracks artifacts, reviews, checks, risks, findings, slice-runs, and timing.

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

### 3) E2E scenarios

The approved E2E scenario artifact lives at `artifacts/e2e-scenarios/<feature>.e2e.md`.
Use [artifacts/templates/e2e-scenarios.example.md](../artifacts/templates/e2e-scenarios.example.md) as the structure reference.

Choose the smoke path first:

```bash
scripts/flow-log.sh set-e2e-mode --feature my-feature --mode REUSE_EXISTING --by TL --reason "Internal change; existing Karate coverage is sufficient"
```

or

```bash
scripts/flow-log.sh set-e2e-mode --feature my-feature --mode SCENARIOS_REQUIRED --by TL --reason "API contract and smoke coverage change"
```

- `REUSE_EXISTING`: skip the E2E artifact and let Team Lead run the existing Karate suite after coding.
- `SCENARIOS_REQUIRED`: create and approve the E2E scenario artifact, then route smoke work through `E2E Tester`.

```bash
scripts/flow-log.sh register-artifact e2e --feature my-feature --path artifacts/e2e-scenarios/my-feature.e2e.md
scripts/flow-log.sh approve-artifact e2e --feature my-feature --by TL
```

`summary` and `status` expose the E2E decision and artifact state (`e2e.mode`, `e2eApproved`, `e2eStale`).

### 4) Plan (v4)

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

### 5) Slice-runs, reviews, and checks

```bash
scripts/flow-log.sh start-slice-run --feature my-feature --slice S1 --type intermediate --by TL
scripts/flow-log.sh summary --feature my-feature
scripts/flow-log.sh verify --feature my-feature --profile slice --by JavaCoder
scripts/flow-log.sh complete-slice-run --feature my-feature --status complete
scripts/flow-log.sh start-slice-run --feature my-feature --slice S2 --type final --by TL
scripts/flow-log.sh verify --feature my-feature --profile slice --by JavaCoder
scripts/flow-log.sh complete-slice-run --feature my-feature --status complete
scripts/flow-log.sh run-check --feature my-feature --name karate --by TL
scripts/flow-log.sh check-log --feature my-feature --name finalCheck --lines 40
```

`start-slice-run` requires one approved slice ID from the registered v4 plan and an explicit handoff type (`intermediate` or `final`). The `summary` output includes the current slice-run number, handoff type, active slice ID, and slice-run-owned changed files so downstream agents can stay narrowly scoped.

`verify --profile slice` is the standard coder gate. In the active workflow, `Java Coder` owns `verifyQuick` and `finalCheck`.

- When `e2eMode = SCENARIOS_REQUIRED`, `E2E Tester` owns `karate` via `run-check --name karate`.
- When `e2eMode = REUSE_EXISTING`, Team Lead runs `karate` directly via `run-check --name karate` using the existing smoke suite.

`verify --profile full` remains available for exceptional combined reruns.

`run-check` and `verify` now persist a redacted per-check log under `artifacts/check-logs/<feature>/...`. Use `check-log` to fetch the latest stored log tail for a check without reopening the whole state file.

### 6) Architecture risks and code findings

```bash
scripts/flow-log.sh add-risk \
  --feature my-feature \
  --description "Wrong slice ownership" \
  --plan-ref S1-U2 \
  --suggested-fix "Move the unit to the correct slice" \
  --by ArchitectureReviewer

scripts/flow-log.sh decide-risk \
  --feature my-feature \
  --id 1 \
  --status ACCEPTED \
  --reason "Known non-blocking architecture debt for this release" \
  --by TL

scripts/flow-log.sh add-finding \
  --feature my-feature \
  --severity HIGH \
  --description "Null check missing on mapper input" \
  --file src/main/java/Mapper.java \
  --by CodeReviewer

scripts/flow-log.sh decide-finding \
  --feature my-feature \
  --id 2 \
  --status DEFERRED \
  --reason "Track in cleanup story after release" \
  --follow-up cleanup-story \
  --by TL
```

`--plan-ref` is required for every architecture risk. Use slice IDs (`S1`), unit IDs (`S1-U2`), or shared rule / decision IDs (`R1`, `D1`).

Use `decide-risk` and `decide-finding` only for `MEDIUM` or `LOW` items that TL is consciously carrying forward. `ACCEPTED` means the debt is known and allowed in the current delivery. `DEFERRED` means it is known and should be revisited later, optionally with a follow-up reference.

## Notes

- Output is JSON only.
- `verifyQuick` is recorded but not part of sign-off readiness.
- Sign-off blocks stale story or plan approvals and stale `finalCheck` / `karate` evidence. It also blocks stale E2E scenario approvals when `e2eMode = SCENARIOS_REQUIRED`.
- Sign-off also blocks non-blocking architecture risks or code findings that are still undecided. Mark them `ACCEPTED` or `DEFERRED` before final sign-off.
- Run commands from the repository root directory. The canonical wrapper is `scripts/flow-log.sh`.
- `status` is the compact check, `summary` is the medium-detail handoff view, and `history` is the event trail.

## Detailed Docs

- [docs/plan-management.md](docs/plan-management.md)
- [docs/review-commands.md](docs/review-commands.md)
