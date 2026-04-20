Hi Team,

Please implement one more pass-through feature for `flow-orchestrator`: ability to update an existing issue.

We already have issue creation support. Now we need issue update support under the same capability base path.

## API Contract

Add a new endpoint:

```http
PATCH /api/issues/{issueId}
```

`issueId` is the same project-scoped issue identifier already used by the existing issues API.

Request body:

```json
{
  "title": "Updated title",
  "description": "Updated description",
  "addLabels": ["bug"],
  "removeLabels": ["test"]
}
```

## Request Rules

- At least one updatable field must be provided.
- A field counts as provided only when:
  - `title` is non-null
  - `description` is non-null
  - `addLabels` is non-null and non-empty
  - `removeLabels` is non-null and non-empty
- `title`, when provided:
  - must be non-blank
  - min length: `3`
  - max length: `255`
- `description`, when provided:
  - max length: `1_000_000`
  - empty string is allowed and should mean clear/update description intentionally
  - `null` means no change
- `addLabels`, when provided:
  - max `10` items
  - each item must be non-blank
- `removeLabels`, when provided:
  - max `10` items
  - each item must be non-blank
- Reject a request when the same label appears in both `addLabels` and `removeLabels`.

## Response Contract

- Response must reuse the existing `IssueSummaryDto`.
- Do not introduce a new response DTO for this feature.
- Reuse existing orchestration and mapping structures wherever possible.

## Creation Alignment

As part of the same task, align issue creation validation as well:

- apply title min/max limits to create issue
- apply description max limit to create issue
- apply max labels limit to create issue labels

Use shared high-level configuration properties from `application.yml` so the same limits can be reused across create and update.

## Configuration

Make the following limits configurable:

- title min length
- title max length
- description max length
- max labels per request field

These properties should be used by both create and update flows.

## GitLab Integration

Use the official GitLab update issue API.

Important mapping requirements:

- local API stays `PATCH /api/issues/{issueId}`
- downstream GitLab call uses the documented issue update endpoint
- map fields as follows:
  - `title` -> `title`
  - `description` -> `description`
  - `addLabels` -> `add_labels`
  - `removeLabels` -> `remove_labels`

When adding labels, use GitLab additive label update behavior. Do not replace all labels unless explicitly required.

Return the updated issue mapped back to `IssueSummaryDto`.

## Verification Expectations

- update `flow-orchestrator/http/issues.http`
- add or update unit tests
- add or update integration/component tests where needed
- add or update Karate coverage for the API contract
- run the repository-supported verification workflow required for `flow-orchestrator`

Please check the official GitLab documentation carefully before implementation.
