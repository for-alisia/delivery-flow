# User Story: Create Milestone API

**Feature name:** `create-milestone-api`
**Story date:** `2026-04-25`

## Business Goal

Enable delivery teams to programmatically create milestones in GitLab through a dedicated API endpoint, allowing them to establish delivery cycles as part of their delivery planning workflow without requiring direct GitLab access.

## Problem Statement

Teams need to create milestones to organize and track delivery cycles within GitLab, but currently lack a streamlined, validated API for this operation. Creating milestones requires teams to manage GitLab authentication and project routing on a per-request basis, adding friction to delivery planning automation.

## User Story

As a delivery team member,
I want to create a new milestone in the configured GitLab project through a simple REST API,
so that I can establish and manage delivery cycles as part of our delivery planning workflow.

## Locked Request Constraints

These constraints are non-negotiable and come directly from the Team Lead requirement lock.

- **Endpoint and project routing:**
  - Endpoint is `POST /api/milestones`
  - Client must not pass GitLab project ID, project path, or project URL in the request
  - The configured project from `application-local.yml` is the only source of truth for the target project

- **Request contract:**
  - `title` is required; must be validated to be more than 3 characters and less than 500 characters
  - `description` is optional; no validation required
  - `dueDate` is optional; if provided, must be in a valid date format
  - `startDate` is optional; if provided, must be in a valid date format
  - If both `dueDate` and `startDate` are provided, `dueDate` must be after `startDate`

- **Response contract:**
  - On successful creation, return HTTP `201 Created`
  - Response body must include `milestoneId` (GitLab milestone IID, not GitLab database ID) and `title`
  - On validation error, return HTTP `400 Bad Request` with an error message
  - On GitLab failures, follow the already established error pattern

- **Scope limitation:**
  - Keep scope limited to this API addition within the flow-orchestrator milestone capability
  - Do not extend to milestone updates, deletions, or other milestone operations

## External Contracts

### Client Contract

- **Endpoint:** `POST /api/milestones`
- **Required request shape:**
  - `title` (string, required): milestone title; must be longer than 3 characters and shorter than 500 characters
  - `description` (string, optional): milestone description; no length validation
  - `dueDate` (string, optional): ISO 8601 date format (e.g., `2024-12-31`); required to be after `startDate` if both are provided
  - `startDate` (string, optional): ISO 8601 date format (e.g., `2024-01-01`); required to be before `dueDate` if both are provided

- **Title-only request example:**
  ```json
  {
    "title": "Release v1.0"
  }
  ```

- **Full request example:**
  ```json
  {
    "title": "Q2 2026 Delivery",
    "description": "Second quarter release cycle",
    "startDate": "2026-04-01",
    "dueDate": "2026-06-30"
  }
  ```

- **Required response shape:**
  - Success (HTTP `201 Created`): response body contains `milestoneId` (GitLab IID) and `title`
  - Validation failure (HTTP `400`): response body contains an error message describing the constraint violation
  - GitLab failure: response follows established error pattern for GitLab integration failures

- **Minimal success response example:**
  ```json
  {
    "milestoneId": 42,
    "title": "Q2 2026 Delivery"
  }
  ```

- **Validation error example:**
  ```json
  {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": [
      "dueDate must be after startDate"
    ]
  }
  ```

- **GitLab failure example:**
  ```json
  {
    "code": "RESOURCE_NOT_FOUND",
    "message": "GitLab create milestone operation failed",
    "details": []
  }
  ```

### Upstream / GitLab Contract

