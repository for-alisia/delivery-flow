# User Story: Label Events API — Story 2 (Label Change History)

**Feature name:** `label-events-api`
**Story date:** `2026-04-08`

## Business Goal

Give delivery managers and agile team members visibility into the complete label change history for individual work items, so they can understand how item ownership, priority, and workflow status have evolved over time.

## Problem Statement

Story 1 (Get Single Issue API) provides a snapshot of current issue state including current labels. However, teams lack insight into when and why labels were added or removed—critical context for understanding how an issue moved through workflow states and who has touched it. Without change history, managers cannot audit label-driven workflow progression or troubleshoot why an issue's status has changed unexpectedly.

## User Story

As a delivery manager or agile team member,
I want to see a complete history of label changes on a single work item when retrieving its details,
so that I can audit workflow progression, track ownership changes, and troubleshoot unexpected state transitions.

## Locked Request Constraints

These constraints are non-negotiable and must be respected during architecture and implementation:

- Keep the public endpoint as `GET /api/issues/{issueId}`; do not introduce a separate public label-events endpoint.
- Populate `changeSets` in the existing single-issue response using GitLab `GET /projects/:id/issues/:issue_iid/resource_label_events`.
- The public `issueId` path parameter maps only to GitLab `issue_iid` for the configured project; clients must not pass any project identifier.
- Continue using GitLab `GET /projects/:id/issues/:issue_iid` for main issue details and keep Story 1 response fields intact except replacing placeholder empty `changeSets` with mapped label-event history when events exist.
- Integration must expose a separate method for fetching issue label events and must not combine issue details and label events into the final response.
- Orchestration must call both integration methods and assemble the final response; client-facing `changeSets` mapping must happen in orchestration or an orchestration-facing mapper.
- `changeSets` is built only from label events in this story; future event sources are explicitly out of scope.
- `changeSets[].changeType` maps from GitLab label-event action; `change.field` is always `label` for this story; events should be returned in GitLab order unless existing project conventions require otherwise.
- Design `changeSet` and `change` using interfaces or equivalent extensibility so future event sources can implement the same contract.
- Partial failure is not allowed: if either the issue-details call or label-events call fails, return the standard sanitized error response and no partial issue detail payload.
- Empty label history must return `changeSets: []`.
- Confirm GitLab label-events endpoint details against official GitLab REST API docs during planning/implementation.
- Add or update tests at the correct levels while preserving backward compatibility with Story 1 clients.

## Business Context and Constraints

- **Primary users:** Delivery managers and agile team members using GitlabFlow to track delivery flow and work-item status (same users as Story 1).
- **Primary stakeholder:** Product leadership and flow orchestration domain teams.
- **Important terminology:**
  - *ChangeSets:* A sequence of change records, each capturing a label modification event with timestamp, actor, and change details.
  - *Label event:* A single label add or remove action on an issue in GitLab (from `resource_label_events` endpoint).
  - *Backward compatibility:* Story 1 clients must continue to receive all Story 1 response fields without breaking changes.
- **Business-facing performance expectation:** Response should be delivered in real time with no noticeable latency to the user; no async or deferred loading. Label event fetches must not significantly degrade response time compared to Story 1.
- **Security expectation:** No sensitive fields or over-disclosure beyond what the authenticated GitLab user would see on the connected project; label events visible only to users with access to the underlying issue and its labels.

## Scope

### In Scope

- Extend the existing `GET /api/issues/{issueId}` endpoint (from Story 1) to populate `changeSets` with label-event history.
- Fetch label events from GitLab's `GET /projects/:id/issues/:issue_iid/resource_label_events` endpoint for the configured project.
- Map GitLab label-event fields to client-facing `changeSet` and `change` structures (`changeType`, `changedBy`, `change.field`, `change.id`, `change.name`, `changedAt`).
- Handle empty label history correctly: return `changeSets: []` when no events exist.
- Ensure integration layer exposes separate methods for issue details and label events; orchestration layer calls both and assembles the final response.
- Design `changeSet` and `change` interfaces to support extensibility for future event types (audit events, state transitions, etc.) without response contract changes.
- Preserve all Story 1 response fields and structure; only populate the previously empty `changeSets` array.
- Implement no-partial-failure semantics: if either issue-details or label-events fetch fails, return error response with no partial data.
- Test at appropriate levels while maintaining full backward compatibility with Story 1 clients.

### Out of Scope

- Separate public endpoint for label events (use existing single-issue endpoint only).
- Future event sources (e.g., workflow state events, assignment changes, milestone changes): abstract the design but do not implement; tagged as explicit future work.
- GitLab API-level pagination or filtering of label events (accept server defaults; orchestration layer does not offer client-level filtering on changeSets).
- Modifying, deleting, or creating label events through this API.
- Role-based filtering or masking of label-event visibility (GitHub authenticaton scope determines what GitLab returns).

## Acceptance Criteria

1. **Endpoint returns label history:** When calling `GET /api/issues/{issueId}` for an issue with label events, the response includes `changeSets` populated with one entry per label event (in GitLab order).

2. **ChangeSet structure is correct:** Each entry in `changeSets` includes exactly `changeType`, `changedBy`, `change`, and `changedAt` fields. `change` includes `field` (always `"label"` in Story 2), `id`, and `name`.

3. **ChangeType maps from GitLab action:** `changeType` equals the GitLab label-event `action` field (e.g., `"add"`, `"remove"`).

4. **ChangedBy includes user details:** Each `changedBy` includes `id`, `username`, and `name` matching GitLab user details from the label event.

5. **Empty history returns empty array:** When no label events exist for an issue, `changeSets: []` is returned (not null, not omitted).

6. **All Story 1 fields preserved:** The response includes all Story 1 fields unchanged (`issueId`, `title`, `description`, `state`, `labels`, `assignees`, `milestone`, timestamps) except `changeSets` is now populated instead of empty.

7. **Client-facing mapping in orchestration:** Orchestration layer (not integration) maps GitLab label-event structure to client-facing `changeSet` structure; integration layer returns GitLab-native response untouched.

8. **Separate integration methods:** Integration layer provides at least two distinct methods: one for issue details, one for label events; orchestration orchestrates both calls.

9. **No partial failures:** If GitLab issue-details fetch fails or label-events fetch fails, endpoint returns a sanitized error response (4xx or 5xx) with no partial issue detail payload.

10. **Backward compatibility maintained:** Story 1 clients calling the endpoint before label events exist on their issues receive `changeSets: []` and no response structure changes; all other fields remain as Story 1 defined.

11. **Extensible design in place:** `changeSet` and `change` are defined as interfaces or abstract types so future event sources (non-label events) can implement the same contract without breaking the response structure.

12. **GitLab API contract aligned:** Label-event mapping follows GitLab `resource_label_events` endpoint specification; no speculative or inferred mappings.

## Dependencies and Assumptions

- **External dependency:** GitLab REST API endpoints `GET /projects/:id/issues/:issue_iid` and `GET /projects/:id/issues/:issue_iid/resource_label_events` are available and responding for the configured project.
- **Architectural dependency:** Story 1 (Get Single Issue API) must be implemented and backward compatible; this story extends it, not replaces it.
- **Assumption:** Client calling the endpoint has valid authentication; GitLab permissions determine which label events are visible.
- **Assumption:** GitLab `resource_label_events` endpoint is stable and returns events in consistent, predictable order.
- **Assumption:** Label-event API behavior is documented in official GitLab REST API docs and does not change during implementation.

## Open Questions

- None identified at this stage. Locked constraints provide sufficient specification for architecture and implementation planning.
