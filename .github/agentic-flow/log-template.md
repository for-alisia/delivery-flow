# Agentic Flow vX.Y.Z - run log

**Date:** YYYY-MM-DD
**Branch:** `branch-name`
**Scope:** Short description of this version / increment
**Compared to:** `vX.Y.Z`

---

## Version Updates

### Change Summary

Use this section to describe what changed in this version compared to the previous one.
Prefer concrete, implementation-focused entries.

| Area | How it was | What is changed | What is now |
|------|------------|-----------------|-------------|
| Example: Architect model fallback | Architect fallback was unstable and often resolved to the wrong model | Fallback chain was simplified and validated | Architect now uses a single verified model |
| | | | |
| | | | |

### Addressed Findings From Previous Log Analysis

List how the issues and suggestions from the previous log were handled in this version.
If one issue required multiple actions, list them in the same cell as separate bullets.

| Issue spotted | How was addressed | What pain point should be solved |
|---------------|-------------------|----------------------------------|
| Example: Coder fought with shell scripts and produced unreliable evidence | - Added dedicated verification scripts<br>- Simplified required command flow<br>- Reduced free-form shell usage in agent instructions | Fewer wasted loops, less random terminal behavior, more reliable handoffs |
| | | |
| | | |

## Flow Setup

Use this section to show the intended flow design for the version captured in this log.
The goal is to make the orchestration path and quality gates visible before the run notes start.

Suggested content:
- Which agent hands work to which next agent
- Which artifact is produced at each step
- Which file acts as the quality gate or handoff evidence
- Where the main approval points exist

### Planning Phase Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                        INTENDED FLOW                                 │
│                                                                      │
│   1:TL ──────► 2:PM ──────► 3:TL ──────► 4:ARCH ──────► 5:TL      │
│    create+lock   story.md     register     plan.json      register   │
│                                                                      │
│   5:TL ──► 6:ARCH-REV ◄──► 4:ARCH (loop) ──► 7:TL                 │
│    increment     risks (UNCLASSIFIED)          arch-gate PASS        │
└──────────────────────────────────────────────────────────────────────┘
```

### Implementation Phase Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                                                                      │
│   7:TL ──────────────────────► 8:CODER ──────────────────► 9:TL    │
│    start-batch (max 2 slices)   run-check + add-change               │
│                                                                      │
│   9:TL ──► 10:CODE-REV ◄──► 8:CODER (loop) ──► 11:TL              │
│    increment   findings         code-review-gate PASS    sign-off    │
└──────────────────────────────────────────────────────────────────────┘
```

### Flow Step Clarification

- **1:TL** — Requirement lock; `create` + `lock-requirements` with request source path
- **2:PM** — Produces `artifacts/user-stories/<feature>.story.md`; story must preserve locked scope and acceptance criteria
- **3:TL** — Story gate; `register-artifact story` + `approve-artifact story`
- **4:ARCH** — Produces `artifacts/implementation-plans/<feature>.plan.json` via v3 draft lifecycle; plan must include slices, payloads, validation boundaries, logging, and tests
- **5:TL** — Plan gate; `validate-plan` + `plan-summary` + `register-artifact plan` + `approve-artifact plan`; then `increment-round` and invoke Architecture Reviewer
- **6:ARCH-REV** — Records risks via `add-risk` (all UNCLASSIFIED); TL classifies severity via `reclassify-risk`; TL runs `architecture-gate`
- **7:TL** — Architecture gate passed; `set-review architectureReview PASS`; hands off approved slices to Coder
- **8:CODER** — Implements slices; records checks via `run-check` and files via `add-change`; runs `verify-all` before handoff; runs `coder-handoff-check.sh`
- **9:TL** — Coder gate; checks `*Stale` fields in `flow-log status`; independent `run-check --name finalCheck` + `run-check --name karate`; `increment-code-review-round` and invokes Code Reviewer
- **10:CODE-REV** — Records findings via `add-finding`; updates `capabilities/<capability>.md` and `.http` examples
- **11:TL** — Final audit gate; `readiness signoff` must return `ready: true`; `complete` to record timing

---

## Runs

Each run is a separate delivery attempt within this version.
Record one expandable section per run. Copy the run template below for each new run.
Between runs, apply fixes and document them in the "Fixes Applied After This Run" subsection.

<!-- ────────────── RUN TEMPLATE (copy for each run) ────────────── -->

<details>
<summary><strong>Run N — YYYY-MM-DD — short label</strong></summary>

### Run Configuration

