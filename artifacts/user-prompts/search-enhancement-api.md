Hi Team,

Client wants us to extend the existing `POST /api/issues/search` endpoint.

We need to add a new filter field: `audit`.
It should be an array and, for now, it may contain only one supported value: `"label"`.
Please model it in a way that is ready for future enum-like expansion, because later we expect values like milestone as well.

How it should work:
- If `audit` is not empty, enhance each returned search item with `changeSets` for the requested audit event type.
- For now only `label` is supported, so for this story `changeSets` should be built from label events.
- Flow should be: execute search as today -> after issues are returned from GitLab -> fetch label events for each returned issue -> merge label-event data into each issue as `changeSets` -> return response.
- Please check how label-event enrichment was already implemented for the single issue endpoint and reuse the same modelling where it makes sense.
- When `audit` contains `label`, each search item includes `changeSets`.
- When `audit` is absent, null, or empty, no extra GitLab event calls are made.

Important notes you MUST follow:
- Orchestration decides on calls and mapping logic. REST DTOs should not introduce business modelling unless they are only transport carriers. Main modelling should stay in orchestration models.
- Everything that can be reused from the model perspective MUST be reused. You do not need separate `change` or `changeSet` models if existing ones already carry the same data.
- You should extend current `Issue` model with optional `changeSets` field, but it should be null when audit is not requested. This way we can reuse the same model for all search responses, and it will be up to the client to check if changeSets are present or not.
- This functionality is performance-risky. You already existing AsyncComposer for handling multiple parallel calls.
- Limit the maximum requested page size to `40`. Clients agreed to that limit, so it is safe to enforce it. At the same time update default per page to 20 (as agreed with clients)
- Client may provide `audit` as missing, `null`, or empty array. All of these are valid and should behave the same.
- If any GitLab call needed for this enrichment fails, treat it as full failure for now and return an error to the client. This is not the final long-term strategy, but it is the expected behavior for this step.
- We update only `POST /api/issues/search`.
- You are allowed to introduce a dedicated REST DTO for the enriched search response if needed, but the orchestration `Issue` model should remain reusable for all endpoints (you are allowed to enhance it), including delete and search without `audit`.
- If client sends unsupported values in `audit` return explicit validation error. null values inside can be just omitted from the list of audit values unless other values are correct

Payload examples:

Default search request, no audit:
```json
{
  "pagination": {
    "page": 1,
    "perPage": 20
  },
  "filters": {
    "state": "opened"
  }
}
```

Search request with audit enrichment:
```json
{
  "pagination": {
    "page": 1,
    "perPage": 20
  },
  "filters": {
    "state": "opened",
    "audit": ["label"]
  }
}
```

Search response item example when audit contains label:
```json
{
  "id": 101,
  "issueId": 17,
  "title": "Deploy failure",
  "description": "Step 3 failed",
  "state": "opened",
  "labels": ["bug"],
  "assignee": "john.doe",
  "milestone": "M1",
  "parent": null,
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
        "name": "bug"
      },
      "changedAt": "2026-01-15T09:30:00Z"
    }
  ]
}
```

Search response item example when audit is absent or empty:
```json
{
  "id": 101,
  "issueId": 17,
  "title": "Deploy failure",
  "description": "Step 3 failed",
  "state": "opened",
  "labels": ["bug"],
  "assignee": "john.doe",
  "milestone": "M1",
  "parent": null
}
```
