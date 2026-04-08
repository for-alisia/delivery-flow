# Implementation Plan: Label Events API — Story 2

**Artifact path:** `artifacts/implementation-plans/label-events-api.plan.md`
**Task name:** `label-events-api`
**Plan date:** `2026-04-08`
**Status:** `Draft`

---

## Business Goal

Extend `GET /api/issues/{issueId}` to populate `changeSets` with the complete label change history from GitLab's `resource_label_events` endpoint. Story 1 clients remain unaffected; the response adds label-event history to the previously empty `changeSets` array.

---

## Requirement Lock / Source Of Truth

- Original request: `artifacts/user-prompts/label-events-api.md`; story: `artifacts/user-stories/label-events-api.story.md`
- Endpoint stays `GET /api/issues/{issueId}`; no separate label-events endpoint.
- Integration layer exposes **two separate port methods** (issue detail and label events); orchestration calls both and assembles.
- Client-facing `changeSets` mapping happens in orchestration or orchestration-facing mapper — not in the adapter.
- Partial failure forbidden: if either GitLab call fails, return sanitized error; no partial payload.
- Empty label history → `changeSets: []`.
- `changeType` maps from GitLab `action`; `change.field` is always `"label"` for Story 2.
- `ChangeSet` and `Change` must be interfaces or equivalent extensibility for future event sources.
- GitLab endpoint confirmed: `GET /projects/:id/issues/:issue_iid/resource_label_events` → array of `{ id, user: { id, name, username }, created_at, label: { id, name }, action }`.

---

## Payload Examples

```json
// Success Response — issue with label events
{
  "issueId": 17,
  "title": "Deploy failure",
  "description": "Step 3 failed",
  "state": "opened",
  "labels": ["bug"],
  "assignees": [{ "id": 1, "username": "jdoe", "name": "Jane Doe" }],
  "milestone": null,
  "createdAt": "2026-01-10T10:00:00Z",
  "updatedAt": "2026-03-01T12:00:00Z",
  "closedAt": null,
  "changeSets": [
    {
      "changeType": "add",
      "changedBy": { "id": 1, "username": "root", "name": "Administrator" },
      "change": { "field": "label", "id": 73, "name": "bug" },
      "changedAt": "2026-01-15T09:30:00Z"
    },
    {
      "changeType": "remove",
      "changedBy": { "id": 2, "username": "jdoe", "name": "Jane Doe" },
      "change": { "field": "label", "id": 73, "name": "bug" },
      "changedAt": "2026-02-01T11:00:00Z"
    }
  ]
}

// Success Response — issue with no label events
{ ...same fields..., "changeSets": [] }

// Error Response (either GitLab call fails)
{ "code": "INTEGRATION_FAILURE", "message": "Operation failed" }

// Error Response (issue not found)
{ "code": "RESOURCE_NOT_FOUND", "message": "Operation failed" }
```

---

## Validation Boundary Decision

| Validation Rule | Boundary | Why Here |
|---|---|---|
| `issueId` must be positive (`@Positive`) | REST controller (`@PathVariable @Positive`) | Existing rule; no change |
| No additional validation for label-events fetch | N/A | `issueId` is already validated before port calls |

---

## Scope

### In Scope
- New model interfaces (`ChangeSet`, `Change`) and records (`ChangedBy`, `LabelChange`, `LabelChangeSet`) in `orchestration/issues/model/`
- Port extension: `List<ChangeSet> getLabelEvents(long issueId)` on `IssuesPort`
- Service: call both port methods, assemble `EnrichedIssueDetail`; map `LabelChangeSet` list in service
- REST DTO: `ChangeSetDto` marker interface + `LabelChangeSetDto`, `ChangedByDto`, `LabelChangeDto` nested records in `IssueDetailDto`; `changeSets` field type updated
- Response mapper: convert `List<ChangeSet>` → `List<IssueDetailDto.ChangeSetDto>`
- Integration: `GitLabLabelEventResponse` DTO, `GitLabLabelEventMapper`, adapter implementation
- Component test coverage with WireMock; Karate `issues-get-single.feature` update
- `issues.http` comment update documenting `changeSets` shape
- `documentation/context-map.md` update for new files

### Out of Scope
- Pagination or filtering of label events at client level
- Non-label event sources
- Separate public label-events endpoint

---

## Class Structure

