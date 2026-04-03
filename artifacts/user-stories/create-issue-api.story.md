# User Story: Create Issue API

**Feature name:** `create-issue-api`
**Story date:** `2026-04-02`

## Business Goal

Enable delivery managers and engineering teams to create GitLab issues programmatically through a structured API. This capability supports workflow automation and issue creation directly from flow orchestration clients, complementing the existing issue-retrieval functionality.

## Problem Statement

Teams can currently retrieve and filter GitLab issues through the API, but they lack the ability to create new issues programmatically. Manual issue creation through the GitLab web interface interrupts workflow automation and prevents teams from integrating issue creation into their delivery tracking and planning processes.

## User Story

As a delivery manager or engineering team member,
I want to create GitLab issues in our configured project through the API with title, description, and labels,
so that I can automate work item creation and maintain consistent issue tracking without leaving my workflow tools.

## Locked Request Constraints

These are the non-negotiable constraints copied from the original request or explicitly locked by Team Lead.

- Configuration constraints:
  - Add issue creation API for the single configured GitLab project only.
  - Clients must not pass GitLab project id, path, or URL in the request.
  - Configured project from application-local.yml is the only source of truth.

- Implementation scope constraints:
  - Feature remains pass-through MVP: validate input, call GitLab, map response, return sanitized errors.

- Endpoint and contract constraints:
  - Creation endpoint must be POST /api/issues with mandatory JSON body.
  - Existing fetch endpoint must be renamed from POST /api/issues to POST /api/issues/search.
  - Rename must be reflected consistently in controller mappings, tests, smoke coverage, and .http examples.

- Request payload constraints:
  - Create payload uses title (required, non-null, non-blank), description (optional), labels (optional array of non-blank strings).
  - Malformed JSON must return the existing validation error shape with a clear message.

- Integration constraints:
  - GitLab create-issue contract details must be confirmed against official GitLab REST API docs before implementation.
  - Expected GitLab fields used by this feature are title (required), description (optional), labels (optional).

- Response constraints:
  - Successful create response must return HTTP 201 Created.
  - Success response must contain id, title, description, and labels.
  - Response description field must always be present; either populated with the value provided in the request or null if description was omitted.
  - Returned id must use GitLab issue id, not iid.

- Error handling constraints:
  - Validation and integration failures must reuse the existing shared error contract and sanitized exception handling.
  - Reuse existing validation, shared integration error handling, logging, test strategy, and documentation conventions where applicable.

## Business Context and Constraints

- Primary users: Delivery managers, engineering managers, and agile team members who need to create work items programmatically for flow tracking and automation.
- Issue terminology: GitLab issue is the MVP representation of a work item in the flow orchestration domain.
- Project baseline: All API calls operate against a single configured GitLab project. Users do not select projects per request.
- Baseline configuration: Project URL and authentication token are externalized in application-local.yml for local development and testing.
- Label behavior: GitLab automatically creates labels that do not exist when provided during issue creation. This is acceptable for MVP.
- Business-facing performance expectations: Issue creation must complete within a reasonable time for synchronous API calls (typically under 3 seconds for normal GitLab API response times).

## Scope

### In Scope

- Create GitLab issues in the configured project with title, description, and labels.
- Accept POST /api/issues with a mandatory JSON request body containing title (required), description (optional), and labels (optional).
- Validate that title is non-null and non-blank.
- Validate that labels, if provided, contains only non-blank strings.
- Return HTTP 201 Created on successful issue creation.
- Return response containing issue id (GitLab issue id, not iid), title, description, and labels.
- Ensure description field is always present in the response; populated with the value from the request or null if description was omitted.
- Reject malformed JSON with the existing validation error shape and clear message.
- Rename existing fetch endpoint from POST /api/issues to POST /api/issues/search.
- Update controller mappings, integration tests, component tests, smoke tests, and .http examples to reflect the endpoint rename.
- Reuse existing shared error handling, validation patterns, logging conventions, and test strategy.

### Out of Scope

- Project selection per request (only the configured baseline project is used).
- Issue update, delete, or state transition support.
- Additional create-time fields beyond title, description, and labels (e.g., milestone, assignee, epic).
- Multi-project support or cross-project issue creation.
- Keeping the old POST /api/issues fetch route alive after the rename (rename fully replaces the old route).

## Acceptance Criteria

1. Given a valid JSON request body with title, description, and labels, when the API is called to create an issue, then the issue is created in the configured GitLab project and HTTP 201 Created is returned with id, title, description, and labels all present in the response body.

2. Given a JSON request body with only title provided (description and labels omitted), when the API is called, then the issue is created successfully and the response includes the issue id, title, description (as null), and labels (as an empty array or null as returned by GitLab).

3. Given a JSON request body with a blank or null title, when the API is called, then the request is rejected with a clear validation error indicating that title must not be blank.

4. Given a JSON request body with labels containing one or more blank strings, when the API is called, then the request is rejected with a clear validation error indicating that label values must not be blank.

5. Given malformed JSON in the request body, when the API is called, then the request is rejected with the existing validation error shape and a clear message.

6. Given a GitLab integration failure (e.g., authentication error, not-found, rate-limit), when the API is called, then a sanitized error response is returned using the existing shared integration error handling pattern without exposing secrets or raw configuration values.

7. Given the existing fetch endpoint was POST /api/issues, when the endpoint rename is applied, then the fetch endpoint is accessible at POST /api/issues/search and the old POST /api/issues route is no longer available for fetch operations.

8. Given the endpoint rename is implemented, when controller mappings, integration tests, component tests, smoke tests, and .http examples are reviewed, then all references consistently use POST /api/issues/search for fetch operations and POST /api/issues for create operations.

9. Given labels are provided in the create request that do not exist in the GitLab project, when the issue is created, then GitLab automatically creates the missing labels and the issue is created successfully.

10. Given a successful issue creation, when the response is returned, then the id field contains the GitLab issue id (not iid).

## Dependencies and Assumptions

- External dependencies:
  - GitLab REST API issues endpoint for creation must be accessible and support the parameters title (required), description (optional), and labels (optional).
  - Baseline GitLab project URL and personal access token are externalized in application-local.yml.
  - GitLab project must exist and allow issue creation with the provided authentication token.

- Assumptions:
  - GitLab API behavior aligns with official documentation for the create issue endpoint.
  - GitLab personal access token has sufficient permissions to create issues in the configured project.
  - GitLab automatically creates labels that do not exist when provided during issue creation.
  - Existing shared validation, error handling, and logging conventions are reusable for this feature.
  - The fetch endpoint rename from POST /api/issues to POST /api/issues/search does not break external clients (or breaking change is accepted as part of MVP iteration).

## Open Questions

- None.
