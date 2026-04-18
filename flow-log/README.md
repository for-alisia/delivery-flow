# Flow Log

Minimal workflow state CLI for feature delivery.

One machine-owned JSON state file per feature. Tracks artifacts, reviews, checks, risks, findings, batches, and timing.

## State File

`artifacts/flow-logs/<feature-name>.json` (override with `--state-path`)

## Quickstart

### 1) Create and Lock

```bash
node flow-log/flow-log.mjs create --feature my-feature
node flow-log/flow-log.mjs lock-requirements --feature my-feature --by TL --request-source <path>
```

### 2) Story

```bash
node flow-log/flow-log.mjs register-artifact story --feature my-feature --path artifacts/user-stories/my-feature.story.md
node flow-log/flow-log.mjs approve-artifact story --feature my-feature --by TL
```

### 3) Plan

Uses v3 draft lifecycle. Full details: [docs/plan-management.md](docs/plan-management.md)

```bash
node flow-log/flow-log.mjs init-plan --feature my-feature
node flow-log/flow-log.mjs plan-create-draft --feature my-feature
# edit draft at /tmp/flow-log-plan-drafts/<feature>.draft.json
node flow-log/flow-log.mjs plan-validate-draft --feature my-feature
node flow-log/flow-log.mjs plan-accept-draft --feature my-feature
node flow-log/flow-log.mjs register-artifact plan --feature my-feature --path artifacts/implementation-plans/my-feature.plan.json
node flow-log/flow-log.mjs approve-artifact plan --feature my-feature --by TL
```

### 4) Reviews, Checks, Queries

```bash
node flow-log/flow-log.mjs set-review --feature my-feature --name architectureReview --status PASS --by Reviewer
node flow-log/flow-log.mjs set-check --feature my-feature --name finalCheck --status PASS --by TL --command scripts/final-check.sh
node flow-log/flow-log.mjs summary --feature my-feature
node flow-log/flow-log.mjs status --feature my-feature
node flow-log/flow-log.mjs readiness signoff --feature my-feature
```

### 5) Events and History

```bash
node flow-log/flow-log.mjs add-event --feature my-feature --type redCard --by TL --target JavaCoder --reason "final-check failed"
node flow-log/flow-log.mjs history --feature my-feature --limit 10
```

### 6) Batches

```bash
node flow-log/flow-log.mjs start-batch --feature my-feature --slice "slice-1" --slice "slice-2" --by TL
node flow-log/flow-log.mjs complete-batch --feature my-feature --status complete
node flow-log/flow-log.mjs reset-checks --feature my-feature --reason "test failure" --by TL --target JavaCoder
node flow-log/flow-log.mjs complete --feature my-feature
```

### 7) Architecture Risks and Code Findings

Full details: [docs/review-commands.md](docs/review-commands.md)

```bash
# risks (architecture review loop)
node flow-log/flow-log.mjs add-risk --feature my-feature --description "..." --suggested-fix "..." --by ArchitectureReviewer
node flow-log/flow-log.mjs reclassify-risk --feature my-feature --id 1 --severity HIGH --reason "..." --by TL
node flow-log/flow-log.mjs respond-risk --feature my-feature --id 1 --status ADDRESSED --note "..." --by JavaArchitect
node flow-log/flow-log.mjs resolve-risk --feature my-feature --id 1 --by ArchitectureReviewer
node flow-log/flow-log.mjs increment-round --feature my-feature
node flow-log/flow-log.mjs architecture-gate --feature my-feature

# findings (code review loop)
node flow-log/flow-log.mjs add-finding --feature my-feature --severity HIGH --description "..." --file "..." --by CodeReviewer
node flow-log/flow-log.mjs respond-finding --feature my-feature --id 1 --status FIXED --note "..." --by JavaCoder
node flow-log/flow-log.mjs resolve-finding --feature my-feature --id 1 --by CodeReviewer
node flow-log/flow-log.mjs increment-code-review-round --feature my-feature
node flow-log/flow-log.mjs code-review-gate --feature my-feature
```

## Sign-off Readiness

`readiness signoff` returns `ready: true` when: requirements locked, story registered+approved, plan registered+approved, `architectureReview` PASS, `codeReview` PASS, `finalCheck` PASS, `karate` PASS.

## Detailed Documentation

- [Plan Management (v3)](docs/plan-management.md) — draft lifecycle, schema, validation, revision workflow
- [Review Commands](docs/review-commands.md) — architectural risks, code findings, gates, escalation

## Notes

- Output is JSON only.
- `verifyQuick` is recorded but not part of sign-off readiness.
- `set-review` accepts legacy aliases `phase1` → `architectureReview`, `phase2` → `codeReview`.
- `reset-checks` automatically records a `redCard` event.
- `status` = short check; `summary` = medium detail; `history` = event trail.
- `create` records `timing.startedAt`; `complete` records `timing.completedAt` and `durationMinutes`.