| Class Path (relative to `.../floworchestrator/`) | Status | Behavior |
|---|---|---|
| `orchestration/issues/model/ChangeSet.java` | NEW | Interface: `String changeType()`, `ChangedBy changedBy()`, `Change change()`, `OffsetDateTime changedAt()` |
| `orchestration/issues/model/Change.java` | NEW | Interface: `String field()` |
| `orchestration/issues/model/ChangedBy.java` | NEW | `@Builder` record: `long id`, `String username`, `String name` |
| `orchestration/issues/model/LabelChange.java` | NEW | `@Builder` record implements `Change`; record components: `long id`, `String name`; `field()` returns constant `"label"` (not a component) |
| `orchestration/issues/model/LabelChangeSet.java` | NEW | `@Builder` record implements `ChangeSet`: `String changeType`, `ChangedBy changedBy`, `LabelChange change`, `OffsetDateTime changedAt` |
| `orchestration/issues/model/EnrichedIssueDetail.java` | MODIFY | `List<Object> changeSets` → `List<ChangeSet> changeSets` |
| `orchestration/issues/IssuesPort.java` | MODIFY | Add `List<ChangeSet> getLabelEvents(long issueId)` |
| `orchestration/issues/IssuesService.java` | MODIFY | Call `issuesPort.getLabelEvents(issueId)`; map to `LabelChangeSet` list; pass to `EnrichedIssueDetail` |
| `orchestration/issues/rest/dto/IssueDetailDto.java` | MODIFY | Add nested: `interface ChangeSetDto {}`, `record LabelChangeSetDto(...)`, `record ChangedByDto(...)`, `record LabelChangeDto(String field, long id, String name)`; `changeSets` type → `List<ChangeSetDto>` |
| `orchestration/issues/rest/mapper/IssuesResponseMapper.java` | MODIFY | Map `List<ChangeSet>` → `List<IssueDetailDto.ChangeSetDto>` via private helper using `instanceof LabelChangeSet` pattern |
| `integration/gitlab/issues/dto/GitLabLabelEventResponse.java` | NEW | `@Builder @JsonIgnoreProperties` record: `long id`, `GitLabUserDetail user`, `@JsonProperty("created_at") OffsetDateTime createdAt`, `GitLabLabelDetail label`, `String action`; nested records for user and label |
| `integration/gitlab/issues/mapper/GitLabLabelEventMapper.java` | NEW | `@Component`; `List<ChangeSet> toLabelChangeSets(List<GitLabLabelEventResponse>)`: maps each event to `LabelChangeSet` |
| `integration/gitlab/issues/GitLabIssuesAdapter.java` | MODIFY | Implement `getLabelEvents(long issueId)`: GET `.../issues/{issueId}/resource_label_events`, null-guard, map via `GitLabLabelEventMapper` |

---

## Implementation Slices

### Slice 1 — Extensible Domain Model Contracts

- **Goal:** Define all `ChangeSet`/`Change` contracts and concrete types; update `EnrichedIssueDetail` type. No behavioral change; backward-compatible compilation.
- **Affected scope:** 5 new model files + `EnrichedIssueDetail` modify
- **Payload/contract impact:** None — service still produces empty `changeSets`
- **Validation boundary decisions:** None
- **Unit tests:** `LabelChangeSetTest` — verify defensive copy, `field()` constant, null-safe builder for all new records; verify `EnrichedIssueDetail` still accepts `List.of()`
- **Integration/Web tests:** None
- **Edge/failure coverage:** Null `changeSets` in `EnrichedIssueDetail` compact constructor → `List.of()`
- **INFO logging:** None
- **WARN logging:** None
- **ERROR logging:** None
- **Documentation updates:** None

---

### Slice 2 — Port Extension, Service Assembly, and REST Wiring (Stub Adapter)

- **Goal:** Wire `getLabelEvents` through port → service → response mapper; adapter stub returns `List.of()`. Full end-to-end with empty changeSets verified. Typed `ChangeSetDto` in REST DTO.
- **Affected scope:** `IssuesPort`, `IssuesService`, `IssueDetailDto`, `IssuesResponseMapper`, `GitLabIssuesAdapter` (stub `getLabelEvents`)
- **Payload/contract impact:** `changeSets` field type changes in `IssueDetailDto` from `List<Object>` to `List<ChangeSetDto>`. JSON output identical for current callers (`[]`).
- **Validation boundary decisions:** None
- **Unit tests:**
  - `IssuesServiceTest`: add `getIssueDetail_populatesChangeSetsFromPort` (port returns two events, result has two `LabelChangeSet`); add `getIssueDetail_labelEventsFailure_propagatesException` (port throws, service propagates)
  - `IssuesResponseMapperTest`: add `toIssueDetailDto_mapsLabelChangeSet` (verify all fields including `change.field == "label"`); add `toIssueDetailDto_emptyChangeSets_returnsEmptyList`; add `toIssueDetailDto_unknownChangeSetType_throwsIllegalArgument`
  - `GitLabIssuesAdapterTest`: verify stub `getLabelEvents` returns empty list
- **Integration/Web tests:** `IssuesControllerIT` — add `getIssueDetail_returnsChangeSetsArray` (response body contains `changeSets` as array)
- **Edge/failure coverage:** Unknown `ChangeSet` subtype in mapper → `IllegalArgumentException`; `List.of()` from stub → `changeSets: []` in response
- **INFO logging:** `IssuesService.getIssueDetail` — add `"Label events fetched issueId={} count={}"` after `getLabelEvents` call
- **WARN logging:** None
- **ERROR logging:** None
- **Documentation updates:** `issues.http` — add comment to get-single example documenting `changeSets` shape

