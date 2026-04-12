# Flow-Log Log Refactoring Plan

## Purpose

Refactor the **working log part** of `flow-log` into a clean, expandable structure without changing user-visible behavior.

This document is intentionally implementation-ready for `Java Coder` to start from. The goal is not to redesign the workflow, but to:

- keep the current CLI contract stable
- split the current log implementation into focused modules
- extract shared logic
- make future changes safer
- preserve the current tests and current behavior

Plan-related redesign is explicitly out of scope for this document.

---

## 1. Scope

### In scope

- Refactor the log/state side currently centered in [log-state.mjs](/Users/alisia/Projects/aiProjects/GitlabFlow/flow-log/src/log-state.mjs:1)
- Refactor the log command handlers currently centered in [log-commands.mjs](/Users/alisia/Projects/aiProjects/GitlabFlow/flow-log/src/log-commands.mjs:1)
- Keep [cli.mjs](/Users/alisia/Projects/aiProjects/GitlabFlow/flow-log/src/cli.mjs:1) thin
- Preserve all existing log commands and JSON outputs unless explicitly called out below
- Preserve and expand existing tests in `flow-log/test`

### Out of scope

- Changing plan commands or plan schema
- Changing orchestration prompts in this implementation phase
- Changing user-facing command names
- Redesigning approval flow
- Replacing JSON file storage

---

## 2. Current Problems

### Structural problems

- `log-state.mjs` currently mixes schema, persistence, state mutation, summary building, gate calculation, and ID/event helpers.
- `log-commands.mjs` currently mixes CLI routing, argument parsing decisions, state loading/saving, domain mutations, and JSON response shaping.
- Shared helpers are scattered and implicit, which makes safe extension slower.
- The file is already long enough that simple changes risk accidental regressions.

### Concrete correctness risk already visible

- `EVENT_TYPES` contains `batchStart`, `batchEnd`, and `archEscalationDecision` in [log-state.mjs](/Users/alisia/Projects/aiProjects/GitlabFlow/flow-log/src/log-state.mjs:9), but `summarizeEventCounts` only initializes counters for `redCard`, `rejection`, `reroute`, and `note` in [log-state.mjs](/Users/alisia/Projects/aiProjects/GitlabFlow/flow-log/src/log-state.mjs:331).
- This should be corrected during refactor so event summaries are stable for all supported event types.

### Design principle to preserve

The current log part works because it records **small workflow facts** after decisions are made. The refactor must preserve that property.

---

## 3. Target Structure

Target source structure:

```text
flow-log/src/
  cli.mjs
  cli-helpers.mjs
  state.mjs
  log/
    common.mjs
    schema.mjs
    store.mjs
    artifacts.mjs
    reviews.mjs
    checks.mjs
    events.mjs
    batches.mjs
    risks.mjs
    findings.mjs
    gates.mjs
    summary.mjs
    index.mjs
  log-commands/
    lifecycle.mjs
    artifacts.mjs
    reviews.mjs
    checks.mjs
    events.mjs
    batches.mjs
    risks.mjs
    findings.mjs
    queries.mjs
    shared.mjs
    index.mjs
```

### Responsibility of each module

`log/common.mjs`

- `timestamp`
- enum validation
- positive integer parsing helpers that are log-domain specific if needed later
- ID helpers: next event ID, next risk ID, next finding ID
- small pure helpers only

`log/schema.mjs`

- exported enums/constants
- `createInitialState`
- `createArtifactEntry`
- `createReviewEntry`
- `createCheckEntry`
- `validateStateShape`

`log/store.mjs`

- `resolveStatePath`
- `loadState`
- `saveState`
- `ensureFeatureMatches`

`log/artifacts.mjs`

- artifact existence helpers
- artifact verification helpers
- artifact registration/approval mutations

`log/reviews.mjs`

- review mutation helpers
- review summarization if still needed

`log/checks.mjs`

- check mutation helpers
- check reset helpers

`log/events.mjs`

- `appendEvent`
- event count summaries
- event normalization

`log/batches.mjs`

- `startBatch`
- `completeBatch`
- batch summary helpers

`log/risks.mjs`

