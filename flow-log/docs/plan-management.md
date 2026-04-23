# Plan Management (v4)

The canonical implementation plan artifact is:

`artifacts/implementation-plans/<feature>.plan.json`

Plans use a single schema version: `4.0`.

## Draft lifecycle

```bash
scripts/flow-log.sh plan-init-draft --feature my-feature
# edit /tmp/flow-log-plan-drafts/<feature>.draft.json
scripts/flow-log.sh plan-validate-draft --feature my-feature
scripts/flow-log.sh plan-accept-draft --feature my-feature
```

Use [artifacts/templates/plan-v4.example.json](../../artifacts/templates/plan-v4.example.json) as the canonical field-level example while authoring the draft.

### Draft behavior

- `plan-create-draft` creates a draft from the canonical plan when none exists.
- If the current draft already matches the canonical plan, the command returns the existing draft path.
- If the current draft has unapplied changes, the command fails. Inspect it with `plan-draft-status` or reset it with `plan-discard-draft`.
- `plan-accept-draft` validates before write and invalidates plan approval metadata when canonical content changes.

## Plan shape

A v4 plan is slice-first and keeps contract payload ownership in the story `External Contracts` section.

Top-level sections:

- `scope`
- `sharedRules`
- `sharedDecisions`
- `slices`
- `finalVerification`

Required top-level fields:

- `schemaVersion`
- `feature`
- `revision`
- `status`
- `scope`
- `slices`
- `finalVerification`

## Slice shape

Each slice is the primary delivery and review unit.

Required slice fields:

- `id`
- `title`
- `goal`
- `dependsOn`
- `units`
- `doneWhen`

Optional slice fields:

- `readsExisting`
- `sliceRules`
- `contractDependency`
- `compositionNotes`

## Unit shape

Each unit identifies one owned implementation artifact.

Required unit fields:

- `id`
- `kind`
- `locationHint`
- `status`
- `purpose`
- `change`
- `tests`

Optional unit fields:

- `contractDetails`
- `loggingNotes`

Supported `kind` values:

- `java-class`
- `karate-feature`
- `archunit-test`
- `config`

## Validation

```bash
scripts/flow-log.sh validate-plan --feature my-feature
scripts/flow-log.sh plan-validate-draft --feature my-feature
```

Validation checks cover:

- required top-level and slice / unit fields
- duplicate IDs
- cross-reference integrity for shared rules, shared decisions, and slice dependencies
- slice readiness (`units`, `doneWhen`, test expectations)
- stable risk references for OPEN, REOPENED, and ADDRESSED architecture risks

## Reading plans

Default to slice-scoped reads whenever possible.

```bash
scripts/flow-log.sh plan-summary --feature my-feature
scripts/flow-log.sh plan-get --feature my-feature --slice S1
scripts/flow-log.sh plan-get --feature my-feature --section sharedRules
```

`plan-get` requires an explicit target. Use `--slice` for implementation intake and `--section` for shared context. `plan-summary` returns slice metadata including slice IDs, titles, dependency IDs, and unit counts.

## Story contract retrieval

External payload contracts stay in the story, not in the plan.

```bash
scripts/flow-log.sh story-get --feature my-feature --section external-contracts
```

Use this when a slice depends on request/response or upstream API details. Keep compact request / response / error examples in the story when field names, nullability, omitted-body behavior, or error body shape are easy to misread. If the story changed after approval, Team Lead must re-approve it before `story-get` will return content.

## Revision rules

Architecture review findings must reference stable IDs:

- slice-level: `S1`
- unit-level: `S1-U2`
- shared rule / decision: `R1`, `D1`

When revising a plan:

- keep unchanged slice IDs stable
- keep unchanged unit IDs stable
- update story `External Contracts` only when the contract understanding changed
- if story `External Contracts` changed, Team Lead must re-register and re-approve the story before approving the plan again
