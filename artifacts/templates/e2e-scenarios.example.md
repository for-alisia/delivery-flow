# E2E Scenarios: feature-name

## Objective

- One-line summary of the runtime behavior that must be smoke-verified.

## Scenario Inventory

| ID | Scenario | Why it matters | Source | Repeatability |
|----|----------|----------------|--------|---------------|
| E2E-1 | Happy-path create/search/etc. | Covers the primary success flow | Locked request + story AC-1 | Use unique suffix in the mutable field |
| E2E-2 | Validation or client error | Confirms contract-safe rejection path | Story `External Contracts` error example | No cleanup needed |
| E2E-3 | Provider/downstream failure or not-found path | Confirms stable integration/error translation | Locked request or existing behavior | Reuse a non-existent ID or controlled stub condition |

## Coverage Decisions

- Identify which existing Karate feature should be extended, or whether a new one is required.
- Note any capability runner change needed.

## Data Strategy

- Unique-data rule for mutable resources.
- Cleanup rule when the API supports deletion or teardown.
- Explicit note when cleanup is impossible and the scenario must remain repeatable through unique values only.

## Open Questions / Blockers

- List unresolved runtime assumptions or provider constraints.