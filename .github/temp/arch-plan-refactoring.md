# Architect Plan Refactoring Direction

## Purpose

Define the **next iteration of the plan system** so it becomes usable for `Java Architect`, readable for `Java Coder`, and traceable for `Architecture Reviewer`, while keeping **one canonical artifact only**.

This is intentionally a **high-level design/refactoring plan**, not a coding task breakdown.

---

## 1. Non-Negotiable Constraints

These constraints are treated as hard requirements for the redesign.

### Single artifact only

- There must be exactly **one canonical plan artifact** for the workflow.
- No Markdown shadow plan.
- No second “human-readable” plan artifact.
- No dual-source setup where one file is authoritative and another is “for readability.”

Canonical artifact:

`artifacts/implementation-plans/<feature>.plan.json`

### Canonical plan must stay tool-owned

- Agents must **not** manually edit the canonical `plan.json`.
- All writes to the canonical plan must go through `flow-log` commands.
- Review/revision loops must not mix direct file edits with tool mutations on the same canonical file.

### Tool must absorb formatting burden

- Architect should not need to compose long JSON blobs in terminal flags.
- Architect should not need to fight shell quoting for nested arrays/objects.
- Tooling must make structured authoring easy and validation strict.

### Plan must show implementation flow clearly

- The current plan shape captures structure, but the main implementation path is still too implicit.
- The next plan must make it obvious:
  - what gets implemented first
  - what depends on what
  - what each coder handoff unit contains
  - what reviewer should validate against

---

## 2. Core Recommendation

Keep one canonical JSON artifact, but change the authoring model from:

- many tiny shell mutation commands

to:

- **tool-managed whole-draft authoring**
- **atomic draft acceptance into canonical plan**
- **stable IDs everywhere**
- **strict cross-reference validation**

This keeps the plan machine-owned while removing the shell-formatting pain from the Architect.

---

## 3. Authoring Model

### Recommended model

The Architect never edits `plan.json` directly.

Instead:

1. Tool initializes the canonical plan skeleton.
2. Tool creates **one tool-private full-plan draft**.
3. Architect edits that draft in the IDE as a whole.
4. Architect can validate the draft repeatedly while working.
5. When ready, Architect calls an accept command.
6. Tool validates and atomically writes canonical `plan.json`.
7. Canonical plan remains untouched if validation fails.

### Why whole-draft is now the default

This is a change from the previous proposal.

I agree that for architecture work, whole-draft authoring is the better default because:

- the model reasons about tradeoffs globally, not section by section
- slices, decisions, models, and verification affect each other continuously
- forcing section commits too early can lock in local consistency while global flow is still weak
- a full draft makes it easier for the Architect to revisit the whole approach before review

### Important tradeoff

Whole-draft authoring has a larger validation surface.

That is acceptable if the tool provides:

- `validate-draft` while the draft is still in progress
- clear validation errors with field paths and IDs
- resumable drafts
- atomic acceptance so the canonical plan is never half-updated

### Secondary fallback, not default

Section-level draft editing can still exist later as a recovery or repair tool for very large plans, but it should not be the main authoring path.

### Why this fits your constraint

This still keeps **one workflow artifact only**.

- The canonical artifact is the only persisted workflow artifact.
- The draft buffer is not part of the workflow contract.
- It is a temporary tool-owned working buffer, not something other agents read, approve, or reference.

This avoids the “2 plan artifacts” failure mode while still removing shell JSON complexity.

### Important distinction

Rejected approach:

- canonical `plan.json` + canonical `plan.md`

Accepted approach:

- canonical `plan.json`
- temporary tool-private full draft file or temp buffer used only during authoring

---

## 4. Proposed Tool Workflow

### Initialization

`init-plan --feature <name>`

Creates canonical plan skeleton only.

### Create or resume full draft

Conceptual command:

`plan-create-draft --feature <name>`

Tool behavior:

- creates a temporary draft file in tool-controlled temp storage
- pre-populates it with:
  - the current canonical plan content
  - commented schema guidance
  - minimal valid examples
- returns:
  - `draftId`
  - temp path
  - short instructions

If an open draft already exists:

- return that draft instead of creating a second one
- make the draft explicitly resumable