- **Feature:** feature-name or description
- **Environment:** IDE / CLI
- **Flow-log state:** `artifacts/flow-logs/<feature-name>.json` (if used)
- **Models:**
  - Team Lead: model-name
  - Product Manager: model-name
  - Architect: model-name
  - Coder: model-name
  - Reviewer: model-name

### Run Notes

Chronological observations from the run. Write freely — timestamps, context %, and key moments.

- 
- 
- 

### Post Run Checks

- Application started without errors:
- Tests are green:
- Code-quality check passed:
- API is verified:
- Main story functionality is delivered:
- Run took (time):
- Run used context (%):
- Run took premium requests (optional):

### Code Observations

Capture code quality, structure, naming, readability, test quality, architecture, and maintainability observations.

- 
- 
- 

### Bugs Identified

Describe confirmed bugs, missing behavior, requirement mismatches, or suspicious areas.

- 
- 
- 

### Fixes Applied After This Run

List prompt, workflow, code, or tooling fixes applied before the next run. Leave empty if this is the last run.

- 
- 
- 

</details>

<!-- ────────────── END RUN TEMPLATE ────────────── -->

## User Suggestions (manually populated by user)

Use this section for improvement ideas for the next version of the flow, tooling, prompts, agent setup, verification, or coding standards.
Applies to the version as a whole, not a single run.

- 
- 
- 

---

## Agent Analysis

**Analyzed by:** GitHub Copilot (Claude Opus)
**Analysis date:** YYYY-MM-DD
**Scope:** Full run analysis using this log, linked artifacts, generated code, and comparison with the previous version

### 1. Executive Summary

Provide a short, opinionated summary of the run:
- Was this version an improvement, regression, or mixed result?
- Did the workflow produce usable code?
- What were the 3-5 most important takeaways?

### 2. Run Snapshot

Summarize the run in a compact, comparable format.

| Dimension | Result | Notes |
|-----------|--------|-------|
| Outcome quality |  |  |
| Requirement fidelity |  |  |
| Code quality |  |  |
| Verification reliability |  |  |
| Agent coordination |  |  |
| Tooling effectiveness |  |  |
| Context efficiency |  |  |
| Time efficiency |  |  |

### 3. Observation Analysis

This section should heavily use the user-entered notes above and turn them into clear conclusions.

#### 3.1 What Worked Well

List the strongest parts of the run:
- Which agents behaved well
- Which workflow changes helped
- Which tools or templates worked
- Which quality or architecture choices should be kept

#### 3.2 What Failed or Regressed

Focus on the highest-impact failures first:
- Requirement loss
- Wrong model pickup
- Bad handoffs
- Verification false positives
- Context rot
- Random terminal behavior
- Poor code quality patterns
- Weak review value

For each important issue, explain:
- What happened
- Why it likely happened
- What impact it had on time, context, quality, or trust

#### 3.3 Bottleneck Analysis

Identify the biggest delivery bottlenecks across the run.

| Bottleneck | Evidence from run | Impact | Likely root cause |
|------------|-------------------|--------|-------------------|
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |

#### 3.4 Agent-by-Agent Assessment

Evaluate each relevant role separately.

| Agent | What went well | What went wrong | Recommendation |
|-------|----------------|-----------------|----------------|
| Team Lead |  |  |  |
| Product Manager |  |  |  |
| Architect |  |  |  |
| Coder |  |  |  |
| Reviewer |  |  |  |

#### 3.5 Actual Flow Execution Diagram

Use this section to show how the flow actually behaved in this run based on the user notes and artifacts.
This is intentionally different from the setup diagram above.

Capture deviations such as:
- Context rot
- Rejected handoffs
- Repeated coder loops
- Reviewer reruns
- Missing artifacts
- Wrong model pickup
- Gate failures

##### Planning Phase - Actual

```
┌──────────────────────────────────────────────────────────────────────┐
│                        ACTUAL FLOW — vX.Y.Z                          │
│                                                                      │
│   1:TL ── 2:PM ── 3:TL ── 4:ARCH ── 5:TL ── 6:ARCH-REV ── 7:TL   │
│   create   story   register  plan    register  risks                 │
│                                                                      │
│  (annotate retries inline, e.g.: 4:ARCH×2 if Architect was          │
│   re-invoked after architecture review risks)                        │
└──────────────────────────────────────────────────────────────────────┘
```

##### Implementation Phase - Actual

