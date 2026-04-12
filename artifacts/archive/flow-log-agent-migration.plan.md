# Flow-Log Agent Migration Plan

## Purpose

Migrate the agentic delivery flow to use `flow-log` as the **single source of truth** for workflow state. Eliminate unnecessary housekeeping artifacts (verification logs, implementation reports, review reports, signoff JSONs). Keep only story and plan as human-readable artifacts alongside the flow-log state file.

---

## 1. Design Direction

### What `flow-log` replaces

| Before (v2.3.0) | After (v2.4.0+) | Rationale |
|------------------|------------------|-----------|
| `/memories/session/<feature>-checkpoint.json` | `flow-log status` / `flow-log get` | Flow-log already tracks phase, requirements, artifacts, reviews, checks, batches, events — everything the checkpoint duplicated |
| `artifacts/implementation-reports/<feature>.report.json` | `flow-log` checks + events + changes | Report was a manual JSON artifact that agents struggled to write correctly. Flow-log records the same facts via simple CLI commands |
| `artifacts/implementation-reports/<feature>-verification.log` | `flow-log set-check` with `--details` and `--command` flags | Verification evidence is captured per-check in the state file |
| `artifacts/review-reports/<feature>.review.json` | `flow-log set-review` + `add-event` for failures | Review gate status is in flow-log. Detailed review findings go in events or the Reviewer's chat output |
| `artifacts/implementation-signoffs/<feature>.signoff.json` | `flow-log readiness signoff` + `flow-log complete` | Readiness is computed, not manually written. `complete` records timing |
| `/memories/session/<feature>-phase2-brief.md` | `flow-log summary` | Summary provides all context Reviewer needs for Phase 2 |

### What stays

| Artifact | Why |
|----------|-----|
| `artifacts/user-stories/<feature>.story.md` | Human-readable, business-facing. Registered in flow-log |
| `artifacts/implementation-plans/<feature>.plan.md` | Human-readable, technical. Registered in flow-log |
| `artifacts/flow-logs/<feature>.json` | Machine-owned state file. The single source of truth |

### What this eliminates