### Draft format

Recommended draft format:

- YAML or JSONC, not strict JSON

Why:

- comments are needed
- punctuation burden is lower
- the Architect can read and update it more safely in the editor

Canonical output remains strict JSON.

### Architect edits draft

Architect edits only the temp draft file, not the canonical plan.

### Validate draft

Conceptual command:

`plan-validate-draft --feature <name> --draft-id <id>`

Tool behavior:

- validates syntax of the draft format
- validates schema
- validates cross-references
- validates readiness rules
- returns actionable errors without touching canonical plan

### Accept draft

Conceptual command:

`plan-accept-draft --feature <name> --draft-id <id>`

Tool behavior:

- validates syntax
- validates schema
- validates cross-references against the current plan
- validates readiness rules
- atomically rewrites canonical `plan.json`
- bumps revision metadata if needed
- invalidates prior plan approval in flow-log if canonical content changed

### Abort draft

Conceptual command:

`plan-discard-draft --draft-id <id>`

### Optional quality-of-life command

Conceptual command:

`plan-show-draft-status --feature <name>`

Returns:

- whether a draft exists
- draft path
- last validated timestamp
- current validation status
- whether canonical approval would be invalidated on accept

---

## 5. Why This Is Better Than Large Terminal JSON

This directly addresses the problem you called out:

- no huge inline JSON in shell flags
- no quoting/escaping pain
- no partial terminal command corruption
- no manual edits to the canonical plan
- no ambiguous “sometimes tool, sometimes editor” ownership of the real file
- no need to prematurely freeze architecture into section commits

The model burden becomes:

- work on one coherent draft in the editor
- validate while thinking
- accept only when the whole plan is ready

instead of:

- craft large nested JSON inside terminal commands

---

## 6. Proposed Canonical Plan Structure

The next plan should be organized around **stable implementation units** first, with structure and validation attached to them.

Recommended top-level structure:

