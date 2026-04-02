# Feature Request: Create Issue API

**Feature name:** `create-issue-api`

## Goal

Create one more pass-through MVP feature to exercise the agentic workflow when the repository is already seeded with the initial story and skeleton.

We already have the first "fetch issues" feature. The next MVP step is to let clients create GitLab issues through `flow-orchestrator`, using the same general principles and quality bar as the existing issues feature.
This feature also includes renaming the existing fetch endpoint so the API contract stays REST-consistent.

## Business Context

- This is an addition to the recently created fetch-issues capability.
- Clients want to start creating work items in GitLab through our API.
- For now this is intentionally a pass-through feature only.
- We do **not** want to add new business/domain orchestration logic yet.
- We work against the same GitLab project configured in `application-local.yml`; clients must not choose the project per request.

## Key Expectations

- Follow the same architecture and conventions as the existing issues feature where it makes sense.
- Keep the implementation maintainable and easy to extend later.
- Reuse shared validation, integration error handling, logging, test strategy, and documentation conventions already present in the repository.
- Verify GitLab endpoint details against the official GitLab API docs instead of assuming them.

## Locked Requirements

1. Add an API that creates a GitLab issue in the single configured project used by `flow-orchestrator`.
2. The client must **not** pass GitLab project id, project path, or project URL in the request.
3. The configured project from `application-local.yml` is the only source of truth for the target project.
4. This feature must stay pass-through for MVP: validate input, call GitLab, map response, return sanitized errors.
5. `POST /api/issues` must be used for issue creation.
6. The previously delivered fetch endpoint must be renamed from `POST /api/issues` to `POST /api/issues/search`.
7. The rename is part of this feature scope and must be reflected consistently in controller mappings, tests, smoke coverage, and `.http` examples.
8. The create request must be a **POST** with a mandatory JSON payload.
9. Proposed request payload:

```json
{
  "title": "string",
  "description": "string",
  "labels": ["array", "of", "string"]
}
```

10. Validation expectations at the API boundary:
   - `title` is mandatory.
   - `title` must be non-null and non-blank.
   - `description` is optional.
   - `labels` is optional.
   - If `labels` is provided, it should contain only non-blank strings.
   - Malformed JSON must return a clear validation error.
11. Research and confirm the correct GitLab API contract for issue creation. Current expectation is:
   - required GitLab field: `title`
   - optional GitLab fields used by us: `description`, `labels`
12. Successful response must return **201 Created**.
13. Success response must contain:

```json
{
  "id": 123456,
  "title": "string",
  "description": "string or null",
  "labels": ["array", "of", "string"]
}
```

14. `id` must be the GitLab issue **id**, not **iid**.
15. In case of error, return an explicit user-friendly message.
16. Reuse the existing shared error contract and exception handling style instead of inventing a new error format.

## Error Handling Expectations

- Validation failures should return the existing API validation error shape with clear messages.
- Malformed JSON should return the existing validation error shape.
- GitLab authentication, not-found, rate-limit, and general integration failures should be mapped through the shared integration error flow unless a better shared approach already exists and is justified.
- Error responses should stay sanitized and should not expose secrets or raw token/config values.

## Out Of Scope

- No project selection per request.
- No issue update, delete, or state transition support.
- No milestone, assignee, epic, or other create-time fields unless they are required for this feature.
- No new domain workflow logic beyond pass-through validation and mapping.
- No support for multiple GitLab projects in this slice.
- No requirement to keep the old `POST /api/issues` fetch route alive after the rename.

## Expected API Examples

### Sample Request

```json
{
  "title": "Add release readiness dashboard",
  "description": "We need a first MVP dashboard for release readiness checks.",
  "labels": ["mvp", "dashboard"]
}
```

### Expected Success Response

```json
{
  "id": 123456,
  "title": "Add release readiness dashboard",
  "description": "We need a first MVP dashboard for release readiness checks.",
  "labels": ["mvp", "dashboard"]
}
```

### Expected Validation Error Example

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    "title must not be blank"
  ]
}
```

## Other questions to clarify

1. When `description` is omitted, the response may include `"description": null` or can be omitted
2. GitLab creates missing labels automatically when labels are passed during issue creation. This is acceptable for MVP