- **Operation:** `POST /projects/:id/milestones`
- **GitLab request shape:** Maps client payload to GitLab milestone creation endpoint; `title` is required in GitLab, `description`, `start_date`, and `due_date` are optional
- **GitLab response fields relied on:** `iid` (mapped to `milestoneId`), `title`
- **Date format mapping:** Client accepts ISO 8601 dates; GitLab accepts `YYYY-MM-DD` format
- **Official doc reference:** GitLab REST API project milestones endpoint — `POST /projects/:id/milestones` (https://docs.gitlab.com/api/milestones.html#create-a-milestone)

## Business Context and Constraints

- **Primary users:** Delivery teams using GitlabFlow to manage work cycles and release planning.
- **Business rules:** Milestones represent delivery cycles in GitLab; reuse the single configured project consistently across all milestone operations to eliminate per-request project routing.
- **External dependency:** GitLab REST API project milestones creation endpoint; requires valid project configuration and GitLab authentication.
- **Security considerations:** Do not expose GitLab project ID or authentication details in client payloads; all configuration is server-side.

## Scope

### In Scope

- Accept milestone creation requests with title and optional metadata (description, start/due dates)
- Validate title length (greater than 3 characters, less than 500 characters)
- Validate date formats and ensure logical date ordering (start before due)
- Map client request to GitLab API contract
- Return HTTP `201` with `milestoneId` (GitLab IID) and `title` on successful creation
- Return HTTP `400` with validation error message on input validation failure
- Handle GitLab integration failures following the established error pattern
- Extend the existing milestones capability within `flow-orchestrator`

### Out of Scope

- Milestone updates or modifications
- Milestone deletion
- Group-level milestone creation
- Per-request project routing or multi-project support
- Bulk milestone creation
- Advanced milestone configuration (e.g., access levels, weight, burndown chart settings)
- Milestone deletion or archival
- Cascading operations triggered by milestone creation

## Acceptance Criteria

1. **Endpoint is available and routable:** `POST /api/milestones` accepts requests and returns a response with the appropriate HTTP status code.

2. **Minimal valid request succeeds:** When called with only a required `title` field (e.g., `{"title": "Release v1.0"}`), the endpoint creates a milestone and returns HTTP `201` with `milestoneId` and `title` in the response body.

3. **Title validation is enforced:** 
   - Providing a title with 3 or fewer characters returns HTTP `400` with a validation error message.
   - Providing a title with 500 or more characters returns HTTP `400` with a validation error message.
   - Providing a title with 4–499 characters succeeds and creates the milestone.

4. **Optional fields are accepted:** Providing `description`, `startDate`, and `dueDate` in the request body succeeds and maps those values to the GitLab milestone creation request.

5. **Date format validation works:** 
   - Providing `dueDate` or `startDate` in a non-ISO-8601 format (e.g., `"2024/12/31"` or `"December 31, 2024"`) returns HTTP `400` with a validation error message.
   - Providing valid ISO 8601 dates (e.g., `"2024-12-31"`) succeeds.

6. **Date ordering is validated:** When both `startDate` and `dueDate` are provided and `dueDate` is not after `startDate`, the endpoint returns HTTP `400` with a validation error message specifying the date ordering constraint.

7. **Optional fields do not block creation:** When `description`, `startDate`, or `dueDate` are omitted, the endpoint still creates the milestone successfully with only the title.

8. **Response includes GitLab IID, not database ID:** The `milestoneId` field in the response is the GitLab IID (sequence number within the project), not the GitLab database ID.

9. **GitLab errors are handled and sanitized:** When GitLab returns an error (e.g., authentication failure, project not found), the response sanitizes the error and does not expose internal details, stack traces, or provider-specific messages.

10. **Architecture follows established patterns:** Endpoint validation, layer boundaries, domain/orchestration/integration separation, and error handling match the existing Issues and Milestones Search APIs.

## Dependencies and Assumptions

- **External dependencies:**
  - GitLab REST API project milestones creation endpoint (`POST /projects/:id/milestones`) is stable and available.
  - Application configuration provides a valid, authenticated GitLab project ID in `application-local.yml`.

- **Assumptions:**
  - Client applications will use standard HTTP libraries to call the endpoint; no special client library is provided.
  - Title and date validation is sufficient for MVP; future milestones may require additional validation or metadata.
  - The configured project remains static during the application lifetime (no runtime project switching).
  - Validation follows the same patterns established by the Issues and Milestones Search capabilities.

## Open Questions

- Should the response body include additional milestone fields beyond `milestoneId` and `title` (e.g., `createdAt`, `state`), or does MVP scope limit this to the two required fields?
- Are there specific date format requirements beyond ISO 8601, or should the validation accept a flexible date parsing approach?
