# User Story: Update Issue API

**Feature name:** `update-issue-api`
**Story date:** `2026-04-19`

## Business Goal

Enable delivery managers and engineering teams to update existing GitLab issues programmatically through a structured API, including field modifications and label management. This capability complements issue creation and retrieval, providing full lifecycle management for work items through the flow orchestration API.

## Problem Statement

Teams can currently create and retrieve GitLab issues through the API, but they lack the ability to update existing issues programmatically. Manual issue updates through the GitLab web interface interrupt workflow automation and prevent teams from automating issue refinement, label management, and status updates as part of their delivery tracking processes.

## User Story

As a delivery manager or engineering team member,
I want to update titles, descriptions, and labels on existing GitLab issues through the API,
so that I can automate issue refinement and label management without leaving my workflow tools.

## Locked Request Constraints

These are the non-negotiable constraints copied from the original request or explicitly locked by Team Lead.

- Endpoint contract constraints:
  - New endpoint: PATCH /api/issues/{issueId}
  - issueId is the same project-scoped issue identifier already used by existing issues API
  - Request body contains title (optional), description (optional), addLabels (optional), removeLabels (optional)

- Request payload validation rules:
  - At least one updatable field must be provided (title, description, addLabels non-empty, or removeLabels non-empty)
  - title, when provided: must be non-null, non-blank, min length 3, max length 255
  - description, when provided: max length 1,000,000; empty string is allowed and means clear/update description intentionally; null means no change
  - addLabels, when provided: max 10 items, each item must be non-blank
  - removeLabels, when provided: max 10 items, each item must be non-blank
  - Reject request when the same label appears in both addLabels and removeLabels

- Response contract constraints:
  - Response must reuse existing IssueSummaryDto
  - Do not introduce new response DTO for this feature

- Configuration constraints:
  - Make the following limits configurable via application.yml: title min length, title max length, description max length, max labels per request field
  - Reuse same configured limits across create and update flows

- Creation alignment constraints:
  - Apply title min/max limits to create-issue endpoint
  - Apply description max limit to create-issue endpoint
  - Apply max labels limit to create-issue endpoint labels

- GitLab integration constraints:
  - Use official GitLab update issue API
  - Local API stays PATCH /api/issues/{issueId}
  - Downstream GitLab call uses documented issue update endpoint
  - Field mapping: title → title, description → description, addLabels → add_labels, removeLabels → remove_labels
  - When adding labels, use GitLab additive label update behavior (do not replace all labels unless explicitly required)

- Response mapping constraints:
  - Return updated issue mapped back to IssueSummaryDto

## Business Context and Constraints

- Primary users: Delivery managers, engineering managers, and agile team members who need to manage work item details programmatically for flow tracking and automation.
- Issue terminology: GitLab issue is the MVP representation of a work item in the flow orchestration domain.
- Project baseline: All API calls operate against a single configured GitLab project (same as create-issue-api).
- Label behavior: GitLab automatically creates labels that do not exist when adding labels. Removing non-existent labels is safely handled by GitLab.
- Business-facing performance expectations: Issue updates must complete within a reasonable time for synchronous API calls (typically under 3 seconds for normal GitLab API response times).
- Validation consistency: Title, description, and label limits must be consistent between create-issue-api and update-issue-api for predictable user experience.

## Scope

### In Scope

- Update existing GitLab issues in the configured project with optional title, description, and label changes.
- Accept PATCH /api/issues/{issueId} with optional JSON request body containing title (optional), description (optional), addLabels (optional), removeLabels (optional).
- Validate that at least one updatable field is provided.
- Validate title constraints: non-null when provided, non-blank, min length 3, max length 255.
- Validate description constraints: max length 1,000,000 when provided; null means no change; empty string is allowed and intentionally clears description.
- Validate addLabels constraints: max 10 items, each non-blank, when provided.
- Validate removeLabels constraints: max 10 items, each non-blank, when provided.
- Reject updates where the same label appears in both addLabels and removeLabels.
- Return HTTP 200 OK on successful issue update with updated issue mapped to IssueSummaryDto.
- Apply shared title min/max length, description max length, and max labels limits to both create-issue-api and update-issue-api.
- Make limits configurable in application.yml for reuse across both endpoints.
- Reuse existing shared error handling, validation patterns, logging conventions, and test strategy.

