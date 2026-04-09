# User Story: Search Enhancement API — Audit-Enriched Issue Search

**Feature name:** `search-enhancement-api`
**Story date:** `2026-04-09`

## Business Goal

Enable search results to be optionally enriched with audit change history (starting with label changes), so delivery managers can see how issues have evolved directly in search results without making additional API calls for each item.

## Problem Statement

The search endpoint returns current snapshots of issues. Managers performing bulk searches for audit or troubleshooting purposes must choose between searching without history context or fetching change history separately for each result. This creates friction and forces extra API calls. By embedding change history into search results when requested, managers gain immediate visibility into issue evolution within a single operation.

## User Story

As a delivery manager or auditor,
I want to retrieve a list of issues with their label change history included in a single search request,
so that I can understand how issues have evolved without making separate API calls and can audit workflow progression at scale.

## Locked Request Constraints

These constraints are non-negotiable and locked by Team Lead:

- **Endpoint:** Extend only `POST /api/issues/search`; do not create separate endpoints.
- **Audit filter field:** Add optional `audit` filter as an array of strings in the request payload.
- **Extensible enum:** Model `audit` as open for future values (e.g., `milestone`). For this story, only `"label"` is supported.
- **Supported values:** Accept `"label"` in the `audit` array. Reject requests with any other values with explicit validation error (null values within the array are omitted, not errors).
- **Payload semantics:** When `audit` is missing, `null`, or empty array, all three are equivalent: no enrichment, no extra GitLab calls.
- **Enrichment logic:** When `audit` contains `"label"`, populate `changeSets` in each search result using label-event history (via GitLab `resource_label_events` endpoint).
- **Data flow:** Execute search as today → after issues returned from GitLab → fetch label events for each returned issue in parallel → merge into results as `changeSets` → return response.
- **Model reuse:** Extend the existing orchestration `Issue` model with optional `changeSets` field (null when audit not requested); keep this model reusable across all endpoints (search with/without audit, single issue, delete).
- **ChangeSet design:** Reuse the same `changeSet` and `change` models from the label-events API; no separate models unless REST transport requires dedicated DTOs.
- **Layering:** Orchestration owns enrichment orchestration logic (multiple parallel calls, merging); REST DTOs are transport only; main business modeling stays in orchestration models.
- **Performance:** Use existing AsyncComposer for parallel label-event calls; do not make sequential calls.
- **Pagination limits:** Enforce maximum `perPage` of `40`; set default `perPage` to `20` (previously unspecified or higher).
- **Failure semantics:** If any enrichment call (label events for any issue) fails, treat as full failure and return error response to client. Partial success is not allowed in this iteration.
- **Response model:** Issue model used in response includes `changeSets` (populated when audit requested, null/omitted otherwise); all other search response fields remain unchanged from current search behavior.

## Business Context and Constraints

- **Primary users:** Delivery managers, auditors, and agile team members performing bulk issue searches to track delivery workflow and audit label-driven state transitions.
- **Primary stakeholder:** Product leadership and orchestration domain teams.
- **Important terminology:**
  - *Audit filter:* Optional search filter request field that specifies which types of change history to include in results.
  - *ChangeSet:* A sequence of label change records, each capturing when a label was added or removed, by whom, and when.
  - *Enrichment:* The process of fetching and merging label-event history into each search result.
- **Business-facing performance expectation:** Response should be delivered in real time with no noticeable latency to the user. Parallel fetching of label events must keep response time acceptable for typical search result sets (the audit enrichment itself should not significantly degrade response time beyond a baseline search).
- **Security expectation:** No sensitive fields beyond what the authenticated GitLab user would see on the connected project; label events visible only to users with permission to see the underlying issues and labels; no over-disclosure of internal state.

## Scope

### In Scope