- `addRisk`
- `respondToRisk`
- `resolveRisk`
- `reopenRisk`
- `incrementReviewRound`
- unresolved/high-risk queries used by gates

`log/findings.mjs`

- `addFinding`
- `respondToFinding`
- `resolveFinding`
- `reopenFinding`
- `incrementCodeReviewRound`

`log/gates.mjs`

- `buildSignoffReadiness`
- `buildArchitectureGate`
- `buildCodeReviewGate`
- `deriveCurrentPhase`

`log/summary.mjs`

- `buildStatus`
- `buildSummary`
- compact summarizers for artifacts/checks/reviews/events/risks/findings/batches

`log/index.mjs`

- stable public exports used by command handlers and compatibility layer

### Command structure

`log-commands/lifecycle.mjs`

- `create`
- `lock-requirements`
- `complete`

`log-commands/artifacts.mjs`

- `register-artifact`
- `approve-artifact`

`log-commands/reviews.mjs`

- `set-review`

`log-commands/checks.mjs`

- `set-check`
- `add-change`

`log-commands/events.mjs`

- `add-event`

`log-commands/batches.mjs`

- `start-batch`
- `complete-batch`
- `reset-checks`

`log-commands/risks.mjs`

- `add-risk`
- `respond-risk`
- `resolve-risk`
- `reopen-risk`
- `increment-round`
- `architecture-gate`

`log-commands/findings.mjs`

- `add-finding`
- `respond-finding`
- `resolve-finding`
- `reopen-finding`
- `increment-code-review-round`
- `code-review-gate`

`log-commands/queries.mjs`

- `get`
- `history`
- `status`
- `summary`
- `readiness`

`log-commands/shared.mjs`

- `openState`
- `assertStateDoesNotExist`
- common response helpers only if they reduce duplication cleanly

---

## 4. Public API Rules

These rules are mandatory during refactor:

- `node flow-log/flow-log.mjs <existing-log-command>` must keep working
- Existing JSON response shapes should remain unchanged unless there is a clear correctness bug
- Existing tests must stay green
- [state.mjs](/Users/alisia/Projects/aiProjects/GitlabFlow/flow-log/src/state.mjs:1) should remain as a compatibility re-export file during the transition
- [cli.mjs](/Users/alisia/Projects/aiProjects/GitlabFlow/flow-log/src/cli.mjs:1) should remain the single dispatch entrypoint

---

## 5. Detailed Refactoring Approach

### Phase 1: Create the new log core modules

Create the new `flow-log/src/log/` directory and move pure logic first.

Order:

1. Create `common.mjs`
2. Create `schema.mjs`
3. Create `store.mjs`
4. Create `events.mjs`
5. Create `batches.mjs`
6. Create `risks.mjs`
7. Create `findings.mjs`
8. Create `gates.mjs`
9. Create `summary.mjs`
10. Create `index.mjs`

Important rule:

- During this phase, leave `log-state.mjs` in place and make it delegate to the new modules if needed.
- Do not delete `log-state.mjs` until tests are migrated and passing.

### Phase 2: Extract command handlers by domain

Create `flow-log/src/log-commands/`.

Order:

1. Move simple query handlers first: `get`, `status`, `summary`, `history`, `readiness`
2. Move artifact handlers
3. Move review/check/event handlers
4. Move batch handlers
5. Move risk handlers
6. Move finding handlers
7. Create `index.mjs` with `dispatchLogCommand` and `LOG_COMMAND_HELP`

Important rule:

- Preserve the command help text exactly unless a bug or stale text is being corrected.

### Phase 3: Reduce `log-state.mjs` and `log-commands.mjs` to compatibility wrappers

After all logic is moved:

- make `log-state.mjs` re-export from `./log/index.mjs`
- make `log-commands.mjs` re-export from `./log-commands/index.mjs`

At this point, the old filenames still exist for compatibility, but the real implementation is split.

### Phase 4: Correct small correctness issues discovered during extraction

Allowed in this phase:

- fix `summarizeEventCounts` so all supported event types are counted deterministically
- normalize event summary output shape so no event type can produce `NaN`
- add missing tests for any bug fixed here

Not allowed in this phase:

- redesign event schema
- add new commands
- change gate semantics

---