```
┌──────────────────────────────────────────────────────────────────────────┐
│                                                                          │
│   8:CODER×1 ─✗─ 8:CODER×2 ─✗─ 8:CODER×3 ─✓─ 9:TL                    │
│   <reason>      <reason>        set-check+add-change                     │
│                                                                          │
│   9:TL ── 10:CODE-REV×1 ─✗─ 10:CODE-REV×2 ─✓─  11:TL / BLOCKED      │
│   final-check  <reason>          findings resolved    sign-off           │
│                                                                          │
│  CTX   ████  ██  ████  ██  ███  ██  ████████████  ██  ▓▓▓▓  ░░░░      │
│                                            ↑                  ↑         │
│                                       compacted            corrupt       │
└──────────────────────────────────────────────────────────────────────────┘
```

##### Actual Flow Deviations

Describe what deviated from the intended flow and the cost of each deviation:
- **Planning deviations** (PM retries, Architect re-invocations, architecture review rounds):
- **Coder loops** (count, root cause per loop, false positives):
- **Code Reviewer reruns** (count, root cause, context state at time of failure):
- **Biggest single cost or quality impact:**

### 4. Cost And Context Efficiency

Use this section to explicitly analyze whether the run was cost-effective, not only whether it produced acceptable output.

#### 4.1 Cost Snapshot

| Cost dimension | Result | Notes |
|----------------|--------|-------|
| Total context used |  |  |
| Premium requests used |  |  |
| Most expensive phase |  |  |
| Most expensive agent |  |  |
| Number of costly reruns / loops |  |  |
| Cost efficiency verdict |  |  |

#### 4.2 Main Cost Drivers

Explain what consumed the most context, time, or premium requests.
Typical examples:
- Overly large plan artifacts
- Repeated coder retries
- Reviewer reruns with low added value
- Repeated terminal failures
- Weak handoffs causing rework
- Unnecessary model usage for low-value tasks

#### 4.3 Cost Reduction Actions

List concrete actions that should reduce context usage, premium requests, and wasted loops in the next run.

| Action | Expected savings | Trade-off / risk |
|--------|------------------|------------------|
|  |  |  |
|  |  |  |
|  |  |  |

### 5. Improvement Plan

Turn the observations into a concrete next-version plan.

#### 5.1 Priority Actions

Order actions by impact and urgency.

| Priority | Action | Why it matters | Expected impact | Owner |
|----------|--------|----------------|-----------------|-------|
| P1 |  |  |  |  |
| P1 |  |  |  |  |
| P2 |  |  |  |  |
| P3 |  |  |  |  |

#### 5.2 Recommended Prompt / Workflow Changes

Use this subsection for changes to:
- Agent instructions
- Handoff structure
- Model selection
- Red card or circuit breaker logic
- Validation gates
- Context management
- Tool permissions

#### 5.3 Recommended Tooling / Script Changes

Use this subsection for:
- New helper scripts
- Better verification flows
- Better quality gates
- Formatting / linting automation
- Test data and smoke test improvements

#### 5.4 Metrics To Track In The Next Run

Define measurable targets so future logs can compare results consistently.

| Metric | Current run | Next target |
|--------|-------------|-------------|
| Total run time |  |  |
| Context used |  |  |
| Premium requests used |  |  |
| Coder retry loops |  |  |
| Reviewer reruns |  |  |
| Manual fixes after run |  |  |

### 6. Code Change Analysis

This section should analyze the generated code itself, not only the process.

#### 6.1 Architecture and Boundaries

Review whether the produced code respects:
- Package structure
- Separation of concerns
- Domain / orchestration / integration boundaries
- DTO and mapper discipline
- Error handling location
- Configuration ownership

#### 6.2 Maintainability and Readability

Review:
- Naming quality
- Model count and necessity
- Null-handling patterns
- Constructor patterns
- Reuse vs duplication
- Auto-generated feel vs human-maintainable structure

#### 6.3 Testing and Verification Quality

Review:
- Test coverage depth
- Missing edge cases
- Correct test placement
- Usefulness of smoke tests
- Confidence level from verification evidence

#### 6.4 Risks and Follow-Up Refactors

List the most important technical risks that remain after the run.

| Risk | Why it matters | Recommended follow-up |
|------|----------------|-----------------------|
|  |  |  |
|  |  |  |
|  |  |  |

### 7. Final Verdict

Close with a direct conclusion:
- Is this version ready to keep as the new baseline?
- Which parts should definitely remain?
- Which parts should be changed before the next run?
- Is the system trending toward better quality/cost balance or away from it?

---

## Research Summary

- Reviewed previous run logs to identify stable sections, useful analysis patterns, and recurring pain points.
- Structured the template so user-entered observations can be reused directly by the analysis section.
- Added a stronger Opus-friendly analysis frame focused on bottlenecks, improvement planning, and code change quality.
