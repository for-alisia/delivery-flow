# Feature Request: Milestones Search API

**Feature name:** `milestones-search-api`

## Goal

Add the first milestone endpoint to `flow-orchestrator` by allowing clients to retrieve milestones from the single configured GitLab project with a small filter payload.

This should stay a pass-through MVP feature: validate the request, fetch milestones from GitLab, map them into our public response, and return sanitized errors.

## Business Context

- This is the first endpoint for the new milestones capability.
- Milestones are already a core business concept in the product: they represent a GitLab delivery cycle.
- The repository already contains shared milestone model / DTO foundations, but no milestone endpoints yet.
- Clients want a simple way to retrieve milestones for planning and delivery views without passing the project on every request.
- This is a project-milestone feature only, not a group-milestone feature.

## Key Expectations

- Follow the same architecture and conventions as the implemented issues capability where that makes sense.
- Reuse the single configured GitLab project from `application-local.yml`; clients must not choose the project per request.
- Reuse shared validation, logging, integration error handling, and test strategy patterns already present in the repository.
- Verify GitLab milestone endpoint details against the official GitLab API docs instead of assuming them.
- Reuse the existing milestone foundation if appropriate. If the current shared milestone model/DTO is too narrow for this endpoint, extend it carefully instead of creating a parallel milestone representation without justification.

## Locked Requirements

1. Add an API that returns milestones from the single configured GitLab project used by `flow-orchestrator`.
2. The client must **not** pass GitLab project id, project path, or project URL in the request.
3. The configured project from `application-local.yml` is the only source of truth for the target project.
4. This feature must stay pass-through for MVP: validate input, call GitLab, map response, return sanitized errors.
5. Use `POST /api/milestones/search` for this feature.
6. Do **not** use `POST /api/milestones` for this search/list endpoint, because milestone creation is a likely future capability and we should avoid the routing conflict that happened earlier with issues.
7. The request body is optional. If the body is omitted, or `filters` is omitted, return the default milestone set.
8. Default behavior:
   - return all **active** project milestones from GitLab
   - no client-facing pagination is exposed in this slice
9. Recommended request payload shape:

```json
{
  "filters": {
    "state": "active",
    "titleSearch": "release",
    "milestoneIds": [1, 2, 3]
  }
}
```

10. Validation expectations at the API boundary:
    - `filters` is optional.
    - `state` is optional.
    - If `state` is provided, allowed values are exactly `active`, `closed`, and `all`.
    - `titleSearch` is optional.
    - If `titleSearch` is provided, it must be non-blank after trimming.
    - `milestoneIds` is optional.
    - If `milestoneIds` is provided, it must contain only positive integers.
    - If `milestoneIds` is provided, duplicate values must be rejected.
    - Malformed JSON must return a clear validation error.
11. Research and confirm the correct GitLab API contract for milestone listing. Current expectation is:
    - use project milestones API: `GET /projects/:id/milestones`
    - `state=active` returns active milestones
    - `state=closed` returns closed milestones
    - `iids[]` filters by GitLab milestone IID values
    - `search` matches milestone title or description
    - `title` is exact-title matching, not partial title search
12. Public request filter mapping:
    - `state=active` maps to GitLab `state=active`
    - `state=closed` maps to GitLab `state=closed`
    - `state=all` means omit the upstream `state` filter
13. `milestoneIds` in our request must map to GitLab `iids[]`, not database `id`.
14. `titleSearch` should map to GitLab `search` for MVP because GitLab documents partial search through `search`, while `title` is exact-match only.
15. Do not invent a stronger title-only substring contract than GitLab documents unless explicitly justified. If stricter title-only semantics are desired later, that should be treated as product logic rather than pure pass-through behavior.
16. The project milestones API page does not explicitly call out pagination, but GitLab REST API pagination rules still apply to listing endpoints, and live testing against the configured GitLab.com test project confirmed that the milestones endpoint honors `page` and `per_page` and returns pagination headers.
17. Even though our public API is not paginated in this slice, the implementation must fetch all pages from GitLab and aggregate the results before returning them.
18. Use `per_page=100` when calling GitLab list milestones and continue requesting pages until the result set is exhausted.
19. Successful response must return `200 OK`.
20. Recommended response shape:

```json
{
  "milestones": [
    {
      "id": 12,
      "milestoneId": 3,
      "title": "Release 1.0",
      "description": "Version",
      "startDate": "2026-05-01",
      "dueDate": "2026-05-15",
      "state": "active"
    }
  ]
}
```

21. Response field mapping:
    - `id` -> GitLab `id`
    - `milestoneId` -> GitLab `iid`
    - `title` -> GitLab `title`
    - `description` -> GitLab `description`
    - `startDate` -> GitLab `start_date`
    - `dueDate` -> GitLab `due_date`
    - `state` -> GitLab `state`
22. Response `state` should use GitLab milestone vocabulary (`active`, `closed`) to stay aligned with the existing shared milestone model and GitLab’s actual milestone resource shape.
23. Confirm whether the existing shared `Milestone` / `MilestoneDto` should be expanded for this endpoint, because the current foundation appears to omit `description` and `startDate`, both of which are required by this response.

## Error Handling Expectations

- Validation failures should return the existing API validation error shape with clear messages.
- Malformed JSON should return the existing validation error shape.
- GitLab authentication, not-found, authorization, rate-limit, and general integration failures should be mapped through the shared integration error flow unless a better shared approach already exists and is justified.
- Error responses must stay sanitized and must not expose secrets or raw provider configuration.

## Out Of Scope

- No group milestone support in this feature.
- No single-milestone endpoint in this slice.
- No milestone creation, update, delete, close, or activate operations in this slice.
- No milestone issues, merge requests, burndown events, or release aggregation in this slice.
- No client-facing pagination in this slice.
- No sorting or date-window filters unless they are explicitly requested in a future story.

## Expected API Examples

### Empty Request Uses Defaults

```http
POST /api/milestones/search
```

Response meaning:

- returns all active milestones from the configured project

### Sample Request

```json
{
  "filters": {
    "state": "all",
    "titleSearch": "release",
    "milestoneIds": [1, 2, 3]
  }
}
```

### Expected Success Response

```json
{
  "milestones": [
    {
      "id": 12,
      "milestoneId": 3,
      "title": "Release 1.0",
      "description": "Version",
      "startDate": "2026-05-01",
      "dueDate": "2026-05-15",
      "state": "active"
    }
  ]
}
```

### Expected Validation Error Example

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    "filters.state must be one of: active, closed, all",
    "filters.milestoneIds must contain only positive integers"
  ]
}
```