---

### Slice 3 — Live GitLab Integration, Component Test, and Karate

- **Goal:** Replace stub with real GitLab integration; add WireMock component scenarios; update Karate feature.
- **Affected scope:** `GitLabLabelEventResponse` (new), `GitLabLabelEventMapper` (new), `GitLabIssuesAdapter` (real impl), `GitLabLabelEventsStubSupport` (new), `IssuesApiComponentTest` (add scenarios), `issues-get-single.feature` (update), `context-map.md` (update)
- **Payload/contract impact:** None — contract defined in Slice 2
- **Validation boundary decisions:** Null body from GitLab label-events endpoint → `IntegrationException` (same guard as existing `getIssueDetail`)
- **Unit tests:** `GitLabLabelEventMapperTest` — verify mapping of `action`, `user.*`, `label.*`, `created_at`; empty list input → empty list output
- **Integration/Web tests:** None (covered by component)
- **Edge/failure coverage:** `IssuesApiComponentTest`:
  - `getIssueDetail_withLabelEvents_returnsPopulatedChangeSets` (stub both issue-detail and label-events, assert `changeSets[0]` fields)
  - `getIssueDetail_emptyLabelEvents_returnsEmptyChangeSets` (stub label-events returning `[]`)
  - `getIssueDetail_labelEventsFails_returnsErrorResponse` (stub label-events returning 500, assert 500 + error code + no issue fields in body)
  - `getIssueDetail_issueDetailFails_labelEventsNotCalled` (stub issue-detail returning 404, assert 404 error)
- **INFO logging:** `GitLabIssuesAdapter.getLabelEvents` — `"Fetching GitLab label events issueId={}"` before call; `"GitLab label events fetched issueId={} count={}"` after
- **WARN logging:** None
- **ERROR logging:** None
- **Documentation updates:** `documentation/context-map.md` — add new files to issues capability table and tests table

---

## Karate Feature: `resources/issues/issues-get-single.feature`

Update the existing file. Add one new non-smoke scenario. Existing smoke scenarios are preserved unchanged.

```gherkin
  Scenario: changeSets is an array in single-issue response
    * def suffix = java.lang.System.currentTimeMillis()
    * def title = 'Karate label-events check ' + suffix

    Given path 'api/issues'
    And request { title: '#(title)' }
    When method post
    Then status 201
    * def createdIssueId = response.issueId

    Given path 'api/issues', createdIssueId
    When method get
    Then status 200
    And match response.changeSets == '#array'

    Given path 'api/issues', createdIssueId
    When method delete
    Then status 204
```

---

## Testing Matrix

| Level | Required | Planned Coverage | Evidence Target |
|---|---|---|---|
| Unit — new model records | Yes | `LabelChangeSetTest` | Slice 1 |
| Unit — service label-events assembly | Yes | `IssuesServiceTest` updates | Slice 2 |
| Unit — response mapper `ChangeSet`→`ChangeSetDto` | Yes | `IssuesResponseMapperTest` updates | Slice 2 |
| Unit — GitLab label event mapper | Yes | `GitLabLabelEventMapperTest` | Slice 3 |
| Unit — adapter stub then real | Yes | `GitLabIssuesAdapterTest` updates | Slices 2 + 3 |
| Integration (`@WebMvcTest`) | Yes | `IssuesControllerIT` — changeSets array present | Slice 2 |
| Component (WireMock) | Yes | `IssuesApiComponentTest` — 4 scenarios | Slice 3 |
| Karate smoke | Yes | Existing `@smoke` scenario (backward compat) + new array-check scenario | Slice 3 |

---

## Acceptance Criteria

All 12 story acceptance criteria traceable to the slices above. Key mappings:
- AC 1–4, 12: Slice 3 component test + Karate
- AC 5 (empty → `[]`): Slice 2 mapper test + Slice 3 component
- AC 6 (Story 1 fields preserved): existing tests remain green; no field removed
- AC 7 (mapping in orchestration): `IssuesResponseMapper.toIssueDetailDto` handles conversion — Slice 2
- AC 8 (separate port methods): Slice 2 port + service tests
- AC 9 (no partial failure): Slice 3 component test `labelEventsFails_returnsErrorResponse`
- AC 10 (backward compat): all existing tests stay green after each slice
- AC 11 (extensible design): `ChangeSet`/`Change` interfaces enable future implementors without contract change

---

## Final Verification

- `scripts/verify-quick.sh` after each slice
- `scripts/final-check.sh` before handoff to Reviewer
- `scripts/karate-test.sh` after Slice 3 confirms `@smoke` passes and new scenario passes
- `curl GET /api/issues/{realIssueId}` — confirm `changeSets` is an array; observe populated entries if label events exist on the issue
- Report paths: `flow-orchestrator/target/checkstyle-result.xml`, `target/pmd.xml`, `target/spotbugsXml.xml`, `target/site/jacoco/jacoco.xml`