- Extend `POST /api/issues/search` endpoint to accept optional `audit` filter field (array of strings).
- Validate `audit` filter: reject requests with unsupported audit types; allow `"label"` only in this story; treat null array elements as ignored (not errors).
- When `audit` contains `"label"`, fetch label-event history from GitLab for each returned search result in parallel using AsyncComposer.
- Map GitLab label events to client-facing `changeSet` structure (`changeType`, `changedBy`, `change.field`, `change.id`, `change.name`, `changedAt`).
- Extend orchestration `Issue` model with optional `changeSets` field (populated when audit requested, null when not).
- Enforce maximum `perPage` limit of `40` and set default `perPage` to `20`.
- Update integration layer to expose separate method for fetching issue label events (reuse existing method if available from label-events-api work).
- Orchestration layer orchestrates both search and enrichment calls; assemble final response with changeSets merged into each issue.
- Handle empty label history correctly: return `changeSets: []` when no events exist, not null or omitted.
- Implement no-partial-failure semantics: if any enrichment call fails, return error response with no partial search results.
- Preserve all current search response fields and structure; only populate the previously null/empty `changeSets` field.
- Keep `Issue` model reusable across all endpoints (search with/without audit, single issue, delete endpoints).

### Out of Scope

- Support for other audit event types (e.g., milestone changes, assignment events, state transitions): explicitly deferred; design must be extensible but implementation only supports `"label"` in this story.
- Separate public endpoint for label events; use search endpoint with audit filter only.
- GitLab API-level pagination or filtering of label events (accept server defaults; orchestration layer does not offer client-level filtering on changeSets).
- Modifying, deleting, or creating label events through this API.
- Role-based filtering or masking of label-event visibility (GitLab authentication scope determines visibility).
- Backward-compatibility breaks for existing search API clients. Existing clients not using `audit` filter must receive no response structure changes.

## Acceptance Criteria

1. **Audit filter field accepted:** `POST /api/issues/search` accepts optional `audit` field in request payload as an array of strings (e.g., `"audit": ["label"]`).

2. **Default pagination:** When `perPage` is not specified, default is `20`; when specified, enforce maximum of `40`.

3. **No enrichment when audit absent:** When `audit` is missing, `null`, or empty array in request, no label-event queries are made and `changeSets` is null (or omitted) in each search result.

4. **Label history populated:** When `audit` contains `"label"`, each search result includes `changeSets` populated with one entry per label event (in GitLab order), using the same `changeSet` structure as label-events-api.

5. **ChangeSet fields correct:** Each `changeSets` entry includes `changeType` (from GitLab action), `changedBy` (id, username, name), `change` (field="label", id, name), and `changedAt` (timestamp).

6. **Empty history returns empty array:** When an issue has no label events, `changeSets: []` is included in that result (not null, not omitted).

7. **Validation — unsupported audit values:** Request containing unsupported `audit` values (e.g., `["label", "milestone"]`) returns explicit validation error; null elements in audit array are silently omitted and do not cause errors.

8. **All current fields preserved:** Search response includes all current search fields unchanged except `changeSets` is now populated instead of null/omitted.

9. **Parallel execution:** Label events for multiple search results are fetched in parallel using AsyncComposer, not sequentially.

10. **Full-failure semantics:** If any label-event fetch fails for any search result, endpoint returns error response (4xx/5xx) with no partial search results.

11. **Model reusability maintained:** The `Issue` model used in all endpoints (search with/without audit, single issue, delete) is the same; `changeSets` is optional and null when not requested.

12. **Orchestration owns enrichment:** Orchestration layer calls both search and enrichment integration methods and assembles response; integration layer returns GitLab-native responses; REST DTOs are transport only.

13. **Backward compatibility:** Existing search API clients that do not use `audit` filter continue to work with no response structure changes.

## Dependencies and Assumptions

- **External dependency:** GitLab REST API endpoints `GET /projects/:id/issues` (for search) and `GET /projects/:id/issues/:issue_iid/resource_label_events` are available for the configured project.
- **Architectural dependency:** Label-events-api implementation (Story 2) must be complete; this story reuses its `changeSet` and `change` models and integrations for label-event fetching.
- **Code dependency:** AsyncComposer exists and is available for parallelizing label-event fetches across multiple issues.
- **Assumption:** GitLab label-events endpoint returns events in consistent order (or GitLab-canonical order); no client-side sorting or filtering of label events is performed.

## Open Questions

- None at handoff; locked constraints are complete and unambiguous.
