# Plan Refactoring Implementation Plan

## Purpose

Implement the next version of the plan workflow so `Java Architect` can produce a strong plan without fighting terminal JSON, while `Java Coder`, `Architecture Reviewer`, and `Team Lead` can still operate from one canonical artifact.

This document is intentionally more implementation-oriented than [arch-plan-refactoring.md](/Users/alisia/Projects/aiProjects/GitlabFlow/.github/temp/arch-plan-refactoring.md:1). It defines:

- what should be built first
- what the initial structure should be
- how the review/edit loop should work
- what should be deferred to later iterations

---

## 1. Validate Current Log Baseline

Before building the new plan workflow, the current log split is good enough as a baseline and should **not** block the plan feature work.

### What is already in a workable state

- [cli.mjs](/Users/alisia/Projects/aiProjects/GitlabFlow/flow-log/src/cli.mjs:1) already separates log and plan dispatch.
- [log-state.mjs](/Users/alisia/Projects/aiProjects/GitlabFlow/flow-log/src/log-state.mjs:1) and [plan-state.mjs](/Users/alisia/Projects/aiProjects/GitlabFlow/flow-log/src/plan-state.mjs:1) are already split.
- [log-commands.mjs](/Users/alisia/Projects/aiProjects/GitlabFlow/flow-log/src/log-commands.mjs:1) and [plan-commands.mjs](/Users/alisia/Projects/aiProjects/GitlabFlow/flow-log/src/plan-commands.mjs:1) are already split.
- The log side already provides durable review state for risks and findings, which is the correct place to keep reviewer commentary.

### Conclusion

For the plan feature MVP:

- do **not** pause for deeper log refactoring
- reuse the current log/risk machinery
- add only the minimum log-side extensions required for the new plan lifecycle

### Log-side additions that are required for the plan feature

These are the only log changes that should be treated as part of the plan-feature implementation:

1. Plan artifact approval must be tied to a specific canonical plan revision.
2. Ideally approval should also be tied to a content hash.
3. Architectural risks need plan-aware references and reviewer hints.

Recommended additions to log state:

- `artifacts.plan.approvedRevision`
- `artifacts.plan.approvedHash`

Recommended additions to architectural risk entries:

- `planRefs`
- `connectedAreas`
- continue using `suggestedFix`

Rationale:

- approval must become stale when the canonical plan changes
- reviewer fix guidance belongs to review state, not to the plan itself

---

## 2. Main Decisions For The MVP

These decisions should be treated as locked for the first implementation iteration.

### Draft format

MVP should use:

- draft: strict JSON
- canonical plan: strict JSON

### Why not JSONC yet

I do **not** recommend JSONC draft support in the first iteration.

Reason:

- conversion and comment-stripping is extra complexity
- error reporting becomes harder
- the real unknown is whether the whole-draft workflow itself works for the Architect
- changing both authoring model and file format at once makes failure analysis harder

Conclusion:

- prove the concept first with JSON-to-JSON
- consider JSONC only in a later improvement iteration

### Number of canonical artifacts

- one canonical plan artifact only

Canonical artifact:

`artifacts/implementation-plans/<feature>.plan.json`

### Draft ownership rule

- the Architect never edits canonical `plan.json` directly
- the Architect edits one tool-created draft file
- the tool validates and accepts the draft into canonical `plan.json`

### Draft count rule

For MVP:

- one draft per feature
- no draft IDs
- no multiple concurrent drafts for the same feature

This keeps commands simpler and easier for the model to use.

Recommended draft location:

- outside repo artifacts
- example: `/tmp/flow-log-plan-drafts/<feature>.draft.json`

Reason:

- keeps the canonical workflow artifact singular inside the repo
- avoids accidental git noise
- reduces the chance that another agent mistakes the draft for an approved artifact

---

## 3. Supporting Files

You explicitly asked to avoid a spread of many support files.

For MVP, the supporting structure should stay minimal.

### Canonical workflow artifact

`artifacts/implementation-plans/<feature>.plan.json`

### One support file only

`flow-log/schema/plan-v3.schema.json`

Purpose of this single schema file:

- validation source for the tool
- readable structure guide for the Architect
- field descriptions and examples in one place

