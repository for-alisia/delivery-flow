Hi Team,

Client needs more extensive information about a single GitLab issue, so we decided to implement:

```http
GET /api/issues/{issueId}
```

This is story 1 of 2 for the full single-issue details capability. This is still pass-through feature, no need to implement domain logic yet. 2 layers here - intergation and orchestration.

## Full Intended Capability

The final capability should return detailed issue data and issue change history (for now just for label related events):

- Story 1, this prompt: return detailed single issue data from GitLab issue details.
- Story 2, separate prompt `label-events-api.md`: populate `changeSets` from GitLab label events.

Important architectural note:

- Agents must design Story 1 knowing that `changeSets` is coming in Story 2.
- Do not make architectural choices that make change history hard to add later.
- Do not overload the existing search issue DTO/model if that would force a flat model or mix search-specific fields with detailed single-issue fields.
- Prefer dedicated single-issue detail types, with a reserved `changeSets` field.
- Story 1 must return `changeSets: []` as an empty array placeholder.
- Story 1 must not call GitLab label events yet.
- Story 1 must not implement real `changeSets` mapping yet.

## Story 1 Expected Response

For now, the client expects the response shape below. `changeSets` is intentionally present but empty until Story 2.

```json
{
  "issueId": 7,
  "title": "Issue title",
  "description": "Issue description",
  "state": "opened",
  "labels": ["backend", "api", "gitlab"],
  "assignees": [
    {
      "id": 9,
      "username": "alice",
      "name": "Alice Example"
    }
  ],
  "milestone": {
    "id": 11,
    "milestoneId": 3,
    "title": "v2.3.0",
    "state": "active",
    "dueDate": "2026-04-15"
  },
  "createdAt": "2024-06-01T12:00:00Z",
  "updatedAt": "2024-06-01T12:00:00Z",
  "closedAt": null,
  "changeSets": []
}
```

## Mapping To GitLab REST API

### Main Issue Details Source

Use:

```http
GET /projects/:id/issues/:issue_iid
```

Important:

- Our public `GET /api/issues/{issueId}` path parameter maps to GitLab `issue_iid`.
- The configured GitLab project remains the only project source. Clients must not pass project id, project path, or project URL.
- Confirm exact GitLab field names against official GitLab REST API docs before implementation.

### Label History Source - Not In This Story

Do not call this endpoint in Story 1:

```http
GET /projects/:id/issues/:issue_iid/resource_label_events
```

This endpoint is reserved for Story 2 in `label-events-api.md`.

## Story 1 Field Mapping

| Our field | Source endpoint | GitLab field | Optional |
|---|---|---|---|
| `issueId` | issue details | `iid` | no |
| `title` | issue details | `title` | no |
| `description` | issue details | `description` | yes, may be `null` |
| `state` | issue details | `state` | no |
| `labels` | issue details | `labels` | no, but may be empty |
| `assignees` | issue details | `assignees` | no, but may be empty |
| `assignees[].id` | issue details | `assignees[].id` | no when assignee exists |
| `assignees[].username` | issue details | `assignees[].username` | no when assignee exists |
| `assignees[].name` | issue details | `assignees[].name` | no when assignee exists |
| `milestone` | issue details | `milestone` | yes, may be `null` |
| `milestone.id` | issue details | `milestone.id` | no when milestone exists |
| `milestone.milestoneId` | issue details | `milestone.iid` | no when milestone exists |
| `milestone.title` | issue details | `milestone.title` | no when milestone exists |
| `milestone.state` | issue details | `milestone.state` | no when milestone exists |
| `milestone.dueDate` | issue details | `milestone.due_date` | yes, may be `null` |
| `createdAt` | issue details | `created_at` | no |
| `updatedAt` | issue details | `updated_at` | no |
| `closedAt` | issue details | `closed_at` | yes, may be `null` |
| `changeSets` | reserved for Story 2 | always return `[]` in Story 1 | no, empty array |

## Future Change Sets Contract

The fields below are not implemented in Story 1, but the model/response design must leave room for them in Story 2:

```json
{
  "changeType": "add",
  "changedBy": {
    "id": 1,
    "username": "root",
    "name": "Administrator"
  },
  "change": {
    "field": "label",
    "id": 73,
    "name": "backend"
  },
  "changedAt": "2024-06-01T12:00:00Z"
}
```

Future constraints:

- `changeSets` will initially represent label history only.
- `changeType` will map GitLab label event actions like `add` and `remove`.
- `change.field` will initially always be `label`.
- In the future, `changeSets` may be expanded with milestone, state, weight, or other change sources while keeping the public contract backward compatible.

## Layering Rules

- Integration layer must expose a method for fetching single issue details.
- Integration layer must not call or combine label events in Story 1.
- Integration layer must not build the final client-facing API response.
- Mapping from raw GitLab DTOs into the client-facing response should happen at orchestration layer or an orchestration-facing mapper, not inside the GitLab integration adapter.
- If placeholder `changeSets` types are introduced now, keep them minimal and aligned with the future contract above.

## Error Handling

- If fetching issue details fails, the whole request returns the standard sanitized error response.
- No partial response is allowed.
- Since Story 1 does not call label events, label-event failures are not applicable yet.

## Notes

- Empty or missing `labels` should return `labels: []`.
- Empty or missing `assignees` should return `assignees: []`.
- A missing milestone should return `milestone: null`.
- `description` may be `null`.
- `closedAt` should be `null` when the issue is not closed.
- `changeSets` must be present and must be `[]` in Story 1.
