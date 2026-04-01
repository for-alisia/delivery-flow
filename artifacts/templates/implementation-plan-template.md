# Implementation Plan: <title>

**Artifact path:** `artifacts/implementation-plans/<feature-name>.plan.md`
**Task name:** `<task-name>`
**Plan date:** `<yyyy-mm-dd>`
**Status:** `Draft`

## Ownership

- Architect owns this plan only.
- Coder writes `artifacts/implementation-reports/<feature-name>.report.json`.
- Reviewer writes `artifacts/review-reports/<feature-name>.review.json`.
- Team Lead writes `artifacts/implementation-signoffs/<feature-name>.signoff.json`.

## Business Goal

Precise paragraph describing the business or technical outcome.

## Requirement Lock / Source Of Truth

- Original request source:
- Non-negotiable input or source-of-truth constraints:
- Non-negotiable contract or payload constraints:
- Non-negotiable configuration or external-system constraints:
- Explicit assumptions or unresolved items:

## Payload Examples

Provide concrete examples when the change affects request/response or error contracts. Use `N/A` only when no payload contract is involved.

### Request Example

```json
{
  "<field>": "<value>"
}
```

### Success Response Example

```json
{
  "<field>": "<value>"
}
```

### Error Response Example

```json
{
  "message": "<error message>",
  "status": 500
}
```

### Validation Error Response Example

```json
{
  "message": "<validation error summary>",
  "status": 400,
  "errors": [
    {
      "field": "<field>",
      "message": "<reason>"
    }
  ]
}
```

## Validation Boundary Decision

| Validation Rule | Boundary | Why Here |
|---|---|---|
| `<rule>` | `DTO binding / use case / domain` | `<reason>` |

## Scope

### In Scope

- 

### Out of Scope

- 

## Class Structure

### Affected Classes

| Class Path | Status | Proposed Behavior |
|---|---|---|

## Implementation Slices

Repeat the slice structure below for as many slices as needed. Keep each slice precise and unambiguous.

### Slice N - <short title>

- Goal:
- Affected scope:
- Payload / contract impact:
- Validation boundary decisions:
- Unit tests:
- Integration / Web tests:
- Edge / failure coverage:
- INFO logging:
- WARN logging:
- ERROR logging:
- Documentation updates:

## Testing Matrix

| Level | Required | Planned Coverage | Evidence Target |
|---|---|---|---|

## Acceptance Criteria

- 

## Final Verification

- `scripts/verify-quick.sh`
- Local static analysis via `scripts/quality-check.sh` for `flow-orchestrator` when production code changes
- Repository-supported startup verification
- Required `curl` smoke checks

## Risks / Notes

- 

## Linked Handoff Artifacts

- Implementation report target: `artifacts/implementation-reports/<feature-name>.report.json`
- Review report target: `artifacts/review-reports/<feature-name>.review.json`
- Sign-off target: `artifacts/implementation-signoffs/<feature-name>.signoff.json`