### Why schema is better than multiple templates in MVP

This is the cleanest compromise against context bloat.

Instead of:

- one schema file
- one template file
- one examples file

start with:

- one schema file only

That schema file should include:

- `description`
- `examples`
- required fields
- enum values
- structural rules

This gives the Architect one source to inspect in advance without introducing three separate reference files.

### Important limitation

JSON Schema is less friendly than a commented template for writing.

That is acceptable for MVP because:

- the draft itself is tool-generated from the correct shape
- the schema is mainly for understanding and validation
- if the concept works, a more ergonomic authoring template can be added later

---

## 4. Draft Workflow

This is the recommended end-to-end workflow for the Architect.

### First creation

1. TL creates flow-log state and locks requirements.
2. Architect runs `init-plan --feature <name>`.
3. Architect runs `plan-create-draft --feature <name>`.
4. Tool creates one draft file from the canonical skeleton in the tool-private draft location.
5. Architect edits the full draft in the IDE.
6. Architect runs `plan-validate-draft --feature <name>` repeatedly while drafting.
7. When ready, Architect runs `plan-accept-draft --feature <name>`.
8. Tool validates, rewrites canonical plan, increments revision if needed, and clears stale approval.
9. TL runs plan gate checks and approves the current revision.

### Review loop

1. Reviewer reads canonical plan.
2. Reviewer records risks in flow-log with `description`, `suggestedFix`, `planRefs`, and `connectedAreas`.
3. Architect runs `plan-create-draft --feature <name>`.
4. Tool creates or resumes a draft seeded from the current canonical plan.
5. Architect edits the full draft, keeping existing IDs stable where the concept still survives.
6. Architect validates the draft.
7. Architect accepts the draft.
8. Tool rewrites canonical plan and clears prior approval because the revision changed.
9. Architect responds to risks in flow-log with notes that mention the updated plan IDs.
10. Reviewer rereads the canonical plan and resolves or reopens risks.

---

## 5. Where Reviewer Hints Should Live

This should live in **flow-log**, not in the plan.

### Why it should not live in the plan

Reviewer hints are:

- review-state
- temporary
- external critique of the current design

They are not part of the design itself.

If reviewer hints go into the plan:

- the plan mixes design and critique
- revisions become harder to read
- stale reviewer commentary can remain after the design changes

### Recommended risk structure

Architectural risk entries in flow-log should evolve to include:

- `description`
- `severity`
- `suggestedFix`
- `planRefs`
- `connectedAreas`
- `responseNote`

### Semantics

`suggestedFix`

- the reviewer’s concrete recommendation for how the issue could be fixed

`planRefs`

- the primary IDs directly affected by the issue
- examples: `["S2"]`, `["F3", "D1"]`

`connectedAreas`

- additional IDs indirectly affected by the issue
- examples: `["M4", "C7", "EX2"]`

### Example

```json
{
  "id": 3,
  "severity": "HIGH",
  "description": "Slice S2 hides orchestration and integration work in one step, which makes constitution boundaries reviewable only after coding.",
  "suggestedFix": "Split the runtime path into separate flow steps for orchestration coordination and integration enrichment; keep one slice if you want, but make the layer boundary explicit in implementationFlow.",
  "planRefs": ["S2"],
  "connectedAreas": ["F3", "F4", "D1"]
}
```

This gives the Architect more than criticism. It gives a convergence target.

---

## 6. Canonical Plan Structure

The canonical plan should use this top-level shape in MVP:

```json
{
  "schemaVersion": "3.0",
  "feature": "search-enhancement-api",
  "revision": 1,
  "status": "draft",
  "scope": {},
  "implementationFlow": [],
  "contractExamples": [],
  "validationRules": [],
  "designDecisions": [],
  "models": [],
  "classes": [],
  "slices": [],
  "verification": {}
}
```

### Required meaning of each section

`scope`

- what the feature is
- what is in scope
- what is out of scope
- what constraints are locked

`implementationFlow`

- the path through layers and components
- what each layer does
- why that placement is correct under constitution rules

`contractExamples`

- representative request/success/error/validation-error examples
- only the branches needed to remove ambiguity