## 6. Function Migration Map

Move the current functions as follows.

From `log-state.mjs`:

| Current function/area | Target module |
|---|---|
| constants/enums | `log/schema.mjs` |
| `resolveStatePath`, `loadState`, `saveState`, `ensureFeatureMatches` | `log/store.mjs` |
| `validateValue`, ID helpers, timestamp helper | `log/common.mjs` |
| artifact helpers | `log/artifacts.mjs` |
| `appendChangedFiles` | `log/checks.mjs` or `log/events.mjs` is wrong; keep in `log/checks.mjs` or create `log/changes.mjs` only if it becomes large |
| `appendEvent` | `log/events.mjs` |
| `buildSignoffReadiness`, `deriveCurrentPhase`, architecture/code gates | `log/gates.mjs` |
| `buildSummary`, `buildStatus`, summarizers | `log/summary.mjs` |
| batch helpers | `log/batches.mjs` |
| risk lifecycle helpers | `log/risks.mjs` |
| finding lifecycle helpers | `log/findings.mjs` |

From `log-commands.mjs`:

| Current handler group | Target module |
|---|---|
| create/lock/complete | `log-commands/lifecycle.mjs` |
| register/approve artifact | `log-commands/artifacts.mjs` |
| set-review | `log-commands/reviews.mjs` |
| set-check/add-change | `log-commands/checks.mjs` |
| add-event | `log-commands/events.mjs` |
| batch/reset handlers | `log-commands/batches.mjs` |
| risk handlers | `log-commands/risks.mjs` |
| finding handlers | `log-commands/findings.mjs` |
| query handlers | `log-commands/queries.mjs` |
| `openState`, no-exist checks | `log-commands/shared.mjs` |

---

## 7. Testing Requirements

### Existing tests that must remain green

- [log-cli.test.mjs](/Users/alisia/Projects/aiProjects/GitlabFlow/flow-log/test/log-cli.test.mjs:1)

### Tests to add during refactor

Add focused tests for modules and edge cases that are currently implicit.

#### Event summary tests

- counts all supported event types
- does not produce `NaN`
- includes zero counts for unsupported/missing categories only if that is the intended response shape

#### Gate tests

- architecture gate returns `PASS` when no unresolved Critical/High risks remain
- architecture gate returns `FAIL` when unresolved Critical/High risks remain
- architecture gate returns `ESCALATE` at the configured max round
- same set for code review gate

#### Persistence tests

- `saveState` updates `updatedAt`
- `loadState` rejects invalid state shape
- feature mismatch is rejected

#### Batch tests

- cannot complete when no batch is active
- current batch and history counts remain correct across multiple batches

### Test style guidance

- Keep command-level tests for public behavior
- Add pure-function tests only where they reduce regression risk cleanly
- Avoid snapshot-heavy tests

---

## 8. Acceptance Criteria

This refactor is complete only when all of these are true:

- The log implementation is split into domain modules under `flow-log/src/log/`
- The log command implementation is split into domain modules under `flow-log/src/log-commands/`
- `cli.mjs` remains a thin dispatcher
- `state.mjs` remains a compatibility export layer
- Existing log commands still behave the same
- Existing tests pass
- New tests cover the event summary bug and core gates
- No plan-related code was redesigned as part of this task

---

## 9. Recommended Implementation Order for Coder

Use this order to keep the change low-risk:

1. Create `log/common.mjs`, `log/schema.mjs`, `log/store.mjs`
2. Move risk/finding/batch/event logic
3. Move summary/gate logic
4. Add `log/index.mjs`
5. Run tests
6. Split command handlers into `log-commands/`
7. Rewire `log-commands.mjs` into compatibility export
8. Add focused regression tests
9. Correct event summary counting
10. Final test run

---

## 10. Explicit Non-Goals for Coder

- Do not redesign the plan system in this task
- Do not rename commands
- Do not change output from pretty JSON to another format
- Do not introduce external dependencies
- Do not merge plan and log logic together again

---

## 11. Follow-Up After This Refactor

Once this log refactor is complete and stable:

- update stale docs that still reference `plan.md`
- then start the separate plan-system redesign
- only after that, retest the full architect-review loop in a realistic run