```jsonc
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

### Why this shape

- `scope` keeps the requirement lock visible inside the plan
- `implementationFlow` makes the code path explicit across layers and capability boundaries
- `contractExamples` and `validationRules` make contract behavior explicit
- `designDecisions` makes reasoning explicit
- `models` and `classes` keep structural inventory
- `slices` become the primary execution spine
- `verification` groups tests/Karate/ArchUnit/final checks

---

## 7. Field-by-Field Design With Purpose

Below is the recommended schema direction with comments beside each field.

```jsonc
{
  "schemaVersion": "3.0", // Tool schema version. Used for migrations and validation rules.
  "feature": "search-enhancement-api", // Feature key. Must match flow-log feature.
  "revision": 3, // Monotonic plan revision. Bumped whenever canonical content changes materially.
  "status": "draft", // draft | review-ready. Tool-managed; not free text.

  "scope": {
    "summary": "Add optional label audit enrichment to issue search", // One-sentence intent of the plan.
    "inScope": [
      "Support filters.audit=[label] on issue search"
    ], // Exact work this plan covers. Keep concrete and short.
    "outOfScope": [
      "Milestone audit",
      "Changing single issue endpoint contract"
    ], // Explicit exclusions so Architect/Coder/Reviewer stop inventing scope.
    "lockedConstraints": [
      "Reuse existing changeSets response shape",
      "Keep page size validation at existing runtime guard"
    ] // Constraints copied from story/request that must remain visible during revisions.
  },

  "implementationFlow": [
    {
      "id": "F1", // Stable flow-step ID for reviewer references and draft validation.
      "order": 1, // Execution order in the intended implementation/runtime path.
      "slice": "S1", // Which slice owns this step.
      "layer": "rest", // Which architectural layer is touched: rest | orchestration | integration | config | common.
      "component": "IssueFiltersRequest + IssuesRequestMapper", // Concrete class or component area touched in this step.
      "responsibility": "Accept audit input and normalize it into orchestration input", // What this layer does here.
      "constitutionReason": "Transport parsing and normalization stay at the boundary before orchestration logic", // Why this placement respects architecture rules.
      "outputs": [
        "SearchIssuesInput"
      ] // What this step produces for the next step in the flow.
    },
    {
      "id": "F2",
      "order": 2,
      "slice": "S1",
      "layer": "orchestration",
      "component": "IssuesService",
      "responsibility": "Execute base search and coordinate optional audit enrichment",
      "constitutionReason": "Cross-provider workflow composition belongs in orchestration service layer",
      "outputs": [
        "List<Issue>"
      ]
    }
  ],

  "contractExamples": [
    {
      "id": "EX1", // Stable reference ID for reviewer findings and slice links.
      "kind": "request", // request | success | error | validation-error.
      "title": "Search request with label audit", // Human-readable short label.
      "purpose": "Shows how audit is requested on the search endpoint", // Why this example exists; what ambiguity it removes.
      "appliesToSlices": ["S1"], // Which slice(s) implement or depend on this contract.
      "payload": {} // Minimal but complete JSON shape. Keep realistic; do not dump huge payloads.
    }
  ],

  "validationRules": [
    {
      "id": "VR1", // Stable reference ID.
      "rule": "filters.audit accepts only supported values [label]", // The actual constraint.
      "owner": "IssuesRequestMapper", // Where this validation/normalization lives.
      "reason": "Transport values must be normalized before orchestration logic", // Why this location is correct.
      "appliesToSlices": ["S1"] // Traceability to execution work.
    }
  ],

  "designDecisions": [
    {
      "id": "D1", // Stable decision ID used by reviewer and later revisions.
      "title": "Composition stays dependent-then-parallel", // Short decision name.
      "decision": "Run base issue search first, then parallel label-event enrichment", // Final chosen approach.
      "reason": "Enrichment depends on issue IDs from the first call", // Why this is the correct choice.
      "alternativesRejected": [
        "Fully parallel search and enrichment",
        "Sequential enrichment per issue"
      ], // Helpful for review convergence; keeps the rejected simpler/other paths visible.
      "appliesToSlices": ["S1"] // Which implementation units depend on this decision.
    }
  ],

  "models": [
    {
      "id": "M1", // Stable model ID.
      "qualifiedName": "com.gitlabflow...Issue", // Exact type to create/modify.
      "kind": "record", // record | enum | interface | sealed-interface.
      "status": "modified", // new | modified | existing.
      "purpose": "Search result entity reused by search and create flows", // Why the model exists in this design.
      "placementJustification": "Stays in orchestration model per constitution boundary", // Why this package/layer is correct.
      "ownedBySlices": ["S1"], // Which slice is responsible for this model change.
      "fields": [
        {
          "name": "changeSets",
          "type": "List<ChangeSet>",
          "nullable": true,
          "defensiveCopy": true,
          "purpose": "Present only when audit enrichment was requested" // What this field means; removes coder guesswork.
        }
      ],
      "notes": [
        "Keep null distinct from empty list"
      ] // Rare edge-case notes only. Avoid prose dump.
    }
  ],

  "classes": [
    {
      "id": "C1", // Stable class ID.
      "path": "src/main/java/.../IssuesService.java", // Exact file path.
      "status": "modified", // new | modified | existing.
      "role": "Coordinates search and optional audit enrichment", // Why this class is touched.
      "ownedBySlices": ["S1"] // Which slices touch this file.
    }
  ],

  "slices": [
    {
      "id": "S1", // Stable execution unit. This is the main handoff ID for TL/Coder/Reviewer.
      "title": "Normalize request and map audit input", // Short execution title.
      "goal": "Create stable orchestration input from search request", // What must be achieved in this slice.
      "dependsOn": [], // Slice IDs that must land first. Empty means this can start first.
      "reasonForSplit": "Keeps request normalization separate from enrichment logic", // Why this is its own slice.
      "flowSteps": ["F1", "F2"], // Ordered flow steps implemented or completed by this slice.
      "covers": {
        "models": ["M1"], // Model IDs implemented here.
        "classes": ["C1"], // Class IDs implemented here.
        "validationRules": ["VR1"], // Validation rules realized here.
        "examples": ["EX1"], // Contract examples this slice must satisfy.
        "decisions": ["D1"] // Decisions this slice depends on.
      },
      "implementationTasks": [
        "Add audit field to request DTO",
        "Normalize audit values in mapper",
        "Build SearchIssuesInput with deduplicated audit types"
      ], // Concrete coder tasks; short and imperative.
      "tests": {
        "unit": [
          "IssuesRequestMapperTest: null and empty audit normalize to no audit"
        ],
        "integration": [],
        "component": []
      }, // Test obligations for this slice only.
      "logging": {
        "info": "None",
        "warn": "None",
        "error": "None"
      }, // Explicit even when none. Removes reviewer ambiguity.
      "doneWhen": [
        "Unsupported audit values produce validation error",
        "SearchIssuesInput contains deduplicated audit types"
      ] // Slice-level acceptance checklist.
    }
  ],

  "verification": {
    "testMatrix": [
      {
        "id": "T1", // Stable verification item ID.
        "level": "unit", // unit | integration | component | karate | archunit.
        "scope": "Mapper normalization",
        "coversSlices": ["S1"],
        "required": true
      }
    ],
    "karate": {
      "required": true, // Whether endpoint contract changed enough to require Karate updates.
      "featureFile": "src/test/karate/resources/issues/search-audit.feature",
      "scenarios": [
        "Search with label audit",
        "Search without audit"
      ]
    },
    "archUnit": {
      "existingRulesReviewed": true,
      "newRules": []
    },
    "finalChecks": [
      "verify-quick per coder batch",
      "final-check before handoff",
      "karate by Team Lead after final batch"
    ] // Final gate expectations; short operational checklist.
  }
}
```

---

## 8. What `contractExamples` Means

You asked specifically what “examples” should be.

Recommended interpretation:

- `contractExamples` are **representative contract shapes**
- they are not free-form notes
- they are not implementation examples
- they are not “everything the endpoint can do”

They should be used only to lock down the branches that would otherwise create coder/reviewer ambiguity:

- request variants
- success response variants
- error shape
- validation error shape

Keep them:

- minimal
- realistic
- directly tied to slices and validation rules

Do not keep:

- giant full payload dumps
- prose explanations duplicated from `scope` or `designDecisions`
- examples unrelated to real ambiguity

---

## 9. Where The Flow Of Code Fits

This is the major schema change compared to the earlier proposal.

The plan should not only say:

- which models/classes/slices exist

It should also say:

- which layers are touched
- in what order they participate
- what responsibility each layer step owns
- why that placement is constitutionally correct

That is why `implementationFlow` is now a top-level required section.

### What `implementationFlow` should capture

Each flow step should answer all of these:

- which slice owns this step
- which layer is being touched
- which concrete component(s) are involved
- what functionality is introduced there
- what output moves to the next step
- why this placement is correct under constitution/architecture rules

### Why this helps the agents

- Architect is forced to think in terms of layer responsibility, not only objects
- Coder sees the intended path through the codebase before coding
- Reviewer can challenge wrong layer placement directly
- TL can understand whether slices are coherent or arbitrarily split

### Practical rule

If a plan has models, classes, and slices but cannot explain the path through layers, it is not review-ready.

---

## 10. Where Slices Fit

Slices are the primary implementation spine and must stay in the plan.

### Recommendation

- Keep `slices` in the plan
- Keep `batches` in the log
- Do not move `batches` into the plan
- Do not replace slices with batches

### Why both should exist

`slices`

- design-time implementation units
- written by Architect
- stable IDs used by Coder and Reviewer
- express dependency order and scope

`batches`

- runtime execution groupings
- owned by Team Lead in flow-log
- record how TL actually sent work to Coder
- useful for retries, red cards, and verification history

### Recommended operating rule

- **1 slice = smallest coherent coder work unit**
- **1 batch = 1 or 2 slice IDs selected by TL**

This matches your current operating model and keeps context manageable.

For larger stories:

- Architect creates more slices
- TL still sends max 2 slices per coder invocation

That gives you both:

- a clear design sequence
- bounded coder context

---

## 11. Revision and Review Model

### Revision rule

When Architect changes the plan materially:

- canonical plan `revision` increments
- flow-log plan approval becomes stale
- TL must revalidate and reapprove the new revision

### Reviewer findings

Reviewer findings should stay in flow-log, but reference plan IDs.

Recommended additions to risk shape:

- `planRefs`: array of IDs like `["S2", "D1", "M1"]`
- `connectedAreas`: additional linked IDs affected by the issue

Example meaning:

- “HIGH risk on `S1` because it violates decision `D1`; also affects `M1` and `EX1`”

This makes review loops much more precise than today’s paragraph-only references.

---

## 12. Validation Model

Validation should be split into levels.

### Shape validation

Purpose:

- ensure plan is structurally parseable and internally consistent

Checks:

- required top-level sections exist
- IDs are unique
- references resolve
- `dependsOn` references existing slices
- `flowSteps` reference existing implementation flow IDs
- every `implementationFlow` step references an existing slice
- no duplicate class/model IDs
- required fields per item kind exist

### Readiness validation

Purpose:

- ensure plan is good enough for coding handoff

Checks:

- at least one slice exists
- every modified/new model belongs to at least one slice
- every modified/new class belongs to at least one slice
- every slice references at least one `implementationFlow` step
- `implementationFlow.order` is contiguous and unambiguous
- every flow step declares a `layer`, `component`, `responsibility`, and `constitutionReason`
- every slice has `goal`, `implementationTasks`, `tests`, `doneWhen`
- every contract-changing slice references at least one `contractExample`
- every validation-sensitive slice references at least one `validationRule`
- composition-sensitive work must reference a `designDecision`
- verification section must cover all slices

### Review traceability validation

Purpose:

- ensure reviewer can point to precise plan objects

Checks:

- every slice, model, class, example, rule, and decision has a stable ID
- every implementation flow step has a stable ID
- no renumbering/relabeling of IDs during revision unless item is truly deleted

---

## 13. How Architect Learns the Schema

The tool should provide schema guidance explicitly, not expect the model to infer it from old plans.

Recommended support package:

### A machine-readable schema

Store:

`flow-log/schema/plan-v3.schema.json`

Purpose:

- tool validation
- future migrations
- deterministic shape checking

### Commented full-draft template

Store:

```text
flow-log/schema/templates/
  plan-draft.template.yaml
  plan-draft.minimal.yaml
  plan-draft.medium.yaml
