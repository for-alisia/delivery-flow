# User Story: Search Enhancement — Audit Trail Visibility

**Feature name:** `search-enhancement-api`  
**Story date:** `2026-04-09`

## Business Goal

Enable delivery teams to quickly understand the change history of multiple issues in a single request. Teams currently must fetch audit trails issue-by-issue, creating inefficiency during bottleneck analysis. Bulk audit visibility in search results reduces round trips and helps teams understand workflow progression across multiple items at once.

## Problem Statement

Delivery teams and managers need to understand why issues have reached their current state — specifically, when labels were added or removed and by whom. The existing single-issue endpoint provides this trail, but applying it to search results requires multiple sequential API calls. This creates performance and usability friction when analyzing multiple issues in a milestone or by state.

## User Story

As a delivery manager or team lead,  
I want to optionally request audit trails (specifically label-change history) for multiple issues in a single search call,  
so that I can quickly understand workflow progression and who is responsible for state changes without multiple round trips.

## Locked Request Constraints

These are the non-negotiable constraints from the original request:

- **Audit request model:** `audit` is an array field in the search filters; unsupported values must return a validation error; `null`, missing, or empty array are all treated identically (no audit enrichment).
- **Supported audit types:** Only `"label"` is supported in this story; the model must permit future types (e.g., `"milestone"`) without contract breaking.
- **Enrichment strategy:** If `audit` contains `"label"`, each returned search item receives populated `changeSets` (built from GitLab label-event history); if `audit` is absent/null/empty, no extra calls are made and `changeSets` is null.
- **Model reuse:** The `changeSets` structure must match the existing single-issue label-event modelling. The core `Issue` model must not duplicate or diverge.
- **Error handling:** If any GitLab call needed for enrichment fails, treat it as a full request failure and return an error to the client.
- **Page size constraints:** Maximum `perPage` is 40 (client agreed); default `perPage` is 20.
- **Performance strategy:** Use the existing async parallel-composition capability to fetch label events for multiple issues concurrently, not sequentially.

## Business Context and Constraints

- **Primary users:** Delivery managers, team leads, and engineering leads analyzing milestone progress or bottlenecks.
- **Business terminology:** "Audit trail" = label-change history showing what changed, when, and by whom. "Enrichment" = adding audit context to the search result set.
- **Related capability:** The single-issue endpoint already returns `changeSets`; this story extends that pattern to search results.
- **Performance expectation:** Audit-enriched searches must remain responsive even for 40 issues; parallel event fetching must prevent sequential bottlenecks.
- **Data consistency:** Label events must be fetched from the same source as the single-issue audit trail (GitLab `resource_label_events` endpoint) to ensure consistency.

## Scope

### In Scope

- Add optional `audit` filter array to the `POST /api/issues/search` request contract.
- When `audit` contains `"label"`, fetch and merge label-event history into each search result as `changeSets`.
- Enforce maximum page size of 40 and change default to 20.
- Validate `audit` array; reject unsupported values with explicit error.
- Handle all three null-like cases (`null`, missing, empty array) as "no-audit" behavior.
- Use async parallel composition to avoid sequential bottlenecks when fetching multiple label-event sequences.
- Extend response contract to include `changeSets` when audit is requested.

### Out of Scope

- Audit types other than `"label"` (milestone, pipeline status, MR integration, etc.).
- Partial failure modes (e.g., skip one issue if its audit fetch fails; current requirement is all-or-nothing).
- Client-side filtering, sorting, or grouping by audit metadata.
- Audit trail persistence; events come directly from GitLab sources.
- Modifying the single-issue `GET /api/issues/{issueId}` endpoint or its `changeSets` structure.

## Acceptance Criteria

1. **Given** a client calls `POST /api/issues/search` without `audit` or with `audit: null` or `audit: []`, **when** the response is returned, **then** no extra GitLab event calls are made and each issue object has `changeSets: null`.

2. **Given** a client calls `POST /api/issues/search` with `audit: ["label"]`, **when** the response is returned, **then** each issue object includes a populated `changeSets` array (possibly empty if no label events exist for that issue) containing label-change records identical in structure to those returned by the single-issue endpoint.

3. **Given** a client sends `audit` with unsupported values (e.g., `audit: ["milestone"]`), **when** the request is processed, **then** a validation error is returned explicitly identifying the unsupported value.

4. **Given** a client sends `perPage: 50`, **when** the request is processed, **then** it is rejected with a validation error stating the maximum is 40.

5. **Given** the system receives a valid search request with `audit: ["label"]` for 10 issues, **when** GitLab label-event responses are fetched, **then** all 10 event sequences are fetched concurrently (not sequentially).

6. **Given** a valid search request with `audit: ["label"]` is being processed and one GitLab label-event call fails, **when** the failure is detected, **then** the entire search request returns an error to the client (no partial-success response).

7. **Given** the API default behavior when `perPage` is omitted, **when** a search request is submitted, **then** `perPage` defaults to 20.

8. **Given** a search result set is returned with `audit: ["label"]`, **when** the response is validated against the contract, **then** each issue object contains a `changeSets` field with the same structure and field names as the single-issue endpoint's `changeSets` (matching GitLab label-event action types and including timestamp, actor, and change details).

## Dependencies and Assumptions

- **External dependencies:**
  - GitLab `resource_label_events` endpoint is available and stable (search functionality depends on label-event fetching).
  - The async parallel-composition capability already exists and is production-ready.

- **Assumptions:**
  - The single-issue `changeSets` structure will not change during this story's implementation; if it does, both endpoints must evolve together.
  - GitLab label events are returned in chronological order (or project convention mirrors single-issue behavior).
  - The orchestration layer coordinates all enrichment logic; REST DTOs are transport carriers only.
  - Clients accept that audit enrichment increases response time proportionally with result set size and audit type complexity.

## Open Questions

- **null handling:** Should `null` inside the `audit` array (e.g., `audit: ["label", null]`) be silently stripped or returned as a validation error? Current assumption: strip silently and process only the valid values.
- **Future audit types:** Should the response include metadata indicating which audit types were requested vs. which were populated? Current assumption: the client tracks this; response is implicit (if `changeSets` is populated, it came from `audit: ["label"]`).
