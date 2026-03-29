# Implementation Plan: <title>

**Task name:** `<task-name>`
**Plan date:** `<yyyy-mm-dd>`
**Status:** `Draft`

## Ownership

- Architect owns the planning sections through `Final Verification` and `Risks / Notes` before implementation handoff.
- Reviewer owns `artifacts/review-reports/<feature-name>.review.md`.
- Coder updates `Implementation Update`, `Code Guidance Ledger`, `Acceptance Criteria -> Evidence`, `Blocked Verification`, and `Implementation Details` during or after delivery.
- Team Lead records approvals and final acceptance in a separate sign-off artifact under `artifacts/implementation-signoffs/`.

## Business Goal

Short paragraph describing the business or technical outcome.

## Requirement Lock / Source Of Truth

- Original request source:
- Non-negotiable input or source-of-truth constraints:
- Non-negotiable contract or payload constraints:
- Non-negotiable configuration or external-system constraints:
- Explicit assumptions or unresolved items:

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

Repeat the slice structure below for as many slices as needed.

### Slice N - <short title>

- Short description:
- Affected scope:
- Unit tests:
- Integration / Web tests:
- Edge / failure coverage:
- Documentation updates:

## Testing Matrix

| Level | Required | Planned Coverage | Evidence Target |
|---|---|---|---|

## Acceptance Criteria

- 

## Final Verification

- `mvn test`
- `mvn -q -DskipTests compile`
- Sonar analysis and quality gate for `flow-orchestrator` when production code changes
- Repository-supported startup verification
- Required `curl` smoke checks

## Risks / Notes

- 

## Implementation Update

- Summary:
- Branch / worktree reference:
- Head commit SHA at verification:
- Compared against base/reference:
- Changed files:
- Approved deviations (must be approved by Team Lead):
- `artifacts/code-guidance.md` quality gate passed:
- Verification commands and outcomes (record secrets in redacted form only):
- Sonar command and quality gate result:
- Sonar dashboard URL:
- Sonar CE task URL / ID:
- Sonar analysis ID:
- Documentation updates completed:

## Code Guidance Ledger

| Gate | Status | Evidence / Notes |
|---|---|---|

## Acceptance Criteria -> Evidence

| Acceptance Criterion | Implementation Evidence | Verification Evidence | Status |
|---|---|---|---|

## Blocked Verification

- `None`, or list blocked checks with command, issue, and impact.

## Implementation Details

Coder updates implementation notes here during or after delivery.

- 
