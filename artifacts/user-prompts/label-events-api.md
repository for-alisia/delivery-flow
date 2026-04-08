Hi Team,

This is story 2 of 2 for the full single-issue details capability.

Story 1, `get-single-issue-api.md`, introduced:

```http
GET /api/issues/{issueId}
```

and reserved:

```json
{
  "changeSets": []
}
```

Now we need to implement the real `changeSets` content from GitLab label events.

## Goal

Populate `changeSets` in the existing `GET /api/issues/{issueId}` response using GitLab resource label events.

The public endpoint stays the same:

```http
GET /api/issues/{issueId}
```

The final response should keep all issue detail fields from Story 1 and replace the placeholder empty `changeSets` array with mapped label-event history when events exist.

## Expected Final Response Shape

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
  "changeSets": [
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
  ]
}
```

## Mapping To GitLab REST API

### Main Issue Details Source

Continue using the Story 1 issue details source:

```http
GET /projects/:id/issues/:issue_iid
```

Important:

- Our public `GET /api/issues/{issueId}` path parameter maps to GitLab `issue_iid`.
- The configured GitLab project remains the only project source. Clients must not pass project id, project path, or project URL.

### Label History Source

Add and use:

```http
GET /projects/:id/issues/:issue_iid/resource_label_events
```

Important:

- Confirm exact GitLab endpoint path, parameters, pagination behavior, and response fields against official GitLab REST API docs before implementation.
- `changeSets` is built only from label events in this story.
- Future stories may add other event sources such as milestone, state, or weight changes, but this story should not implement those.
- Change should implement an interface as in future we will have multiple different event sources and changeSet and change should work as far as they implement the correct interface.

## Field Mapping

| Our field | Source endpoint | GitLab field | Optional |
|---|---|---|---|
| `changeSets` | label events | aggregated from `resource_label_events[]` | no, but may be empty |
| `changeSets[].changeType` | label events | `action` | no |
| `changeSets[].changedBy` | label events | `user` | no |
| `changeSets[].changedBy.id` | label events | `user.id` | no |
| `changeSets[].changedBy.username` | label events | `user.username` | no |
| `changeSets[].changedBy.name` | label events | `user.name` | no |
| `changeSets[].change.field` | label events | constant mapped value: `label` | no |
| `changeSets[].change.id` | label events | `label.id` | no |
| `changeSets[].change.name` | label events | `label.name` | no |
| `changeSets[].changedAt` | label events | `created_at` | no |

## Layering Rules

- Integration layer must expose a separate method for fetching issue label events.
- Integration layer must not combine GitLab issue details and GitLab label events into the final API response.
- Orchestration layer should call both methods and combine them into the final response contract.
- Mapping into the client-facing `changeSets` contract should happen at orchestration layer or an orchestration-facing mapper, not inside the GitLab integration adapter.
- Do not change the Story 1 issue details response contract except to populate `changeSets`.
- Do not introduce a separate public endpoint for label events unless explicitly requested later.

## Error Handling

- Partial failure is not allowed.
- If fetching issue details fails, the whole request returns the standard sanitized error response.
- If fetching label events fails, the whole request returns the standard sanitized error response.
- If either GitLab call fails, the endpoint must not return partial issue details with missing `changeSets`.

## Change Sets Scope

- `changeSets` currently represents label history only.
- `changeType` should currently map only GitLab label event actions like `add` and `remove`.
- `change.field` should currently always be `label`. (but expandable in futur)
- `changeSets` should not be sorted by our API. Return events in the order provided by GitLab unless existing project conventions require otherwise.
- Empty label history should return `changeSets: []`.
- Unknown or unsupported future event sources are out of scope.

## Notes

- Reuse the user/person representation from Story 1 assignees where it makes sense. We do not need separate user models for assignees and `changedBy` if a shared internal model keeps the design simpler.
- Keep the contract backward compatible with Story 1: clients that already read issue details should continue to work.
- Add or update tests at the right levels
