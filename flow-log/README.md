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

# Run verification scripts and record results atomically:
node flow-log/flow-log.mjs run-check --feature my-feature --name verifyQuick --by JavaCoder
node flow-log/flow-log.mjs run-check --feature my-feature --name finalCheck --by TeamLead
node flow-log/flow-log.mjs run-check --feature my-feature --name karate --by TeamLead

# Manual override (when script was run outside flow-log):
node flow-log/flow-log.mjs set-check --feature my-feature --name finalCheck --status PASS --by TL --command scripts/final-check.sh

node flow-log/flow-log.mjs summary --feature my-feature
node flow-log/flow-log.mjs status --feature my-feature
node flow-log/flow-log.mjs readiness signoff --feature my-feature
```

### run-check Details

`run-check` executes a verification script via `spawnSync`, captures exit code and output, and records the result in flow-log state atomically. This replaces the old 3-step pattern of: run script → read output → call `set-check`.

```bash
# Uses the default script mapped to each check name:
node flow-log/flow-log.mjs run-check --feature my-feature --name verifyQuick
#   → runs scripts/verify-quick.sh

node flow-log/flow-log.mjs run-check --feature my-feature --name finalCheck
#   → runs scripts/final-check.sh

node flow-log/flow-log.mjs run-check --feature my-feature --name karate
#   → runs scripts/karate-test.sh

# Override the script:
node flow-log/flow-log.mjs run-check --feature my-feature --name verifyQuick --command path/to/script.sh

# Custom timeout (default 300s):
node flow-log/flow-log.mjs run-check --feature my-feature --name karate --timeout 600000
```

**Output:** Structured JSON with `ok`, `status` (PASS/FAIL), `exitCode`, `outputTail` (last 80 lines), `durationMs`, `timedOut`, `sourceFingerprint` (16-char hex hash on PASS, `null` on FAIL).

**Default script mapping:**
| Check name | Script |
|---|---|
| `verifyQuick` | `scripts/verify-quick.sh` |
| `finalCheck` | `scripts/final-check.sh` |
| `karate` | `scripts/karate-test.sh` |

### verify-all

Runs all three checks in sequence (`verifyQuick` → `finalCheck` → `karate`), stopping on the first failure. Each check is saved to state immediately after completion.

```bash
# Run all checks:
node flow-log/flow-log.mjs verify-all --feature my-feature --by JavaCoder

# Custom timeout (applies to each check):
node flow-log/flow-log.mjs verify-all --feature my-feature --timeout 600000
```

**Output:** Structured JSON with `ok`, `results` (array of `{check, status, durationMs}`). On failure, includes `stoppedAt` (check name) and `failedCheck` (full result with `outputTail`).

### Source Fingerprinting And Staleness

When a check passes via `run-check` or `verify-all`, flow-log records a `sourceFingerprint` — a SHA-256 hash (16-char hex) computed from the modification times of all source files in `flow-orchestrator/src/` plus `pom.xml` (extensions: `.java`, `.xml`, `.properties`, `.yml`, `.yaml`, `.json`, `.feature`).

The `status` command computes a live fingerprint and compares it against the recorded fingerprint for each check:

```bash
node flow-log/flow-log.mjs status --feature my-feature
# Output includes: verifyQuickStale, finalCheckStale, karateStale (boolean)
```

- `*Stale: false` — source unchanged since the check passed
- `*Stale: true` — source changed after the check passed; re-run recommended
- `*Stale: false` for `NOT_RUN` or `FAIL` checks — staleness only applies to PASS results

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
- Agents should use `run-check` or `verify-all` instead of running verification scripts directly. `set-check` remains available for manual overrides.
- All commands must run from the repository root directory. Running from a subdirectory without `--state-path` produces an actionable error.
- `set-review` accepts legacy aliases `phase1` → `architectureReview`, `phase2` → `codeReview`.
- `reset-checks` automatically records a `redCard` event.
- `status` = short check; `summary` = medium detail; `history` = event trail.
- `create` records `timing.startedAt`; `complete` records `timing.completedAt` and `durationMinutes`.