`validationRules`

- where each rule is enforced
- why the boundary placement is correct

`designDecisions`

- composition decisions
- shared infrastructure decisions
- rejected alternatives when needed

`models`

- exact model structures and purpose

`classes`

- exact file paths and roles

`slices`

- implementation units for the coder
- dependencies, coverage, tests, and done criteria

`verification`

- test matrix
- Karate expectations
- ArchUnit expectations
- final checks

---

## 7. Why `implementationFlow` Is Mandatory

This is the most important schema improvement.

The old/current plan style captures structure, but not the path of functionality through the system clearly enough.

The new plan must force the Architect to show:

- which layer is touched first
- which component in that layer changes
- what it produces for the next layer
- why that responsibility belongs there

### Minimum fields per flow step

Each `implementationFlow` item should contain:

- `id`
- `order`
- `slice`
- `layer`
- `component`
- `responsibility`
- `constitutionReason`
- `outputs`

### Why this matters

This is the missing bridge between:

- constitution rules
- class inventory
- coder slices

Without it, the plan still risks becoming an object list instead of an execution path.

---

## 8. Slices vs Batches

Your current flow uses coder batches of up to two slices. That should remain.

### Recommendation

- `slices` stay in the plan
- `batches` stay in flow-log

### Do not replace slices with batches

Reason:

- slices are design-time units owned by Architect
- batches are runtime dispatch units owned by TL

These are different concerns and should remain separate.

### Operating rule

- `1 slice = smallest coherent coder work unit`
- `1 batch = 1 or 2 slice IDs chosen by TL`

### Why this is the right compromise

- Architect keeps design sequence and dependency clarity
- TL keeps context control for the coder
- Coder still receives bounded work

---

## 9. Update Strategy After Review

This is the most important operational decision for the review/edit loop.

There should be **two update modes**, but not both in iteration 1.

### Iteration 1: targeted full-draft revision only

Supported in MVP:

- Architect always drafts from the current canonical plan
- Architect edits the whole draft
- Architect updates affected IDs in place
- Architect may add new IDs
- Architect should preserve old IDs for concepts that still exist

### Acceptance rule in iteration 1

`plan-accept-draft` must fail if:

- an OPEN or REOPENED architectural risk references an ID that disappeared from the draft

This keeps the review loop stable and prevents the Architect from accidentally deleting the objects the Reviewer is discussing.

### Practical effect

If the design changed but the concept still exists:

- keep the ID

If the design changed and a concept was split:

- keep the old ID on the dominant surviving concept in MVP
- add new IDs for new concepts

This is not perfect, but it keeps the first implementation tractable.

### Iteration 2: explicit redraft mode

Only after MVP proves stable, add a second mode:

- `plan-create-draft --feature <name> --mode redraft`

This mode allows larger reshaping of slices/classes/flow steps, but requires explicit replacement mapping.

Recommended mechanism:

- any retired ID must declare `replacedBy`
- or the draft must contain a top-level replacement map

Example:

```json
{
  "replacementMap": [
    {
      "oldId": "S2",
      "newIds": ["S2", "S5"],
      "reason": "Original slice split into mapper work and orchestration work"
    }
  ]
}
```

`plan-accept-draft` in redraft mode should then validate that:

- every open risk reference either still exists
- or is explicitly mapped forward

### Conclusion

For MVP:

- support only targeted full-draft revision

For later:

- add controlled redraft mode

This is the lowest-risk path.

---

## 10. Suggested Source Structure

Recommended implementation structure:

```text
flow-log/src/
  plan/
    schema.mjs
    store.mjs
    draft-store.mjs
    validate.mjs
    refs.mjs
    summary.mjs
    accept.mjs
    index.mjs
  plan-commands/
    canonical.mjs
    draft.mjs
    query.mjs
    index.mjs
  plan-state.mjs
  plan-commands.mjs

flow-log/schema/
  plan-v3.schema.json
```

### Module responsibilities

`plan/schema.mjs`

- initial canonical skeleton
- draft skeleton
- enums/constants
- helper accessors

`plan/store.mjs`

- canonical plan load/save
- resolve canonical plan path

`plan/draft-store.mjs`

