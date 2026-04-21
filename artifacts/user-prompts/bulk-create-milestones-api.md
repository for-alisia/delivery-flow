# Feature Request: Bulk Create Milestones API

**Feature name:** `bulk-create-milestones-api`

## Goal

Add the first milestone write capability to `flow-orchestrator` by allowing clients to create multiple GitLab project milestones in one API call.

This should stay implementation-light and enterprise-safe: validate the batch request, create milestones in the single configured GitLab project, return a clear per-item outcome, and keep the structure ready for future milestone endpoints.

## Business Context

- Milestones are already a core business concept in this product: they represent the GitLab delivery cycle.
- The repository already contains shared milestone model / DTO foundations, but no milestone endpoints yet.
- Clients want to seed release or sprint plans faster by creating several milestones in one request instead of making many separate API calls.
- This feature should operate only on the single configured GitLab project used by `flow-orchestrator`.
- This is a project-milestone feature, not a group-milestone feature.

## Key Expectations

- Follow the same architecture and repository conventions as the implemented issues capabilities where that makes sense.
- Reuse the existing configured-project approach; clients must not choose project id or path per request.
- Reuse shared validation, logging, integration error handling, and test strategy patterns already present in the repository.
- Reuse the existing milestone foundation if appropriate. If the shared milestone model/DTO is currently too narrow for this feature, extend it carefully instead of creating a parallel milestone representation without justification.
- Verify GitLab milestone endpoint details against the official GitLab API docs instead of assuming them.

## Locked Requirements

1. Add an API that creates up to 12 GitLab **project milestones** in the single configured project used by `flow-orchestrator`.
2. The client must **not** pass GitLab project id, project path, group id, or project URL in the request.
3. The configured project from `application-local.yml` is the only source of truth for the target project.
4. This feature must stay orchestration-light for MVP: validate input, call GitLab, map response, and return sanitized results/errors.
5. Use `POST /api/milestones/bulk` for this feature.
6. The request must be a **POST** with a mandatory JSON payload.
7. Recommended request payload shape:

```json
{
  "milestones": [
    {
      "title": "Release 1.0",
      "description": "Initial MVP delivery milestone",
      "startDate": "2026-05-01",
      "dueDate": "2026-05-15"
    }
  ]
}
```

8. Validation expectations at the API boundary:
   - `milestones` is mandatory.
   - `milestones` must contain at least 1 item and at most 12 items.
   - `title` is mandatory for each item.
   - `title` must be non-null and non-blank after trimming.
   - `title` must have minimum length 3.
   - Duplicate milestone titles inside the same request must be rejected before any GitLab call is made.
   - `description` is optional and may be null or empty.
   - `startDate` is optional.
   - `dueDate` is optional.
   - `startDate` and `dueDate`, when present, must use ISO date format `YYYY-MM-DD`.
   - If both `startDate` and `dueDate` are present, `dueDate` must be later than `startDate`.
   - Malformed JSON must return a clear validation error.
9. Research and confirm the correct GitLab API contract for milestone creation. Current expectation is:
   - GitLab exposes **single milestone creation** for projects via `POST /projects/:id/milestones`.
   - GitLab does **not** expose a documented bulk-create project milestones endpoint.
   - GitLab fields used by this feature are `title` (required), `description` (optional), `start_date` (optional), and `due_date` (optional).
10. Because GitLab does not provide a documented bulk-create milestone endpoint, this feature must implement bulk creation by issuing one GitLab create request per milestone against the configured project.
11. The batch must be processed in request order.
12. The API must make partial outcomes explicit. Do not hide partially created milestones behind a single generic error.
13. If batch processing is attempted, return `200 OK` with a bulk results payload, even when some items fail at the GitLab layer. Do not use `201 Created` for this bulk endpoint.
14. Recommended success/processing response shape:

