# Implementation Plan: <title>

**Artifact path:** `artifacts/implementation-plans/<feature-name>.plan.md`
**Task name:** `<task-name>`
**Plan date:** `<yyyy-mm-dd>`
**Status:** `Draft`

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

```json
// Request
{ "<field>": "<value>" }

// Success Response
{ "<field>": "<value>" }

// Error Response
{ "message": "<error message>", "status": 500 }

// Validation Error Response
{ "message": "<validation error summary>", "status": 400, "errors": [{ "field": "<field>", "message": "<reason>" }] }
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
- Required `curl` smoke checks via `scripts/smoke-test.sh`
