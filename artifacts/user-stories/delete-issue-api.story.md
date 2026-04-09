# User Story: Delete Issue API

**Feature name:** `delete-issue-api`
**Story date:** `2026-04-06`

## Business Goal

Delivery teams need to remove work items that were created in error, duplicated, or no longer relevant without switching to the GitLab UI. Providing a delete endpoint in GitlabFlow allows client tools and the MCP server to close this gap and keep issue lists accurate and meaningful for flow analysis.

## Problem Statement

Currently, clients can create and retrieve issues through GitlabFlow, but cannot delete them. Teams must leave behind noise issues — duplicates, test entries, or cancelled items — because there is no deletion path through the product. This pollutes the work-item list and undermines the reliability of flow visibility data.

## User Story

As a delivery manager or integrating client,
I want to delete a work item by its project-scoped issue number,
so that I can remove invalid or obsolete issues and keep the work-item list accurate for flow reporting.

## Locked Request Constraints

These are the non-negotiable constraints copied from the original request or explicitly locked by Team Lead.

- **Input or source-of-truth constraints:** The `issueId` path parameter is the project-scoped issue number (GitLab `iid`), not any internal or database identifier.
- **Contract or payload constraints:** Successful deletion returns HTTP 204 No Content with no response body.
- **External-system constraints:** The request is forwarded to GitLab using the URL-encoded project path (not the numeric project ID).
- **Error mapping constraints:** A GitLab 404 response (issue not found by `iid`) must surface to callers as a 404 with the standard error response format. A GitLab 403 response (insufficient PAT permissions) must surface as a 403 with the standard error response format.
- **MVP scope constraint:** No domain logic is applied before or after deletion — this is a pure pass-through for MVP.
- **Unresolved items:** None. All constraints confirmed by Team Lead.

## Business Context and Constraints

- **Primary users or stakeholders:** Delivery managers, engineering managers, and integrating client tools (including the MCP server) that manage work-item hygiene.
- **Important business rules:** The issue is identified by the GitLab project-scoped number (the number teams use daily, e.g., `#42`), not by any internal system key.
- **Happy-path assumption:** For MVP, the configured Personal Access Token is assumed to have the permissions required to delete issues on the target project. PAT permission errors are still mapped if they occur but are not the primary concern for MVP.
- **Business-facing security expectation:** The endpoint must not expose GitLab internal error details or stack traces in error responses.

## Scope

### In Scope

- A delete-issue endpoint that accepts a project-scoped issue number and removes the corresponding issue from GitLab.
- HTTP 204 No Content response on successful deletion.
- HTTP 404 response with a standard error body when the issue does not exist.
- HTTP 403 response with a standard error body when the operation is not permitted by the GitLab PAT.

### Out of Scope

- Soft-delete, archiving, or any state transition that does not fully remove the issue.
- Bulk deletion of multiple issues in a single request.
- Domain-level precondition checks before deletion (e.g., checking issue status or labels before allowing removal).
- Audit logging or event publishing for deletions.
- Rollback or recovery of deleted issues.

## Acceptance Criteria

1. **Given** a valid project-scoped issue number, **when** a client sends `DELETE /api/issues/{issueId}`, **then** the system removes the issue from GitLab and returns HTTP 204 with no response body.
2. **Given** a project-scoped issue number that does not exist in GitLab, **when** a client sends `DELETE /api/issues/{issueId}`, **then** the system returns HTTP 404 with the standard error response format (no GitLab-internal detail exposed).
3. **Given** the configured PAT does not have permission to delete the issue, **when** a client sends `DELETE /api/issues/{issueId}`, **then** the system returns HTTP 403 with the standard error response format (no GitLab-internal detail exposed).
4. **Given** any outcome (success or error), **when** the response is returned, **then** no GitLab-internal identifiers, stack traces, or raw error messages from GitLab are included in the response body.
5. **Given** the delete endpoint is available, **when** a client uses the project-scoped issue number visible in GitLab (e.g., `#42`), **then** the correct issue is deleted without requiring knowledge of any numeric project ID or internal key.

## Dependencies and Assumptions

- **External dependencies:** GitLab REST API — `DELETE /projects/:id/issues/:issue_iid` endpoint, using the URL-encoded project path as `:id`.
- **Internal dependencies:** Builds on the existing GitLab Feign client and error-mapping infrastructure established by the create-issue and first-issues features.
- **Assumptions:**
  - The standard error response format used by existing endpoints is already established and reusable.
  - The URL-encoded project path is already available as a configuration value.
  - The configured PAT has sufficient GitLab permissions for the happy path in all target environments.

## Open Questions

- None.
