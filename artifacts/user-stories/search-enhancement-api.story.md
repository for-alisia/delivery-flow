# User Story: Search Enhancement API — Audit Filter with Parallel Enrichment

**Feature name:** `search-enhancement-api`
**Story date:** `2026-04-18`

## Business Goal

Enable delivery managers and team members to augment issue search results with audit-trail context (starting with label change history) so they can understand not just current issue state but also how issues have evolved—providing richer delivery flow insights without requiring separate label-history queries.

## Problem Statement

The current search endpoint returns a snapshot of issue state at query time. Delivery teams lack visibility into how issues have evolved—when labels were added, who changed them, and in what order. Without this history, managers cannot audit workflow progression or understand if an issue's current state has been stable or frequently contested. Adding optional audit context to search results lets teams gain this visibility efficiently, especially for large result sets.

## User Story

As a delivery manager or engineering team member,
I want to optionally request label change history alongside my issue search results,
so that I can audit how issues have evolved through workflow states and understand ownership and priority transitions without running separate queries.

## Locked Request Constraints

These are the non-negotiable constraints from the original request and must be preserved exactly:

- **Endpoint constraint:** Extend only `POST /api/issues/search`; no other endpoints are modified.
- **Filter constraint:** Add `audit` as a new optional array field in the request `filters` object, modeling it as an extensible enum-like array to accommodate future audit event types (e.g., `milestone`, `assignment`) beyond the current `label` support.
- **Supported values:** In this story, `audit` array supports only `"label"` as a confirmed value; any other values must be rejected with an explicit validation error response.
- **Null/missing handling:** `audit` may be absent, `null`, or an empty array `[]`; all three cases are valid and must behave identically (no extra GitLab calls, no enrichment).
- **Null entries omission:** If `audit` contains `null` entries mixed with valid values (e.g., `["label", null, "label"]`), the null entries must be omitted; enrichment proceeds with remaining valid values if at least one valid value remains. If only null values exist, treat as empty audit.
- **Enrichment logic:** When `audit` contains `"label"`, each returned search item must be enriched with `changeSets` built from GitLab label events; enrichment must execute after search results return from GitLab (not before).
- **No extra calls when audit absent:** When `audit` is absent, `null`, or empty, no GitLab label-event API calls must be made; returned issues must not include `changeSets` or must include `null` for `changeSets` field.
- **Pagination constraints:** Enforce maximum `perPage` of `40`; reject requests exceeding this limit with a clear validation error. Update default `perPage` to `20` (previously `40` per first-issues-api story).
- **Orchestration owns modeling:** Business logic for audit filtering, enrichment orchestration, and response assembly must reside in the orchestration layer; REST DTOs must not introduce business rules unless they are transport-only carriers.
- **Reuse existing label models:** The `changeSets` structure (including `changeSet`, `change`, `changedBy` models) must reuse existing models from the label-events-api story; do not introduce duplicate or conflicting label-event models.
- **Extend shared Issue model:** Extend the reusable orchestration `Issue` model with an optional `changeSets` field (array or null). When audit is not requested, `changeSets` must be `null` or omitted; this extension must not break existing uses of the `Issue` model for endpoints that do not support audit (delete, search without audit, get-single).
- **Parallel enrichment:** Use the existing `AsyncComposer` utility (from async-composer story) to fetch label events in parallel for multiple returned issues. This is performance-critical as search results may return up to 40 issues, each potentially requiring a label-event fetch.
- **Dedicated response DTO permitted:** A dedicated REST DTO for the search response is permitted if needed for transport concerns; however, the orchestration `Issue` model must remain reusable and transport-agnostic.
- **Full failure on enrichment:** If any GitLab API call required for enrichment (label-event fetch) fails, return a sanitized error response to the client; no partial search results are acceptable in this story.
- **Validation errors:** Unsupported `audit` values must return an explicit validation error (4xx response) with clear feedback about supported values and future roadmap.

## Business Context and Constraints

- **Primary users:** Delivery managers, engineering team members, and agile coaches using GitlabFlow to track delivery flow and understand issue evolution.
- **Primary stakeholders:** Product leadership, flow orchestration domain teams, delivery center of excellence.
- **Important terminology:**
  - *Audit trail:* A sequence of events (starting with label events, future expansion to milestone, assignment) that show how an issue has changed over time.
  - *Enrichment:* The process of augmenting search results with audit data after the search completes.
  - *Parallel fetch:* Concurrent retrieval of label events for multiple issues to avoid sequential API call overhead.
  - *Extensible enum:* A model pattern that allows adding new enum-like values (e.g., new audit event types) in future stories without breaking current clients.
  - *Full failure:* When enrichment fails, the entire request fails (not partial success).
- **Business-facing performance expectation:** Search with audit enrichment should not significantly degrade response time compared to search without audit. For a typical result set of 20–40 issues, parallel label-event fetches should complete within the same request/response window (no async polling or deferred results).
- **Backward compatibility:** Existing clients calling search without the `audit` filter must continue to receive identical responses as before (no `changeSets` field or `changeSets: null`).
- **Business-facing security expectation:** Users calling search with audit enrichment must see only the label events visible to them under their current GitLab project permissions. No over-disclosure of internal events.

