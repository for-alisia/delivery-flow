# Plan Management (v3)

The canonical plan artifact is:

`artifacts/implementation-plans/<feature>.plan.json`

Plans use schema version `3.0` and are tool-owned — the Architect edits a draft, then flow-log validates and accepts it into the canonical file.

## Draft Lifecycle

```bash
# 1. Create canonical v3 skeleton
node flow-log/flow-log.mjs init-plan --feature my-feature

# 2. Create or resume tool-managed draft
node flow-log/flow-log.mjs plan-create-draft --feature my-feature

# 3. Edit the draft directly at /tmp/flow-log-plan-drafts/<feature>.draft.json
#    Use read_file and edit tools. The draft is a plain JSON file.

# 4. Validate draft (shape + readiness + risk link stability)
node flow-log/flow-log.mjs plan-validate-draft --feature my-feature

# 5. Accept validated draft into canonical plan
node flow-log/flow-log.mjs plan-accept-draft --feature my-feature
```

### Draft path

`/tmp/flow-log-plan-drafts/<feature>.draft.json`

### Command behavior

**`plan-create-draft`:**
- Creates a new draft from canonical when no draft exists
- Returns the existing draft when it already matches canonical
- Refuses to silently reuse a draft that has unapplied changes; inspect it with `plan-draft-status` or reset it with `plan-discard-draft`

**`plan-accept-draft`:**
- Validates before write
- Exits non-zero if validation fails
- Returns `changed: false` on no-op
- Bumps canonical `revision` and recomputes `hash` on change
- Invalidates flow-log plan approval metadata (`approved`, `approvedRevision`, `approvedHash`) when canonical content changes

### Draft inspection and cleanup

```bash
node flow-log/flow-log.mjs plan-draft-status --feature my-feature
node flow-log/flow-log.mjs plan-discard-draft --feature my-feature
```

## Plan Schema (v3.0)

Top-level sections of a v3 plan JSON:

| Section | Type | Purpose |
|---------|------|---------|
| `scope` | object | `purpose`, `inScope[]`, `outOfScope[]`, `constraints[]` |
| `implementationFlow` | array | Ordered flow steps with IDs (`F1`, `F2`, …) describing the execution sequence |
| `contractExamples` | array | Payload examples: type (`request`, `success`, `error`, `validation-error`), `description`, `json` |
| `validationRules` | array | Each with `id`, `rule`, `location` |
| `designDecisions` | array | Each with `id`, `decision`, `rationale` |
| `models` | array | Each with `id`, `name`, `kind` (record/enum/interface/sealed-interface/class), `status`, `package`, `justification`, `fields[]` |
| `classes` | array | Each with `id`, `path`, `status`, `role` |
| `slices` | array | Each with `id`, `goal`, `files[]`, tests, logging |
| `verification` | object | `slices[]` (per-slice test expectations), `finalGates[]` |

### Model kinds

`record`, `enum`, `interface`, `sealed-interface`, `class`

### Validation

```bash
# Validate canonical plan
node flow-log/flow-log.mjs validate-plan --feature my-feature

# Validate draft before accepting
node flow-log/flow-log.mjs plan-validate-draft --feature my-feature
```

Validation checks:
- All required top-level sections present
- Schema version is `3.0`
- Models have non-empty `justification`
- Slices reference valid model/class IDs
- Implementation flow references valid IDs
- Cross-references are consistent

## Reading Plans (all agents)

```bash
# Full plan
node flow-log/flow-log.mjs plan-get --feature my-feature

# Specific section
node flow-log/flow-log.mjs plan-get --feature my-feature --section slices
node flow-log/flow-log.mjs plan-get --feature my-feature --section models

# Plan summary (counts and approval status)
node flow-log/flow-log.mjs plan-summary --feature my-feature
```

## Plan Revision (after architecture review)

When addressing architecture review risks:

```bash
# 1. Create a draft from the current canonical plan
node flow-log/flow-log.mjs plan-create-draft --feature my-feature

# 2. Edit the draft JSON directly — rewrite affected sections from scratch
#    Do not patch; remove orphaned mechanisms, stale references, and dual-path ambiguity

# 3. Validate and accept
node flow-log/flow-log.mjs plan-validate-draft --feature my-feature
node flow-log/flow-log.mjs plan-accept-draft --feature my-feature
```

After acceptance, flow-log invalidates prior plan approval until TL re-approves.

## Reviewer Risk References

`add-risk` supports plan-aware references:

```bash
node flow-log/flow-log.mjs add-risk \
  --feature my-feature \
  --severity HIGH \
  --description "Flow step ownership is unclear" \
  --plan-ref F3 \
  --connected-area S2 \
  --connected-area M4 \
  --suggested-fix "Split flow step and rebind slice ownership"
```

Fields: `planRefs` (primary affected plan IDs), `connectedAreas` (secondary related IDs), `suggestedFix` (reviewer-proposed remediation).

Risk link enforcement during validation and acceptance applies to `OPEN`, `REOPENED`, and `ADDRESSED` risks.

## Legacy v2 Commands

Legacy `add-plan-*`, `set-plan-*`, and `revise-plan` commands remain available for existing `schemaVersion: "2.0"` plans. They are marked as legacy in CLI help and should not be used for new plans.
