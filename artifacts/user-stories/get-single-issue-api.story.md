# User Story: Get Single Issue API — Story 1 (Issue Details Pass-Through)

**Feature name:** `get-single-issue-api`
**Story date:** `2026-04-08`

## Business Goal

Provide delivery managers and agile teams with detailed, real-time information about individual work items (GitLab issues) so they can drill down from flow summaries and understand the full context of aging or blocked work.

## Problem Statement

Teams currently have visibility into open issues only as aggregated lists. To make informed prioritization and unblocking decisions, they need to retrieve full details about a single issue—including title, description, state, labels, assignees, milestone, and timestamps—without requiring direct GitLab access.

## User Story

As a delivery manager or agile team member,
I want to request detailed information for a single work item by its issue identifier,
so that I can understand its current state, ownership, priority, and scope without navigating GitLab directly.

## Locked Request Constraints

These constraints are non-negotiable and must be respected during architecture and implementation:

- **API constraint:** Implement only `GET /api/issues/{issueId}` (Story 1). Label history/changeSets implementation is deferred to Story 2.
- **Public contract constraint:** The public `issueId` path parameter maps exclusively to GitLab `issue_iid` from the configured project only. Clients must not supply project identifiers.
- **Architecture constraint:** This is a pass-through capability with no domain logic yet. Include only two layers: integration (fetch raw GitLab data) and orchestration (map and return client-facing response).
- **DTO constraint:** Use dedicated single-issue detail types. Do not overload existing search-issue DTOs/models or mix search-specific fields with single-detail fields.
- **Response payload constraint:** Include `changeSets` as a present, empty array `[]` in Story 1 as a reserved placeholder for future label history. Do not populate or implement real changeSets mapping in Story 1.
- **Integration boundary constraint:** Integration layer must fetch issue details only and must NOT build the final client-facing response. Integration must not call the label events endpoint (`GET /projects/:id/issues/:issue_iid/resource_label_events`).
- **Mapping constraint:** Client-facing mapping from raw GitLab DTOs must happen in the orchestration layer or an orchestration-facing mapper.
- **Field mapping constraint:** All field mappings must align with official GitLab REST API docs for `GET /projects/:id/issues/:issue_iid`. Defaults and null handling are mandatory: `labels: []` when empty or missing, `assignees: []` when empty or missing, `milestone: null` when missing, `description` nullable, `closedAt` nullable, `changeSets: []` always.
- **Error handling constraint:** If issue-details fetch fails, return the standard sanitized error response. No partial response is allowed.
- **Extensibility constraint:** Architecture must reserve clean room for future changeSets expansion (label history) in Story 2 without requiring backward-incompatible response redesign.

## Business Context and Constraints

- **Primary users:** Delivery managers and agile team members using GitlabFlow to track delivery flow and work-item status.
- **Primary stakeholder:** Product leadership and flow orchestration domain teams.
- **Important terminology:** 
  - *Work item:* GitLab issue identified by `issue_iid` within the configured project.
  - *Issue detail:* Full metadata about a single work item (title, description, state, labels, assignees, milestone, timestamps).
  - *ChangeSets (placeholder):* Reserved field for label history in Story 2; present but empty in Story 1.
- **Business-facing performance expectation:** Response should be delivered in real time with no noticeable latency to the user; no async or deferred loading.
- **Security expectation:** No sensitive fields or over-disclosure beyond what the authenticated GitLab user would see on the connected project.

## Scope

### In Scope

- Implement `GET /api/issues/{issueId}` endpoint in the orchestration layer.
- Fetch issue details from GitLab's `GET /projects/:id/issues/:issue_iid` endpoint using the configured project.
- Map GitLab issue fields to client-facing response (title, description, state, labels, assignees, milestone, createdAt, updatedAt, closedAt, issueId).
- Return response with `changeSets: []` as an empty placeholder array.
- Handle null/missing fields correctly: empty label array, empty assignee array, null milestone, nullable description, nullable closedAt.
- Return sanitized error response on fetch failure (no partial data).
- Document integration boundary: integration layer fetches only; orchestration layer maps and returns.

### Out of Scope

- Story 2 (label history/changeSets population via GitLab label events endpoint).
- Domain-level business logic or calculations (aging metrics, flow state inference, etc.).
- Complex issue filtering, search, or multi-issue operations (that is first-issues-api).
- Issue creation, update, or deletion operations.
- Milestone or assignee manipulation.
- Security or role-based access control beyond what GitLab enforces.

## Acceptance Criteria

Use numbered, observable outcomes:

1. **Endpoint exists and responds:** When a client calls `GET /api/issues/{issueId}`, the endpoint exists, accepts the request, and returns an HTTP 200 response with a JSON body.

2. **Correct field mapping:** The response includes all required fields (`issueId`, `title`, `description`, `state`, `labels`, `assignees`, `milestone`, `createdAt`, `updatedAt`, `closedAt`, `changeSets`) mapped correctly from GitLab issue details.

3. **ChangeSets present but empty:** The response always contains `changeSets: []` as an empty array in Story 1; never omitted.

4. **Null/default handling:** When GitLab returns empty or missing fields, the response respects the defined defaults: `labels: []`, `assignees: []`, `milestone: null`, `description: null` if not provided, `closedAt: null` if not provided.

5. **Assignees array structure:** Each assignee in the response includes `id`, `username`, and `name` fields matching GitLab's original structure.

6. **Milestone object structure:** When populated, milestone includes `id`, `milestoneId`, `title`, `state`, and `dueDate` fields.

7. **No label events call:** Integration layer does not call the label events endpoint in Story 1; label history is reserved for Story 2.

8. **Error response on failure:** If GitLab issue-details fetch fails, the endpoint returns a sanitized error response (no partial data, no internal stack traces).

9. **Dedicated types in place:** Implementation uses dedicated single-issue detail DTOs/models, separate from existing search-issue models, to enable clean Story 2 expansion.

10. **Integration boundary respected:** Integration layer responsibility is fetch only; orchestration layer responsibility is mapping and response assembly.

## Dependencies and Assumptions

- **External dependency:** GitLab REST API is available and responding with issue details for the configured project.
- **Assumption:** Client calling the endpoint has valid authentication and the same GitLab permissions as the service account.
- **Assumption:** The issueId supplied by the client is valid GitLab `issue_iid` format (positive integer).
- **Assumption:** Story 2 (label events) will follow this Story 1, allowing changeSets to be populated without response contract change.

## Open Questions

- None identified at this stage. Locked constraints provide sufficient detail for architecture and implementation planning.