## Scope

### In Scope

- Extend `POST /api/issues/search` request contract to include optional `filters.audit` array field.
- Implement `audit` as an extensible enum-like field (modeled to allow future values without response-contract breaking changes).
- Support `audit: ["label"]` to trigger label-event enrichment; fetch label events from GitLab `GET /projects/:id/issues/:issue_iid/resource_label_events` for each returned search result.
- Build `changeSets` array for each search result using existing label-event models (reused from label-events-api story).
- Enrich search results after GitLab search returns; compose enrichment calls in parallel using `AsyncComposer`.
- Validate that `audit` array contains only supported values; reject unsupported values with explicit validation error.
- Handle null entries in `audit` array: omit them and proceed with remaining valid values if any exist.
- Apply no extra GitLab calls when `audit` is absent, `null`, or empty.
- Extend orchestration `Issue` model with optional `changeSets` field, ensuring reusability across all endpoints.
- Enforce new pagination constraints: max `perPage = 40`, default `perPage = 20`.
- Return enriched response with `changeSets` included for each issue (when audit includes `label`), or without (when audit is absent/null/empty).
- Implement full-failure semantics: if any enrichment call fails, return error response with no partial results.

### Out of Scope

- Support for `audit` values beyond `"label"` (future audit types like milestone, assignment are explicitly out of scope; structure is extensible but not implemented).
- Separate public endpoint for audit trails (enrichment is part of search response only).
- Client-side filtering or post-processing of changeSets (return all label events in GitLab order).
- Pagination of changeSets within a single issue's label history (return full history).
- Modifying, creating, or deleting label events.
- Role-based masking or filtering of label events beyond GitLab's own permission model.
- Changes to endpoints other than `POST /api/issues/search` (delete, create, get-single, etc. remain unchanged).

## Acceptance Criteria

1. **Endpoint accepts audit filter:** When a client sends `POST /api/issues/search` with `filters.audit: ["label"]`, the endpoint accepts the request and processes it without error.

2. **Search executes first:** When audit enrichment is requested, the underlying GitLab search completes and returns results to orchestration before any label-event fetches begin.

3. **Enrichment happens in parallel:** When search returns multiple issues (e.g., 20), label-event fetches for all returned issues run concurrently (using AsyncComposer) instead of sequentially.

4. **ChangeSets populated correctly:** Each returned issue includes a `changeSets` array populated from label events (in GitLab order) with each entry containing `changeType`, `changedBy` (with `id`, `username`, `name`), `change` (with `field: "label"`, `id`, `name`), and `changedAt`.

5. **No enrichment when audit absent:** When `audit` is omitted, `null`, or `[]`, no label-event API calls are made and returned issues either omit `changeSets` or include `changeSets: null`.

6. **Backward compatibility preserved:** Existing clients calling search without `audit` filter receive identical responses (same field set, same structure) as before this story.

7. **Unsupported audit values rejected:** When `audit` contains unsupported values (e.g., `["milestone"]`), the endpoint returns a 4xx validation error response with clear explanation of supported values.

8. **Null entries omitted:** When `audit: ["label", null, "label"]`, the null is omitted and enrichment proceeds with the two `"label"` values.

9. **Pagination defaults updated:** Default `perPage` is `20` when omitted; `perPage: 41` or higher is rejected with validation error.

10. **Max perPage enforced:** When `perPage: 40`, request is accepted; when `perPage: 41`, request is rejected with validation error.

11. **Full failure on enrichment error:** If label-event fetch fails for any returned issue, the entire request fails with a sanitized error response (no partial results returned).

12. **Reused models confirmed:** `changeSets`, `changeSet`, `change`, and `changedBy` models are identical to those used in label-events-api story (no duplicate definitions).

13. **Issue model remains reusable:** The orchestration `Issue` model extended with optional `changeSets` is used by all issue endpoints (search, delete, create, get-single, search-without-audit) without forcing audit-unaware clients to handle enrichment fields.

14. **Validation error for enrichment failure:** When GitLab label-event fetch fails, error response is sanitized (no internal stack traces, no provider-specific error details).

## Dependencies and Assumptions

- **Architectural dependency:** `AsyncComposer` utility (from async-composer story) must be implemented and available for parallel enrichment orchestration.
- **Model dependency:** Label-event models (`changeSet`, `change`, `changedBy`) from label-events-api story must be available for reuse.
- **External dependency:** GitLab REST API endpoints `GET /projects/:id/issues` (search) and `GET /projects/:id/issues/:issue_iid/resource_label_events` are available and responding for the configured project.
- **Integration dependency:** GitLab integration layer must expose methods for both search and label-event fetching; orchestration layer coordinates both.
- **Assumption:** Default pagination change from `40` to `20` does not break existing clients; API versioning or client contracts support this change.
- **Assumption:** The `Issue` model extension with optional `changeSets` field is backward compatible (null handling respects existing deserialization).
- **Assumption:** Label-event fetches for a typical result set (20–40 issues) complete within standard HTTP request timeout (no timeout reconfiguration needed for this story).

## Open Questions

- None identified at this stage. Locked constraints provide sufficient detail for architecture and implementation planning. Label-event model reuse details will be confirmed during architecture review against label-events-api artifacts.