- create draft from canonical
- resolve draft path
- load/save draft
- discard draft
- draft status

`plan/validate.mjs`

- shape validation
- readiness validation
- risk-ref stability checks

`plan/refs.mjs`

- collect all plan IDs
- cross-reference validation
- replacement-map validation later

`plan/summary.mjs`

- canonical summary
- maybe draft summary later

`plan/accept.mjs`

- accept draft into canonical
- revision bump
- approval invalidation rules

### Compatibility layer

For transition:

- keep `plan-state.mjs` and `plan-commands.mjs`
- make them compatibility exports/wrappers after the new modules are introduced

---

## 11. Command Set By Iteration

### Iteration 1 MVP

Keep existing read commands where possible and add the minimum new lifecycle.

Required commands:

- `init-plan --feature <name>`
- `plan-create-draft --feature <name>`
- `plan-validate-draft --feature <name>`
- `plan-accept-draft --feature <name>`
- `plan-discard-draft --feature <name>`
- `plan-draft-status --feature <name>`
- existing `plan-summary --feature <name>`
- existing `plan-get --feature <name>`

### Iteration 2

Add stronger review/revision tooling:

- `plan-diff --feature <name>`
- `plan-create-draft --feature <name> --mode redraft`

### Iteration 3

Only if the concept proves valuable:

- optional JSONC draft authoring
- section repair helpers
- richer replacement-map tooling

---

## 12. Approval and Review Gate Behavior

This needs to become explicit.

### On draft accept

If canonical content changed:

- increment `revision`
- recompute hash
- mark plan artifact approval as stale in flow-log

### On TL plan approval

Flow-log should store:

- approved revision
- approved hash

### During architecture review loop

Reviewer reads canonical plan only.

Architect revises via draft only.

After Architect accepts a revised draft:

- TL must rerun plan validation
- TL must approve the new revision again before moving forward

This closes a gap in the current behavior.

---

## 13. Iteration Plan

## Iteration 1: MVP Whole-Draft Workflow

### Goal

Prove that the Architect can create and revise a full plan through a draft without editing canonical `plan.json` directly.

### Deliverables

- new canonical schema shape with `implementationFlow`
- one schema file: `flow-log/schema/plan-v3.schema.json`
- one draft per feature, strict JSON
- draft lifecycle commands
- shape + readiness validation
- approval invalidation on accept
- risk fields: `planRefs`, `connectedAreas`, keep `suggestedFix`
- targeted full-draft revision only

### Explicitly not included

- JSONC draft authoring
- multiple drafts
- redraft mode with replacement mapping
- section-level draft editing

### Success criteria

- Architect can create first draft without huge shell JSON commands
- Architect can revise after review without manual canonical edits
- Reviewer can point to stable IDs and give concrete fix hints
- TL can tell exactly which revision is approved

## Iteration 2: Review-Aware Revision Hardening

### Goal

Make the review/edit loop more robust once MVP proves usable.

### Deliverables

- `plan-diff`
- stronger risk-ref validation
- better diagnostics when draft changes affect open-risk references
- better summary output showing current revision vs approved revision/hash

### Success criteria

- review loops become more precise
- fewer “plan changed but review context is lost” failures

## Iteration 3: Controlled Redraft Mode

### Goal

Support major architecture changes without losing reviewer traceability.

### Deliverables

- explicit redraft mode
- replacement map support
- validation for retired/replaced IDs

### Success criteria

- whole-flow redesigns are possible without manual review-state cleanup

## Iteration 4: Authoring Ergonomics Improvements

### Goal

Improve usability only after the core workflow is proven.

### Deliverables

- optional JSONC draft support
- richer authoring aids if still needed

### Success criteria

- lower authoring friction without destabilizing canonical validation

---

## 14. Recommendation

Implement this in order:

1. treat current log split as sufficient baseline
2. build MVP with strict JSON draft and strict JSON canonical
3. use one schema file only for validation + structure guidance
4. keep reviewer hints in flow-log, not in the plan
5. support targeted whole-draft revision first
6. add full redraft mode only after the basic loop works reliably

This is the smallest plan feature that tests the real concept without burying the team in format-conversion or authoring-tool complexity.
