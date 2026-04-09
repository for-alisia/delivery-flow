# User Story: Expose Project-Scoped Issue Number in API Responses

**Feature name:** `issue-id-field`
**Story date:** `2025-07-24`

## Business Goal

Delivery managers and integrating clients need a stable, human-meaningful identifier to reference individual work items in external tools, dashboards, and reports. The project-scoped issue number (e.g., `#42`) is the identifier teams use daily in GitLab and in conversation. Exposing it consistently on every issue response removes the need for clients to maintain their own lookup tables or perform secondary calls.

## Problem Statement

The current issue API responses return issues without their project-scoped number. Clients that need to link back to a specific GitLab issue, display familiar references in a dashboard, or correlate work items across systems must do so without a reliable human-readable ID. This creates friction for downstream tooling and reduces the usefulness of the API as a delivery-visibility source.

## User Story

As a delivery tool or dashboard integrating with GitlabFlow,
I want every issue object in the API response to include the project-scoped issue number as `issueId`,
so that I can reference, display, and link to individual work items without additional lookups.

## Locked Request Constraints

- **Contract constraints:** The client-facing field name MUST be `issueId`; the internal GitLab field name `iid` must never appear in the API response.
- **Payload constraints:** Both `POST /api/issues/search` and `POST /api/issues` responses must include `issueId` on every returned issue object.
- **Source-of-truth constraint:** `issueId` maps directly from GitLab's `iid` (project-scoped, sequential issue number).
- **Change scope constraint:** This is an additive-only change — no existing fields are renamed, removed, or retyped.
- **Documentation constraint:** `documentation/context-map.md` must be updated to reflect `issueId` on `IssueDto`.
- **Test constraint:** All affected unit, integration, and component tests must be updated.
- **Smoke-test constraint:** `scripts/smoke-test.sh` must assert `issueId` is present in responses from both endpoints.
- **Unresolved items:** None.

## Business Context and Constraints

- **Primary users:** Delivery managers, engineering managers, Agile teams, and platform integrators consuming the GitlabFlow API.
- **Business rules:** The project-scoped issue number is the number shown in the GitLab UI next to the `#` symbol (e.g., `#42`). It is unique within a project but not globally unique across GitLab.
- **Terminology:** `issueId` in GitlabFlow maps to GitLab's `iid`; it is distinct from GitLab's global `id`.
- **Business-facing performance expectations:** None beyond existing baseline.
- **Business-facing security or privacy expectations:** Issue numbers are already visible to any user with access to the project; no additional access-control concerns.

## Scope

### In Scope

- Add `issueId` field to every issue object returned by `POST /api/issues/search`.
- Add `issueId` field to every issue object returned by `POST /api/issues`.
- Map `issueId` from GitLab's `iid` field.
- Update all affected unit, integration, and component tests to assert `issueId` presence and correctness.
- Update `scripts/smoke-test.sh` to assert `issueId` is present in responses from both endpoints.
- Update `documentation/context-map.md` to document `issueId` on `IssueDto`.

### Out of Scope

- Renaming or removing any existing response field.
- Adding `issueId` to any endpoint other than the two issue endpoints listed above.
- Exposing GitLab's global `id` field.
- Filtering, sorting, or searching by `issueId`.
- Any change to GitLab API authentication or pagination behavior.

## Acceptance Criteria

1. **Given** a client calls `POST /api/issues/search` with valid parameters, **when** the response is returned, **then** every issue object in the response contains an `issueId` field with the integer value of the project-scoped issue number.

2. **Given** a client calls `POST /api/issues` to create an issue, **when** the response is returned, **then** the issue object contains an `issueId` field with the integer value of the newly created issue's project-scoped number.

3. **Given** any issue object in any response from either endpoint, **when** the client inspects the field name, **then** the field is named exactly `issueId` — the name `iid` does not appear anywhere in the response payload.

4. **Given** a known GitLab issue with `iid` equal to `N`, **when** that issue is returned by either endpoint, **then** `issueId` equals `N`.

5. **Given** the updated implementation, **when** `scripts/smoke-test.sh` is executed against a running instance, **then** the script asserts and confirms that `issueId` is present in responses from both `POST /api/issues/search` and `POST /api/issues`.

6. **Given** the updated implementation, **when** unit, integration, and component test suites are executed, **then** all tests pass and tests covering both endpoints assert that `issueId` is present and correct.

7. **Given** the updated implementation, **when** `documentation/context-map.md` is reviewed, **then** it documents `issueId` as a field on `IssueDto` with a note that it maps from GitLab's `iid`.

8. **Given** the change is complete, **when** any existing API response fields are inspected, **then** no previously present field has been renamed, removed, or changed in type — the change is additive only.

## Dependencies and Assumptions

- **External dependencies:** GitLab REST API `iid` field is always present on issue objects returned by the GitLab Issues API.
- **Assumptions:**
  - GitLab's `iid` is always a positive integer; no null-handling for `iid` is required beyond what GitLab guarantees.
  - The `IssueDto` class is the authoritative client-facing response model; changes are applied there and propagated through the mapping layer.
  - No breaking consumer exists that would be harmed by an additive field addition.

## Open Questions

- None.