```

Purpose:

- Architect gets one complete editable draft with inline comments
- the whole architecture can be revised coherently in one place
- easier than learning a full nested JSON artifact from memory

### Golden examples

Store:

```text
flow-log/schema/examples/
  minimal-plan.example.yaml
  medium-plan.example.yaml
```

Purpose:

- show what a valid completed section looks like
- show what a valid complete plan looks like
- reduce “trash plan” output caused by missing implicit expectations

### CLI explain command

Conceptual command:

`plan-explain --section slices`

Returns:

- section purpose
- required fields
- one valid miniature example
- common mistakes

This is much better than expecting the agent prompt to carry all schema knowledge.

---

## 14. Recommended Command Direction

The current very granular `add-plan-*` approach should not be the main authoring path anymore.

Recommended command families:

- `init-plan`
- `plan-summary`
- `plan-get`
- `plan-create-draft`
- `plan-validate-draft`
- `plan-accept-draft`
- `plan-discard-draft`
- `plan-show-draft-status`
- `plan-start-revision`

Possible later convenience commands:

- `plan-list-ids`
- `plan-explain --section <name>`
- `plan-diff --feature <name> --revision <n>`

### Commands to de-emphasize

The current fine-grained mutation commands can stay temporarily for compatibility, but should be treated as legacy for authoring:

- `add-plan-model`
- `add-plan-model-field`
- `add-plan-class`
- `add-plan-slice`
- etc.

They are too low-level for the real authoring burden.

---

## 15. Final Recommendation

Use this direction for the next plan iteration:

- keep one canonical plan artifact only
- keep canonical `plan.json` fully tool-owned
- author via one tool-managed whole draft, not shell JSON flags
- accept the whole draft atomically into canonical plan
- require a dedicated `implementationFlow` section so layer responsibilities are explicit
- make slices the primary implementation spine
- keep batches only in flow-log runtime state
- add stable IDs everywhere for reviewer traceability
- add schema/templates/examples so Architect learns the shape from tooling, not memory

This keeps the system strict, single-source, and reviewable without forcing the Architect to handcraft large JSON commands in the terminal.