### Out of Scope

- Updating fields other than title, description, and labels (e.g., milestone, assignee, state transition, epic).
- Partial-update strategies where missing fields in the request body are interpreted differently (absent fields mean no change; only explicit values cause updates).
- Project selection per request (only the configured baseline project is used).
- Batch or bulk update support.
- Label hierarchy or special label category management.

## Acceptance Criteria

1. Given a valid JSON request body with title, description, addLabels, and/or removeLabels, when the API is called to update an issue, then the issue is updated in the configured GitLab project and HTTP 200 OK is returned with the updated issue mapped to IssueSummaryDto.

2. Given a JSON request body with only title provided (other fields omitted), when the API is called, then only title is updated in GitLab and the response contains the updated issue with the new title and unchanged labels, description, and other fields.

3. Given a JSON request body with description set to an empty string, when the API is called, then the description is intentionally cleared in GitLab and the response reflects the cleared description.

4. Given a JSON request body with description set to null or description omitted, when the API is called, then description is not modified in GitLab.

5. Given a JSON request body with addLabels provided, when the API is called, then those labels are added to the existing labels without removing other labels, and the response contains all labels (existing plus added).

6. Given a JSON request body with removeLabels provided, when the API is called, then only those labels are removed from the issue, and the response contains the remaining labels.

7. Given a JSON request body with both addLabels and removeLabels provided with no overlap, when the API is called, then both operations are applied and the response reflects the updated label set.

8. Given a JSON request body where the same label appears in both addLabels and removeLabels, when the API is called, then the request is rejected with a clear validation error indicating conflicting label operations.

9. Given a JSON request body with no updatable fields (all fields omitted or null/empty), when the API is called, then the request is rejected with a clear validation error indicating that at least one updatable field must be provided.

10. Given a JSON request body with title that is blank or null, when the API is called, then the request is rejected with a clear validation error indicating that title must not be blank.

11. Given a JSON request body with title shorter than 3 characters, when the API is called, then the request is rejected with a clear validation error indicating the title minimum length requirement.

12. Given a JSON request body with title longer than 255 characters, when the API is called, then the request is rejected with a clear validation error indicating the title maximum length requirement.

13. Given a JSON request body with description longer than 1,000,000 characters, when the API is called, then the request is rejected with a clear validation error indicating the description maximum length requirement.

14. Given a JSON request body with addLabels or removeLabels containing more than 10 items, when the API is called, then the request is rejected with a clear validation error indicating the maximum labels per field requirement.

15. Given a JSON request body with addLabels or removeLabels containing one or more blank strings, when the API is called, then the request is rejected with a clear validation error indicating that label values must not be blank.

16. Given a GitLab integration failure (e.g., authentication error, issue not found, rate-limit), when the API is called, then a sanitized error response is returned using the existing shared integration error handling pattern without exposing secrets or raw configuration values.

## Dependencies and Assumptions

- External dependencies:
  - Existing IssueSummaryDto and shared orchestration/mapping structures from create-issue-api are available for reuse.
  - Create-issue-api already exists and will be modified to align with configurable validation limits.
  - GitLab issue update endpoint is documented and stable.
  - Configuration infrastructure (application.yml) supports externalized limit properties.

- Assumptions:
  - Issue identifiers (issueId) map consistently between the local API and GitLab's project-scoped issue identifier (iid), the public-facing issue number visible in GitLab.
  - GitLab additive label update behavior is safe and does not cause unintended side effects.
  - Existing error handling and test infrastructure can be extended to cover update scenarios without significant refactoring.

## Open Questions

- None.
