# User Story: Milestone Search API

**Feature name:** `milestones-search-api`
**Story date:** `2026-04-20`

## Business Goal

Enable delivery teams to retrieve milestones from their configured GitLab project through a single, authenticated API endpoint. This is the MVP for the Milestones capability and establishes the pattern for future milestone-scoped features like searching issues within a milestone.

## Problem Statement

Teams currently lack a streamlined way to query milestones without making direct GitLab API calls. A dedicated, project-scoped milestone API eliminates the need for teams to manage project identifiers and GitLab authentication on a per-request basis, simplifying client code and reducing integration complexity.

## User Story

As a delivery team member,
I want to retrieve milestones from the configured GitLab project through a single API endpoint,
so that I can build delivery planning and cycle tracking features without managing project routing logic.

## Locked Request Constraints

These constraints are non-negotiable and come directly from the Team Lead requirement lock.

- **Endpoint and project routing:**
  - Endpoint is `POST /api/milestones/search`
  - Client must not pass GitLab project ID, project path, or project URL in the request
  - The configured project from `application-local.yml` is the only source of truth for the target project
  - This is a project-milestone feature only (not group-milestones)

- **Request contract:**
  - Request body is optional; if omitted or `filters` is omitted, return the default milestone set
  - `filters` object is optional
  - `state` is optional; allowed values if provided: `active`, `closed`, `all`
  - `titleSearch` is optional; if provided, must be non-blank after trimming
  - `milestoneIds` is optional; if provided, must contain only positive integers with no duplicates
  - Malformed JSON must return a clear validation error

- **GitLab mapping and aggregation:**
  - Map `state=active` to GitLab `state=active`; `state=closed` to GitLab `state=closed`; `state=all` means omit upstream state filter
  - Map `titleSearch` to GitLab `search` parameter (partial title/description search)
  - Map `milestoneIds` to GitLab `iids[]` (GitLab IID values, not database IDs)
  - Fetch all pages using `per_page=100` and aggregate results before returning
  - Do not expose pagination to clients in this slice (all results returned in one response)

- **Response contract:**
  - Successful response returns `200 OK`
  - Response format: `{ "milestones": [...] }` where each milestone contains `id`, `milestoneId`, `title`, `description`, `startDate`, `dueDate`, `state`
  - Field mapping: `id` ← GitLab `id`, `milestoneId` ← GitLab `iid`, `title` ← GitLab `title`

- **MVP scope:**
  - Feature stays pass-through: validate input, call GitLab, map response, return sanitized errors
  - Follow the same architecture and conventions as the implemented Issues capability

## Business Context and Constraints

- **Primary users:** Delivery teams using GitlabFlow to track work cycles and milestones.
- **Business rules:** Milestones represent GitLab delivery cycles; reuse the single configured project consistently across all milestone operations to eliminate per-request project routing.
- **Default behavior:** When no filters are provided, return all active project milestones (the most common use case).
- **External dependency:** GitLab REST API project milestones endpoint (`GET /projects/:id/milestones`); honors pagination and supports `state`, `search`, and `iids[]` filters.

## Scope

### In Scope

- Search/list milestones with optional filters (state, title search, milestone IDs)
- Request validation (state values, titleSearch non-blank, milestoneIds positive and unique)
- Full GitLab page aggregation with `per_page=100`
- Mapping between public API contract and GitLab API shapes
- Sanitized error responses for validation and GitLab call failures
- HTTP `POST` endpoint at `/api/milestones/search`

### Out of Scope

- Milestone creation, update, or deletion
- Group-level milestone searches
- Pagination controls exposed to the client
- Per-request project routing or project-selection capability
- Milestone filtering by date range, opened issues, or other advanced attributes beyond state, title search, and milestone IDs
- Client-side project selection or multi-project searches

## Acceptance Criteria

1. **Endpoint is available and routable:** `POST /api/milestones/search` accepts requests and returns `200 OK` with a milestone list.

2. **Default behavior works:** When called with an empty body or omitted `filters`, the endpoint returns all active milestones from the configured project.

3. **State filter is respected:** When `state=active` is provided, only active milestones are returned; when `state=closed`, only closed milestones are returned; when `state=all`, all milestones regardless of state are returned.

4. **Title search works:** When `titleSearch` is provided with a non-empty value, only milestones matching the GitLab `search` parameter (title or description) are returned.

5. **Milestone ID filter works:** When `milestoneIds` is provided as a list of positive integers, only milestones with those GitLab IIDs are returned.

6. **Request validation rejects invalid input:** 
   - Providing an invalid `state` value returns a validation error identifying the constraint violation.
   - Providing a blank `titleSearch` (empty or whitespace-only) returns a validation error.
   - Providing `milestoneIds` with negative numbers, zero, or duplicates returns a validation error identifying which constraint failed.
   - Sending malformed JSON returns a clear parse error.

7. **All GitLab pages are aggregated:** When the GitLab project has more than 100 milestones, all pages are fetched using `per_page=100` and the full list is returned in a single response.

8. **Response structure matches contract:** Each milestone in the response includes `id`, `milestoneId` (GitLab IID), `title`, `description`, `startDate`, `dueDate`, and `state` fields.

9. **GitLab errors are sanitized:** When GitLab returns an error (e.g., authentication failure, project not found), the response sanitizes the error and does not expose internal details, stack traces, or provider-specific messages.

10. **Architecture follows Issues pattern:** Endpoint validation, layer boundaries, domain/orchestration/integration separation, and logging patterns match the existing Issues capability.

## Dependencies and Assumptions

- **External dependencies:**
  - GitLab REST API project milestones endpoint (`GET /projects/:id/milestones`) is stable and supports `state`, `search`, and `iids[]` filters.
  - Application configuration provides a valid, authenticated GitLab project ID in `application-local.yml`.

- **Assumptions:**
  - The shared milestone model/DTO foundation in the repository is sufficient for this endpoint; if extension is needed, it is minimal and justified.
  - Clients will not send extremely large `milestoneIds` lists (no specific upper limit defined yet; this is a performance consideration for future work if needed).
  - The configured project remains static during the application lifetime (no runtime project switching).

## Open Questions

- Should future milestone operations (e.g., create, get-single) share the same `/api/milestones/*` path structure, or will they be scoped differently?
- If the shared milestone model proves insufficient during implementation, what extension mechanism (inheritance, composition, DTO wrapping) is preferred?