```json
{
  "summary": {
    "requested": 2,
    "created": 1,
    "failed": 1
  },
  "results": [
    {
      "index": 0,
      "status": "CREATED",
      "milestone": {
        "id": 101,
        "milestoneId": 7,
        "title": "Release 1.0",
        "description": "Initial MVP delivery milestone",
        "state": "active",
        "startDate": "2026-05-01",
        "dueDate": "2026-05-15"
      }
    },
    {
      "index": 1,
      "status": "FAILED",
      "title": "Release 1.1",
      "error": {
        "code": "INTEGRATION_VALIDATION_ERROR",
        "message": "GitLab rejected milestone creation"
      }
    }
  ]
}
```

15. If the request is valid and processing is attempted, return a response that preserves per-item outcomes instead of failing the whole batch response shape on the first upstream error.
16. If the entire request is invalid before execution starts, reject it with the existing validation error approach rather than a bulk results payload.
17. Return sanitized integration errors. Do not expose GitLab token values, raw secrets, or internal configuration.
18. Use maintainable naming and structure that leaves room for future milestone capabilities such as list, get-single, update, and milestone-scoped issue retrieval.
19. Confirm whether the existing shared `Milestone` / `MilestoneDto` should be expanded for this feature, because the current foundation appears to omit `description` and `startDate`, both of which are relevant for create responses.
20. Do not invent undocumented GitLab provider limits for title or description length. If an exact provider maximum is not confirmed from official documentation or current upstream source for the target version, do not hardcode a speculative API-boundary max length in this slice without explicit approval.

## Error Handling Expectations

- Validation failures should return the existing API validation error shape with clear messages.
- Malformed JSON should return the existing validation error shape.
- Per-item GitLab validation or conflict failures should be captured as sanitized per-item failures in the bulk result when processing has already started.
- If GitLab rejects a milestone because the title already conflicts with an existing milestone in the project hierarchy, surface that as a sanitized per-item failure.
- If the request cannot be processed at all because of global configuration/authentication/integration failure, reuse the existing shared integration error flow unless a better shared approach already exists and is justified.
- Error responses must stay sanitized and must not expose secrets or raw provider configuration.

## Out Of Scope

- No group milestone support in this feature.
- No project or group selection per request.
- No milestone update, delete, close, activate, or get/list support in this slice.
- No issue assignment to milestones in this slice.
- No automatic rollback of already created milestones when later items fail.
- No retry, deduplication against existing GitLab milestones, or idempotency-key design unless already supported by a shared pattern and clearly justified.

## Expected API Examples

### Sample Request

```json
{
  "milestones": [
    {
      "title": "Release 1.0",
      "description": "Initial MVP delivery milestone",
      "startDate": "2026-05-01",
      "dueDate": "2026-05-15"
    },
    {
      "title": "Release 1.1",
      "description": "",
      "startDate": "2026-05-16",
      "dueDate": "2026-05-31"
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
    "milestones must contain between 1 and 12 items",
    "milestones[0].title must not be blank"
  ]
}
```

### Expected Processed Response Example

```json
{
  "summary": {
    "requested": 2,
    "created": 2,
    "failed": 0
  },
  "results": [
    {
      "index": 0,
      "status": "CREATED",
      "milestone": {
        "id": 101,
        "milestoneId": 7,
        "title": "Release 1.0",
        "description": "Initial MVP delivery milestone",
        "state": "active",
        "startDate": "2026-05-01",
        "dueDate": "2026-05-15"
      }
    },
    {
      "index": 1,
      "status": "CREATED",
      "milestone": {
        "id": 102,
        "milestoneId": 8,
        "title": "Release 1.1",
        "description": "",
        "state": "active",
        "startDate": "2026-05-16",
        "dueDate": "2026-05-31"
      }
    }
  ]
}
```

## Other Questions To Clarify

1. If you want a raw JSON array as the root payload instead of `{ "milestones": [...] }`, that is possible, but the wrapped request object is recommended for forward compatibility.
2. If you want strict API-boundary maximum lengths for `title` or `description`, those should be treated as explicit product rules unless you first confirm a target GitLab-version-specific limit from official documentation or source.
