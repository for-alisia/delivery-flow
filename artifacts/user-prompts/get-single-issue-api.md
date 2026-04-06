Hi Team,

Client needs more extensive information about the issue, so we decided to implement an endpoint, which will return more details ebout single issue. What our client whats to call:

```
GET /api/issues/{issueId}
```

And the client expects to receive the following information:

```
{
  "issueId": "string",
  "title": "string",
  "description": "string",
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
  "closedAt": null, // optional, null if the issue is still open
  "changeSets": [
    {
      "changeType": "add" // enum, can be "add","remove" for now
      "changedBy": { // Here and in assignee and in creator no need to have separate models, 1 should cover
        "id": 1,
        "username": "root",
        "name": "Administrator",
      },
      "change": {
        "field": "label", // enum, for now label only, in future will be added milestone, weight and so on.
        "id": 73,
        "name": "backend"
      },
      "changedAt": "2024-06-01T12:00:00Z"
    }
  ]
}

```

### Mapping to GitLab REST API

The contract above stays as-is. The implementation should map it to GitLab fields as follows:

#### Main issue details source

Use:

- `GET /projects/:id/issues/:issue_iid`

Important:

- our public `GET /api/issues/{issueId}` path parameter should map to GitLab `issue_iid`

#### Label history source

Use:

- `GET /projects/:id/issues/:issue_iid/resource_label_events`

For now, `changeSets` is built only from label events. In future this should be expanded with other event sources like milestone/state/weight when needed.

### Field Mapping

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

### Layering Rules

- Integration layer should not combine GitLab responses into the final API response.
- Integration layer should expose separate methods for fetching single issue details and fetching issue label events.
- Orchestration layer should call both methods and combine them into the final response contract.
- Mapping from raw GitLab DTOs into the final client-facing response should happen at orchestration layer or an orchestration-facing mapper, not inside the GitLab integration adapter.

### Error Handling

- Partial failure is not allowed.
- If fetching issue details fails, the whole request returns error.
- If fetching label events fails, the whole request returns error.
- If either GitLab call fails, the endpoint should not return partial data.

### Change Sets Scope

- `changeSets` currently represents label history only.
- `changeType` should currently map only GitLab label event actions like `add` and `remove`.
- `change.field` should currently always be `label`.
- In future, `changeSets` can be expanded with additional event types, for example milestone changes or state changes, but this contract should stay backward compatible.

### Notes

- `changeSets` should be returned in deterministic order. Recommended order: ascending by `changedAt`.
- `changeSets` should not be sorted from the latest event to the oldest one.
- Empty label history should return `changeSets: []`.
- A missing milestone should return `milestone: null`.
- `closedAt` should be `null` when the issue is not closed.
