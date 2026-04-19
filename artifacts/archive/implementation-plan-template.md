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

### Model Definitions

Define all new or changed models completely. For each record/interface/enum:

- Full field list with types and nullability
- Interface contracts with all accessor methods
- Sealed hierarchy with permitted implementations
- Enum values
- `@Builder` annotation
- Defensive copy fields

| Model | Type | Fields / Contract | Notes |
|---|---|---|---|

### Affected Classes

| Class Path | Status | Proposed Behavior |
|---|---|---|

## Composition Strategy

When the feature involves multiple port calls in a single service method, state the execution approach:

- **Independent calls → parallel** (default). List the calls and confirm no data dependency.
- **Dependent calls → sequential**. State the dependency that requires ordering.

## Shared Infrastructure Impact

- Reused shared mechanisms:
- New shared extractions (if any):
- See `documentation/context-map.md` → "Shared Infrastructure" for current inventory.

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

### Karate checklist (required when plan adds or changes API endpoints)

- [ ] `.feature` file defined under `src/test/karate/resources/<capability>/`
- [ ] Scenario names, HTTP methods, endpoint paths, expected status codes, and key response assertions specified
- [ ] Smoke scenarios tagged with `@smoke`
- [ ] Karate runner updated if a new capability is introduced

### ArchUnit checklist (required when plan introduces new layer interactions or package boundaries)

- [ ] New ArchUnit rule defined in Testing Matrix for each new boundary
- [ ] Existing rules in `FlowOrchestratorArchitectureTest.java` reviewed — plan decisions comply

## Acceptance Criteria

- 

## Final Verification

- `scripts/verify-quick.sh`
- Local static analysis via `scripts/quality-check.sh` for `flow-orchestrator` when production code changes
- Repository-supported startup verification
- API smoke tests via `scripts/karate-test.sh`
