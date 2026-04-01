# User Story: First Issues API

**Feature name:** `first-issues-api`
**Story date:** `2026-03-30`

## Business Goal

Establish the foundational issue-retrieval capability that enables delivery managers and engineering teams to view GitLab issues through a structured API. This is the first step toward building comprehensive flow visibility and bottleneck detection.

## Problem Statement

Teams currently lack a structured way to retrieve and filter GitLab issues programmatically. They need a reliable API that provides filtered issue lists with pagination support to start building flow insights and delivery-state visibility.

## User Story

As a delivery manager or engineering team member,
I want to retrieve GitLab issues from our configured project with optional filters and pagination,
so that I can obtain structured issue data for flow analysis and delivery tracking.

## Locked Request Constraints

These are the non-negotiable constraints copied from the original request or explicitly locked by Team Lead.

- Input constraints:
  - Deliver POST /api/issues with JSON request body preferred over query parameters.
  - Request body is optional; missing body must be accepted and treated as default list-all behavior.
  - Pagination defaults are page=1 and perPage=40 when omitted.
  
- Filter constraints:
  - Supported request filters for MVP are state, labels, assignee, and milestone.
  - labels accepts at most one value; more than one must be rejected with validated client error.
  - assignee accepts at most one value; more than one must be rejected with validated client error.
  - milestone accepts at most one value; more than one must be rejected with validated client error.

- Response shape constraints:
  - Response must contain items, count, and page.
  - Each item must expose id, title, description, state, labels, assignee, milestone, and parent where parent is the parent epic id when present else null.

- Configuration constraints:
  - Use the configured GitLab project as the baseline source; clients must not provide project per request.
  - Project baseline comes from externalized configuration and must not be hardcoded.

- Integration constraint:
  - GitLab project issues endpoint is GET /projects/:id/issues with support for page, per_page, state, labels, assignee_username, milestone, and PRIVATE-TOKEN authentication.

## Business Context and Constraints

- Primary users: Delivery managers, engineering managers, and agile team members who need structured access to issue data for flow visibility.
- Issue terminology: GitLab issue is the MVP representation of a work item in the flow orchestration domain.
- Parent reference: When an issue belongs to a parent epic, the parent epic ID must be included; otherwise null.
- Project baseline: All API calls operate against a single configured GitLab project. Users do not switch projects per request.
- Baseline configuration: Project URL and authentication token are externalized in application-local.yml for local development and testing.

## Scope

### In Scope

- Retrieve issues from the configured GitLab project.
- Support optional JSON request body with pagination and filters.
- Support state filter (e.g., "opened", "closed").
- Support single-label filter with validation to reject multiple labels.
- Support single-assignee filter with validation to reject multiple assignees.
- Support single-milestone filter with validation to reject multiple milestones.
- Return issue details: id, title, description, state, labels, assignee, milestone, and parent (epic id or null).
- Return response with items array, count, and page number.
- Apply pagination defaults (page=1, perPage=40) when not provided.
- Accept requests with no body and return all issues using defaults.

### Out of Scope

- Support for multiple labels, assignees, or milestones per request.
- Project selection per request (only configured baseline project is used).
- Advanced filtering beyond state, labels, assignee, and milestone.
- Issue creation, updates, or deletions (this story is read-only).
- Cross-project issue retrieval.
- Sorting or ordering preferences (will use GitLab default ordering).

## Acceptance Criteria

1. Given the API is called without a request body, when the request is processed, then all issues from the configured GitLab project are returned with default pagination (page 1, 40 items per page).

2. Given a request body with state filter set to "opened", when the request is processed, then only issues with state "opened" are returned.

3. Given a request body with a single label filter, when the request is processed, then only issues with that label are returned.

4. Given a request body with multiple labels, when the request is processed, then a clear validation error is returned and the request is rejected.

5. Given a request body with a single assignee filter, when the request is processed, then only issues assigned to that user are returned.

6. Given a request body with multiple assignees, when the request is processed, then a clear validation error is returned and the request is rejected.

7. Given a request body with a single milestone filter, when the request is processed, then only issues in that milestone are returned.

8. Given a request body with multiple milestones, when the request is processed, then a clear validation error is returned and the request is rejected.

9. Given a valid request, when the response is returned, then each issue includes id, title, description, state, labels, assignee, milestone, and parent (parent epic id or null if no parent exists).

10. Given a valid request, when the response is returned, then the response includes an items array, a count field, and a page field.

11. Given a request with custom pagination (page=2, perPage=20), when the request is processed, then the response reflects the requested pagination settings.

12. Given a request with no pagination values provided, when the request is processed, then default pagination (page=1, perPage=40) is applied.

## Dependencies and Assumptions

- External dependencies:
  - GitLab REST API project issues endpoint (GET /projects/:id/issues) must be accessible and support documented parameters (page, per_page, state, labels, assignee_username, milestone).
  - Baseline GitLab project URL and personal access token are externalized in application-local.yml.
  - GitLab project must exist and contain test issues for verification.

- Assumptions:
  - GitLab API behavior aligns with official documentation for the issues endpoint.
  - GitLab personal access token has sufficient permissions to read project issues.
  - Issue parent (epic) information is available in the GitLab API response when applicable.
  - Clients prefer JSON request bodies over query parameters for filter and pagination inputs.

## Open Questions

- None.
