# Review Commands

Flow-log tracks architecture risks and code findings as durable review state.

## Architecture risks

Used during the architecture review loop between Architecture Reviewer and Java Architect.

### Record a risk

```bash
scripts/flow-log.sh add-risk \
  --feature my-feature \
  --description "Wrong slice ownership — coder will modify the wrong artifact." \
  --plan-ref S1-U2 \
  --connected-area S1 \
  --suggested-fix "Move the unit into the correct slice and preserve the stable unit id." \
  --by ArchitectureReviewer
```

Required:

- `--description`
- `--plan-ref`

Optional:

- `--severity` (TL or known-severity replay only)
- `--connected-area`
- `--suggested-fix`
- `--by`

Use stable `plan-ref` values:

- slice: `S1`
- unit: `S1-U2`
- shared rule / decision: `R1`, `D1`

### Architect responds

```bash
scripts/flow-log.sh respond-risk \
  --feature my-feature \
  --id 1 \
  --status ADDRESSED \
  --note "Moved the unit into S1 and preserved the unit id." \
  --by JavaArchitect
```

### Reviewer adjudicates

```bash
scripts/flow-log.sh resolve-risk --feature my-feature --id 1 --by ArchitectureReviewer
scripts/flow-log.sh reopen-risk --feature my-feature --id 1 --reason "Unit id changed without reason" --by ArchitectureReviewer
```

### Team Lead records non-blocking debt

Use this only for `MEDIUM` or `LOW` risks that are consciously being carried forward.

```bash
scripts/flow-log.sh decide-risk \
  --feature my-feature \
  --id 2 \
  --status ACCEPTED \
  --reason "Valid concern, but not worth another architecture loop for this delivery." \
  --by TL

scripts/flow-log.sh decide-risk \
  --feature my-feature \
  --id 3 \
  --status DEFERRED \
  --reason "Track in follow-up cleanup story after release." \
  --follow-up cleanup-story \
  --by TL
```

`ACCEPTED` means TL reviewed the risk and is consciously carrying it in the current delivery.
`DEFERRED` means TL reviewed the risk and is carrying it with an explicit later follow-up.
Signoff blocks when non-blocking risks remain undecided.

### Round management

```bash
scripts/flow-log.sh increment-round --feature my-feature
scripts/flow-log.sh architecture-gate --feature my-feature
```

## Code findings

Used during the code review loop between Code Reviewer and Java Coder.

### Record a finding

```bash
scripts/flow-log.sh add-finding \
  --feature my-feature \
  --severity HIGH \
  --description "Null check missing on mapper input" \
  --file src/main/java/Mapper.java \
  --by CodeReviewer
```

### Coder responds

```bash
scripts/flow-log.sh respond-finding \
  --feature my-feature \
  --id 1 \
  --status FIXED \
  --note "Added the missing null check." \
  --by JavaCoder
```

### Reviewer adjudicates

```bash
scripts/flow-log.sh resolve-finding --feature my-feature --id 1 --by CodeReviewer
scripts/flow-log.sh reopen-finding --feature my-feature --id 1 --reason "Only one call site was fixed" --by CodeReviewer
```

### Team Lead records non-blocking debt

Use this only for `MEDIUM` or `LOW` findings that are consciously being carried forward.

```bash
scripts/flow-log.sh decide-finding \
  --feature my-feature \
  --id 2 \
  --status ACCEPTED \
  --reason "Readable enough for now; not worth another coder loop." \
  --by TL

scripts/flow-log.sh decide-finding \
  --feature my-feature \
  --id 3 \
  --status DEFERRED \
  --reason "Follow up in refactor ticket." \
  --follow-up refactor-ticket \
  --by TL
```

`ACCEPTED` means TL reviewed the finding and is consciously carrying it in the current delivery.
`DEFERRED` means TL reviewed the finding and is carrying it with an explicit later follow-up.
Signoff blocks when non-blocking findings remain undecided.

### Round management

```bash
scripts/flow-log.sh increment-code-review-round --feature my-feature
scripts/flow-log.sh code-review-gate --feature my-feature
```