- TL manually maintaining checkpoint JSON after every stage transition
- Coder writing implementation report JSON (the #1 cause of handoff failures)
- Coder writing verification log (replaced by `set-check`)
- Reviewer searching for verification evidence files on disk
- TL writing signoff JSON
- TL writing Phase 2 brief for Reviewer
- Double-bookkeeping between checkpoint and flow-log

---

## 2. Checkpoint vs Flow-Log: Field-by-Field Analysis

### Checkpoint fields and their flow-log equivalents

| Checkpoint field | Flow-log equivalent | Notes |
|------------------|---------------------|-------|
| `featureName` | `state.feature` | Identical |
| `currentStage` / `nextStage` | `deriveCurrentPhase(state)` via `status` | Computed from actual gate statuses, not manually set — more reliable |
| `originalRequestSource` | **Not in flow-log** | Needs to be added — this is the user prompt path that agents need to read |
| `requirementLock[]` | `state.requirements.locked` | Flow-log tracks lock status but not the lock content (the constraint text). Agents read constraints from the story, not the checkpoint |
| `artifactPaths.*` | `state.artifacts.story.path`, `state.artifacts.plan.path` | Flow-log tracks story and plan paths. Other artifact paths (report, review, signoff) are eliminated |
| `latestStageResult` | `state.events[-1]` + `state.checks` + `state.reviews` | Decomposed into richer, per-gate tracking |
| `circuitBreakerState` | `state.events` (count `redCard` events per target) | Can be derived: `history` shows all red cards. Coder false-positive count = count of `redCard` events targeting `JavaCoder` |
| `handoffContext.reviewPhase` | `deriveCurrentPhase()` | Computed |
| `handoffContext.changedFiles` | `state.changes.files` | Identical |
| `handoffContext.approvedDeviations` | **Not in flow-log** | Could be added as events of type `note` with a convention, or as a new field. For v2.4.0, deviations go in `add-event --type note` |
| `handoffContext.openBlockers` | **Not in flow-log** | Same — use `add-event --type note` |
| `invocationRule` | Can be a static string in agent prompt | Doesn't need to be in state |

### What agents actually use from the checkpoint

Tracing every agent's checkpoint usage:

1. **Team Lead** — creates and updates it. After migration: TL runs flow-log commands instead. `flow-log status` replaces checkpoint reads for gate decisions.

2. **Product Manager** — reads `featureName`, `originalRequestSource`, and `requirementLock`. After migration: PM receives feature name and request source in the TL invocation prompt. PM reads the story template path from the flow overview. Requirement lock constraints are in the request source doc, not the checkpoint.

3. **Java Architect** — reads `featureName`, story path, locked constraints. After migration: TL invocation prompt provides feature name. `flow-log get` provides story path. Constraints are in the story itself.

4. **Java Coder** — reads plan path, Phase 1 review path, changed files. After migration: `flow-log get` provides plan path. Phase 1 status is in `flow-log status`. Changed files are in `flow-log get`.

5. **Reviewer** — reads checkpoint for artifact paths and current phase. After migration: `flow-log summary` provides everything — phase, artifact paths, check statuses, review statuses, events.

### Conclusion

**Flow-log can fully replace the checkpoint** with two small additions:
1. Add `requirementSource` field to store the original request path (set at `lock-requirements`)
2. Use `add-event --type note` for approved deviations and open blockers

The `circuitBreakerState` can be derived from event history (`redCard` count per target). The `invocationRule` is a static string that belongs in agent prompts, not in state.

---

## 3. Agent Migration Plan

### Invocation pattern change

**Before:** TL passes checkpoint path, agent reads checkpoint to find artifact paths.
**After:** TL passes feature name, agent runs `flow-log summary` or `flow-log get` to get all context.

New invocation format:
```
"<task> for <feature-name>. Run `node flow-log/flow-log.mjs summary --feature <feature-name>` to load context and follow your instructions."
```

### Team Lead changes

| Current behavior | New behavior |
|------------------|--------------|
| Creates checkpoint JSON from template | `flow-log create` + `lock-requirements` |
| Updates checkpoint after every stage | `flow-log` commands auto-update state |
| Reads checkpoint for gate checks | `flow-log status` |
| Writes Phase 2 brief for Reviewer | `flow-log summary` replaces the brief |
| Writes signoff JSON from template | `flow-log readiness signoff` + `flow-log complete` |
| Writes verification log | `flow-log set-check` with details |
| Writes implementation report evidence | Not needed — checks are in flow-log |
| Manages circuit-breaker count | `flow-log history` — count `redCard` events targeting `JavaCoder` |
| Sends coder back with `reset-checks` | `flow-log reset-checks` (resets all checks + records red card) |
| Tracks batch boundaries manually | `flow-log start-batch` / `complete-batch` |

### Coder changes

| Current behavior | New behavior |
|------------------|--------------|
| Writes implementation report JSON | Not needed |
| Writes verification log | Not needed |
| Updates context-map (often forgotten) | Still required — this is a code artifact, not state |
| Reports changed files in handoff | `flow-log add-change` |
| Reports check status in handoff | `flow-log set-check` |
| Returns 5-field handoff summary | `flow-log status` output IS the handoff |

### Reviewer changes

| Current behavior | New behavior |
|------------------|--------------|
| Reads checkpoint for artifact paths | `flow-log summary` |
| Reads verification log for evidence | `flow-log summary` → checks section |
| Searches for Karate reports on disk | `flow-log summary` → checks.karate status |
| Writes review report JSON | `flow-log set-review` with status. Detailed findings go in review output (chat), not a JSON file |
| Hunts for evidence files | Not needed — all evidence is in flow-log |

### Architect and PM changes

Minimal. They receive feature name from TL, run `flow-log summary` to get story/plan paths if needed, and produce their artifacts as before.

---

## 4. Implementation Order

1. **Add `--request-source` flag to `lock-requirements`** — stores original request path in flow-log state
2. **Update `team-lead.agent.md`** — replace checkpoint management with flow-log commands
3. **Update `java-coder.agent.md`** — remove implementation report writing, add flow-log commands
4. **Update `reviewer.agent.md`** — replace checkpoint/evidence reads with `flow-log summary`
5. **Update `java-architect.agent.md` and `product-manager.agent.md`** — replace checkpoint reads with `flow-log summary`
6. **Update `delivery-flow-rules.instructions.md`** — add flow-log as the state management tool
7. **Update `agentic-flow-overview.md`** — reflect new artifact list and flow-log integration
8. **Update `copilot-instructions.md`** — add `flow-log/` to project structure
9. **Remove templates for eliminated artifacts** — `implementation-report-template.json`, `implementation-signoff-template.json`, `review-report-template.json`, `context-checkpoint-template.json`
10. **Run and validate**

---

## 5. Agent-Command Mapping

| Command | Team Lead | Coder | Reviewer | Architect | PM |
|---------|-----------|-------|----------|-----------|-----|
| `create` | ✓ | | | | |
| `lock-requirements` | ✓ | | | | |
| `register-artifact` | ✓ | | | | |
| `approve-artifact` | ✓ | | | | |
| `set-review` | ✓ | | | | |
| `set-check verifyQuick` | | ✓ | | | |
| `set-check finalCheck` | ✓ (recheck) | ✓ (pre-handoff) | | | |
| `set-check karate` | ✓ | | | | |
| `add-change` | | ✓ | | | |
| `add-event` | ✓ | | | | |
| `start-batch` | ✓ | | | | |
| `complete-batch` | ✓ | | | | |
| `reset-checks` | ✓ | | | | |
| `complete` | ✓ | | | | |
| `status` | ✓ | ✓ (pre-handoff) | | | |
| `summary` | ✓ | | ✓ | ✓ | ✓ |
| `readiness signoff` | ✓ | | | | |
| `get` | ✓ | | | | |
| `history` | ✓ | | ✓ | | |

---

## 6. Risk Assessment

| Risk | Mitigation |
|------|------------|
| Agents forget to run flow-log commands | Commands produce clear JSON errors. TL validates via `status` after every handoff. `readiness signoff` is the final blocker |
| Flow-log state corrupted | Pure Node.js, atomic writes, no external deps. 7 passing tests. Recovery: `create --force` and re-record |
| Loss of detail from eliminated reports | Verification evidence is in `set-check --details`. Events capture red cards, rejections, deviations. Story and plan remain as files. The eliminated reports were overhead that confused agents |
| Reviewer loses structured JSON report format | Review findings shift to the reviewer's chat output to TL. The gate status (`PASS`/`FAIL`) is in flow-log. Detailed findings don't need to survive beyond the conversation |
| Context map updates still forgotten by Coder | Keep as a Coder responsibility. `flow-log` doesn't replace code-level artifacts — only workflow state artifacts |
