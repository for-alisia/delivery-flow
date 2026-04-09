# Implementation Plan: Search Enhancement API - Audit-Enriched Issue Search

**Artifact path:** `artifacts/implementation-plans/search-enhancement-api.plan.md`
**Task name:** `search-enhancement-api`
**Plan date:** `2026-04-09`
**Status:** `Revised`

## Scope

Revise only `POST /api/issues/search` so clients can request label-audit enrichment in the existing search response while preserving one primary item DTO strategy: `IssueDto` remains the shared search/create/delete entity DTO. No `SearchIssueDto` is introduced.

## Requirement Lock / Source Of Truth

- Request source: `artifacts/user-prompts/search-enhancement-api.md`
- Story: `artifacts/user-stories/search-enhancement-api.story.md`
- Locked endpoint scope: update only `POST /api/issues/search`
- Locked request: add optional `filters.audit` array; only `"label"` is supported now; `null` elements are ignored; missing, `null`, and `[]` behave the same
- Locked orchestration flow: search first, then fetch per-issue label events in parallel via `AsyncComposer`, then merge them into the returned issues as `changeSets`
- Locked model rule: extend orchestration `Issue` with optional `changeSets`; reuse existing orchestration `ChangeSet` / `Change` / label-event models
- Locked response rule: search/create/delete share one primary item DTO; non-audit search keeps the current wire shape; audit-enabled search adds `changeSets`
- Locked failure rule: any required label-event failure fails the whole request; no partial enrichment
- Locked pagination rule: default `perPage=20`, max `perPage=40`
- GitLab docs verified: `GET /projects/:id/issues` supports `page`, `per_page`, `state`, `labels`, `assignee_username`, `milestone`; `GET /projects/:id/issues/:issue_iid/resource_label_events` returns `user`, `created_at`, `label`, and `action`
- Explicit assumption: GitLab label-event order is consumed as returned; orchestration does not re-sort events

## Payload Examples

```json
// Request: default search without audit
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

```json
// Request: search with label audit enrichment
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

```json
// Success response: audit requested and history found
{
  "items": [
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
  ],
  "count": 1,
  "page": 1
}
```

```json
// Success response: audit absent or empty
{
  "items": [
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
  ],
  "count": 1,
  "page": 1
}
```

```json
// Error response: enrichment call fails
{
  "code": "INTEGRATION_FAILURE",
  "message": "GitLab get label events operation failed",
  "details": []
}
```

```json
// Validation error response: unsupported audit value
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    "filters.audit must contain only supported values [label]"
  ]
}
```

## Validation Boundary Decision

| Validation Rule | Boundary | Why Here |
|---|---|---|
| `pagination.page` and `pagination.perPage` must be positive | REST DTO binding (`PaginationRequest`) | Standard scalar validation belongs at HTTP binding |
| `labels`, `assignee`, `milestone` remain single-value lists and drop `null` elements | `IssueFiltersRequest` compact constructor + existing `@Size(max=1)` | This is transport normalization, not orchestration logic |
| `audit` may be missing, `null`, empty, or contain `null` elements | `IssueFiltersRequest` compact constructor | Request-shape normalization belongs at the boundary |
| `audit` may contain only supported values | `IssuesRequestMapper` when building `SearchIssuesInput` | Supported audit types are orchestration semantics that must still ignore `null` entries |
| `perPage <= 40` | `IssuesService.validatePerPage` using `IssuesApiProperties.maxPageSize()` | Existing runtime guard remains the single owner of configurable page-size enforcement |

## Class Structure

### Model Definitions

| Model | Type | Fields / Contract | Notes |
|---|---|---|---|
| `com.gitlabflow.floworchestrator.orchestration.issues.model.Issue` | changed record | `long id`, `long issueId`, `String title`, `@Nullable String description`, `String state`, `List<String> labels`, `@Nullable String assignee`, `@Nullable String milestone`, `@Nullable Long parent`, `@Nullable List<ChangeSet> changeSets` | Keep `@Builder`; defensively copy `labels`; preserve `changeSets=null` when audit not requested, else `List.copyOf(changeSets)` |
| `com.gitlabflow.floworchestrator.orchestration.issues.model.SearchIssuesInput` | new record | `IssueQuery query`, `List<IssueAuditType> auditTypes` | Keep `@Builder`; defensively copy `auditTypes`; normalize `null` to `List.of()` |
| `com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery` | unchanged record | `int page`, `int perPage`, `@Nullable IssueState state`, `@Nullable String label`, `@Nullable String assignee`, `@Nullable String milestone` | Remains the adapter-facing GitLab search contract; no audit field |
| `com.gitlabflow.floworchestrator.orchestration.issues.model.IssueAuditType` | new enum | values: `LABEL("label")`; methods: `String value()`, `@Nullable static IssueAuditType fromValue(String raw)` | Future values append here without changing the port contract |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueFiltersRequest` | changed record | existing fields plus `List<String> audit` | Keep `@Builder`; sanitize all list fields by dropping `null` elements; return immutable copies |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDto` | changed record | existing fields plus `@Nullable List<IssueAuditChangeSetDto> changeSets` | Keep `@Builder`; defensively copy `labels`; preserve `changeSets=null` or copy; annotate only the `changeSets` component for null omission so other nullable fields keep their current wire shape |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDto.IssueAuditChangeSetDto` | new sealed interface | accessors: `String changeType()`, `IssueAuditChangedByDto changedBy()`, `IssueAuditChangeDto change()`, `OffsetDateTime changedAt()` | Permits `IssueAuditLabelChangeSetDto`; search-only transport for nested audit payload |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDto.IssueAuditChangeDto` | new sealed interface | accessors: `IssueAuditChangeField field()`, `long id()`, `String name()` | Permits `IssueAuditLabelChangeDto` |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDto.IssueAuditLabelChangeSetDto` / `IssueAuditChangedByDto` / `IssueAuditLabelChangeDto` | new records | `IssueAuditLabelChangeSetDto(String changeType, IssueAuditChangedByDto changedBy, IssueAuditLabelChangeDto change, OffsetDateTime changedAt)`; `IssueAuditChangedByDto(long id, String username, String name)`; `IssueAuditLabelChangeDto(IssueAuditChangeField field, long id, String name)` | Keep `@Builder`; DTO-only transport types that mirror existing orchestration change-set contracts |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDto.IssueAuditChangeField` | new enum | values: `LABEL("label")`; methods: `String value()`, `static IssueAuditChangeField from(ChangeField field)` | Serializes as lower-case `label`; isolates search transport from the existing get-single enum serialization |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesResponse` | unchanged record | `List<IssueDto> items`, `int count`, `int page` | Wrapper stays unchanged because `IssueDto` remains the only search/create/delete item DTO |
| `com.gitlabflow.floworchestrator.orchestration.issues.model.ChangeSet` / `Change` / `LabelChangeSet` / `LabelChange` / `ChangedBy` | unchanged interfaces / records | Existing accessor contracts remain exactly as implemented today | Reused orchestration model; no duplicate business models |

### Affected Classes

| Class Path | Status | Proposed Behavior |
|---|---|---|
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/config/IssuesApiProperties.java` | Change | Tighten config max to `40`; keep startup guard `default<=max` |
| `flow-orchestrator/src/main/resources/application.yml` | Change | Set `app.issues-api.default-page-size=20` and `max-page-size=40` |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/IssuesService.java` | Change | Accept `SearchIssuesInput`; validate `perPage`; call `issuesPort.getIssues(input.query())`; if `auditTypes` contains `LABEL`, fetch label events in parallel, fail fast, and merge `changeSets` into copied `Issue` records while preserving item order |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/IssuesPort.java` | Reuse unchanged | Keep `getIssues(IssueQuery)` and `getLabelEvents(long issueId)` separate; no audit field in the provider contract |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/rest/mapper/IssuesRequestMapper.java` | Change | Replace search mapping output with `SearchIssuesInput`; parse supported audit values; keep `IssueQuery` search-only |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/rest/mapper/IssuesResponseMapper.java` | Change | Map search results to `IssueDto`; omit `changeSets` when `null`; map orchestration `ChangeField.LABEL` to lower-case search transport enum |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/integration/gitlab/issues/mapper/GitLabIssuesMapper.java` | Change | Build base `Issue` objects with `changeSets=null` |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/rest/IssuesController.java` | Change | Search endpoint delegates `SearchIssuesInput`; controller logs normalized audit values and preserves existing create/delete/get-single flows |
| `flow-orchestrator/http/issues.http` | Change | Add audit-enabled search, invalid audit, omitted-`changeSets`, and updated default/max page examples |
| `documentation/capabilities/issues.md` | Change | Document `SearchIssuesInput`, `IssueDto.changeSets`, search audit transport subtypes, and fail-fast merge flow |

## Composition Strategy

- Dependent then parallel: `issuesPort.getIssues(input.query())` runs first because returned `issueId` values are required for enrichment
- Independent calls after search: if `auditTypes` contains `LABEL`, `issuesPort.getLabelEvents(issue.issueId())` runs in parallel for each returned item through `AsyncComposer.submit(...)` plus `joinFailFast(...)`
- Merge rule: preserve original search order by storing enrichment futures in item order and rebuilding the page by index
- No-op branch: if `auditTypes` is empty or search returns zero items, skip all label-event calls

## Shared Infrastructure Impact

- Reused shared mechanisms: `AsyncComposer`, `IssuesPort.getLabelEvents(long issueId)`, `GitLabLabelEventMapper`, shared GitLab error mapping, shared REST exception mapping
- New shared extractions: None; this is a reuse of existing shared async composition rather than a second local implementation

## Implementation Slices

### Slice 1 - Search Boundary And Single DTO Strategy

- Goal: add `filters.audit`, keep `IssueDto` as the sole shared item DTO, and preserve the current non-audit search wire shape by omitting only `changeSets` when it is `null`
- Affected scope: `IssuesApiProperties`, `application.yml`, `Issue`, `SearchIssuesInput`, `IssueAuditType`, `IssueFiltersRequest`, `IssueDto`, `IssuesRequestMapper`, `IssuesResponseMapper`, `IssuesController`
- Payload / contract impact: request accepts `filters.audit`; search/create/delete continue to use `IssueDto`; only audit-enabled search items include `changeSets`
- Validation boundary decisions: sanitize `audit` in `IssueFiltersRequest`; validate supported audit values in `IssuesRequestMapper`; keep max-page enforcement in `IssuesService`
- Unit tests: update `IssueFiltersRequestTest`, `IssuesRequestMapperTest`, `IssuesResponseMapperTest`; add `IssueDtoTest` and `IssueTest` for defensive-copy behavior and `changeSets` null omission serialization
- Integration / Web tests: update `IssuesControllerIT` for `audit=["label"]`, `audit=[]`, `audit=null`, `audit=[null,"label"]`, unsupported audit, and default pagination `20`
- Edge / failure coverage: `audit=["milestone"]`, `perPage=41`, non-audit search item must not serialize `changeSets`
- INFO logging: controller logs page, perPage, normalized filters, and normalized audit values; completion logs count/page
- WARN logging: None; validation warnings remain centralized in the existing exception flow
- ERROR logging: None
- Documentation updates: `flow-orchestrator/http/issues.http`, `documentation/capabilities/issues.md`

### Slice 2 - Conditional Label-Event Enrichment

- Goal: enrich search results only for `audit=label`, using fail-fast parallel label-event calls and mapping enrichment into the shared `Issue` model
- Affected scope: `IssuesService`, `GitLabIssuesMapper`, search component stubs, and existing search Karate smoke assertions
- Payload / contract impact: audit-enabled items include `changeSets`; empty label history serializes as `[]`; `change.field` serializes as `"label"`
- Validation boundary decisions: none beyond consuming normalized `SearchIssuesInput.auditTypes`
- Unit tests: update `IssuesServiceTest` for no-audit no-call branch, zero-result branch, audit enrichment branch, empty-history branch, and single-failure full-failure propagation; update `GitLabIssuesMapperTest` for `changeSets=null`; keep adapter tests proving `IssueQuery` stays audit-free
- Integration / Web tests: extend `IssuesControllerIT` to verify omitted versus present `changeSets`; extend `IssuesApiComponentTest` to verify one label-event call per returned issue, zero calls when audit absent, empty `changeSets` when no history, and fail-fast error propagation when one enrichment call fails
- Edge / failure coverage: multiple results with one failing enrichment call, returned issue with no label events, search result order preserved after enrichment
- INFO logging: `IssuesService.getIssues` logs normalized audit types, search result count, enrichment branch taken, total change-set count, and durationMs
- WARN logging: None
- ERROR logging: None in orchestration; existing adapter transport-failure logging remains unchanged
- Documentation updates: `flow-orchestrator/http/issues.http`, `documentation/capabilities/issues.md`

## Testing Matrix and Verification

| Level | Required | Planned Coverage | Evidence Target |
|---|---|---|---|
| Unit | Yes | `IssueTest`, `IssueDtoTest`, `IssueFiltersRequestTest`, `IssuesRequestMapperTest`, `IssuesResponseMapperTest`, `IssuesServiceTest`, `GitLabIssuesMapperTest`, `GitLabIssuesAdapterTest` | Green `scripts/verify-quick.sh` |
| Integration / Web | Yes | `IssuesControllerIT` for request binding, validation, defaulting, omitted-vs-present `changeSets`, and error translation | Green `scripts/verify-quick.sh` |
| Component | Yes | `IssuesApiComponentTest` with search + label-event stubs for no-audit, audit success, empty history, and full-failure behavior | Green `scripts/verify-quick.sh` |
| Karate smoke | Yes | Existing `src/test/karate/resources/issues/issues-search.feature` scenarios for non-audit omission, audit enrichment, and invalid audit validation remain the smoke contract | Green `scripts/karate-test.sh` |
| ArchUnit | No new rule | Existing `FlowOrchestratorArchitectureTest` already covers the touched layers; no new package boundary or dependency direction is introduced | Existing architecture suite stays green |

### Karate Checklist

- [x] `.feature` file already exists under `src/test/karate/resources/issues/`
- [x] Scenario names, HTTP method, endpoint path, expected status codes, and key response assertions are already defined for the changed endpoint
- [x] Smoke scenarios are tagged with `@smoke`
- [x] Karate runner change is not needed because this is not a new capability

### ArchUnit Checklist

- [x] Existing rules in `FlowOrchestratorArchitectureTest` reviewed; plan decisions comply
- [x] No new layer interaction or package boundary requires a new ArchUnit rule

## Final Verification Expectations

- `cd flow-orchestrator && ../scripts/verify-quick.sh`
- `cd flow-orchestrator && ../scripts/final-check.sh`
- `cd scripts && ./karate-test.sh`
- `cd flow-orchestrator && SPRING_PROFILES_ACTIVE=local mvn spring-boot:run`
- Evidence must record exact commands, observed outcomes, and current report paths under `flow-orchestrator/target/`# Implementation Plan: Search Enhancement API - Audit-Enriched Issue Search

**Artifact path:** `artifacts/implementation-plans/search-enhancement-api.plan.md`
**Task name:** `search-enhancement-api`
**Plan date:** `2026-04-09`
**Status:** `Revised`

## Business Goal

Extend `POST /api/issues/search` so clients can request label-audit enrichment in one call while keeping GitLab search concerns out of the provider port and keeping `IssueDto` as the shared search/create/delete entity DTO.

## Requirement Lock / Source Of Truth

- Request source: `artifacts/user-prompts/search-enhancement-api.md`
- Story: `artifacts/user-stories/search-enhancement-api.story.md`
- Locked endpoint scope: update only `POST /api/issues/search`
- Locked request rule: add optional `filters.audit` array; only `"label"` is supported now; `null` elements are ignored; missing, `null`, and `[]` are equivalent
- Locked orchestration rule: run issue search first, then fetch per-issue label events in parallel through `AsyncComposer`, then merge them into returned issues as `changeSets`
- Locked model rule: extend orchestration `Issue` with optional `changeSets`; reuse existing orchestration `ChangeSet` / `Change` / label-event models
- Locked transport rule: keep one primary issue item DTO across search/create/delete; non-audit search must keep its current wire shape; audit-enabled search adds `changeSets`
- Locked failure rule: any required label-event failure fails the whole request; no partial enrichment
- Locked pagination rule: default `perPage=20`, max `perPage=40`
- GitLab docs verified: `GET /projects/:id/issues` supports `page`, `per_page`, `state`, `labels`, `assignee_username`, `milestone`; `GET /projects/:id/issues/:issue_iid/resource_label_events` returns `user`, `created_at`, `label`, and `action`
- Explicit assumption: GitLab label-event order is consumed as returned; orchestration does not re-sort events

## Payload Examples

```json
// Request: default search without audit
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

```json
// Request: search with label audit enrichment
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

```json
// Success response: audit requested and history found
{
  "items": [
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
  ],
  "count": 1,
  "page": 1
}
```

```json
// Success response: audit absent or empty
{
  "items": [
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
  ],
  "count": 1,
  "page": 1
}
```

```json
// Error response: enrichment call fails
{
  "code": "INTEGRATION_FAILURE",
  "message": "GitLab get label events operation failed",
  "details": []
}
```

```json
// Validation error response: unsupported audit value
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    "filters.audit must contain only supported values [label]"
  ]
}
```

## Validation Boundary Decision

| Validation Rule | Boundary | Why Here |
|---|---|---|
| `pagination.page` and `pagination.perPage` must be positive | REST DTO binding (`PaginationRequest`) | Declarative scalar validation belongs at HTTP binding |
| `labels`, `assignee`, `milestone` remain single-value lists and drop `null` elements | `IssueFiltersRequest` compact constructor + existing `@Size(max=1)` | Existing transport normalization stays at the request boundary |
| `audit` may be missing, `null`, empty, or contain `null` elements | `IssueFiltersRequest` compact constructor | Request-shape normalization belongs at the boundary |
| `audit` may contain only supported values | `IssuesRequestMapper` when building `SearchIssuesInput` | Supported audit types are orchestration semantics, not generic bean validation |
| `perPage <= 40` | `IssuesService.validatePerPage` using `IssuesApiProperties.maxPageSize()` | Existing runtime guard remains the single owner of configurable page-size enforcement |

## Scope

### In Scope

- Add `filters.audit` handling for search only
- Introduce orchestration-only audit instruction model separate from the adapter-facing search query
- Extend orchestration `Issue` with optional `changeSets`
- Reuse existing `ChangeSet`, `Change`, `LabelChangeSet`, `LabelChange`, and `IssuesPort.getLabelEvents(long issueId)`
- Keep `IssueDto` as the shared search/create/delete entity DTO and omit `changeSets` when it is `null`
- Use search-only audit transport subtypes only for the `changeSets` payload so `change.field` serializes as lower-case `label` without changing get-single contract
- Update default/max page size to `20` / `40`
- Update `flow-orchestrator/http/issues.http`, `documentation/capabilities/issues.md`, and existing search Karate smoke assertions

### Out of Scope

- Support for audit values beyond `label`
- New endpoints, new ports, or adapter-side composition
- Partial-success enrichment behavior
- Changing get-single response shape or its existing `IssueDetailDto` change-set transport

## Class Structure

### Model Definitions

| Model | Type | Fields / Contract | Notes |
|---|---|---|---|
| `com.gitlabflow.floworchestrator.orchestration.issues.model.Issue` | changed record | `long id`, `long issueId`, `String title`, `@Nullable String description`, `String state`, `List<String> labels`, `@Nullable String assignee`, `@Nullable String milestone`, `@Nullable Long parent`, `@Nullable List<ChangeSet> changeSets` | Keep `@Builder`; defensively copy `labels`; preserve `changeSets=null` when audit not requested, else `List.copyOf(changeSets)` |
| `com.gitlabflow.floworchestrator.orchestration.issues.model.SearchIssuesInput` | new record | `IssueQuery query`, `List<IssueAuditType> auditTypes` | Keep `@Builder`; defensively copy `auditTypes`; normalize `null` to empty list |
| `com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery` | unchanged record | `int page`, `int perPage`, `@Nullable IssueState state`, `@Nullable String label`, `@Nullable String assignee`, `@Nullable String milestone` | Remains the adapter-facing GitLab search contract; no audit field |
| `com.gitlabflow.floworchestrator.orchestration.issues.model.IssueAuditType` | new enum | value: `LABEL("label")`; methods: `String value()`, `@Nullable static IssueAuditType fromValue(String raw)` | Future audit types append here without changing the port contract |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueFiltersRequest` | changed record | existing fields plus `List<String> audit` | Keep `@Builder`; sanitize all list fields by dropping `null` elements and returning immutable copies |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDto` | changed record | existing fields plus `@Nullable List<IssueAuditChangeSetDto> changeSets` | Keep `@Builder`; defensively copy `labels`; preserve `changeSets=null` or copy; annotate only `changeSets` for null omission so other nullable fields keep current behavior |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDto.IssueAuditChangeSetDto` | new sealed interface | accessors: `String changeType()`, `IssueAuditChangedByDto changedBy()`, `IssueAuditChangeDto change()`, `OffsetDateTime changedAt()` | Permits `IssueAuditLabelChangeSetDto`; DTO-only transport for search audit payload |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDto.IssueAuditChangeDto` | new sealed interface | accessors: `IssueAuditChangeField field()`, `long id()`, `String name()` | Permits `IssueAuditLabelChangeDto` |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDto.IssueAuditLabelChangeSetDto` / `IssueAuditChangedByDto` / `IssueAuditLabelChangeDto` | new records | `IssueAuditLabelChangeSetDto(String changeType, IssueAuditChangedByDto changedBy, IssueAuditLabelChangeDto change, OffsetDateTime changedAt)`; `IssueAuditChangedByDto(long id, String username, String name)`; `IssueAuditLabelChangeDto(IssueAuditChangeField field, long id, String name)` | Keep `@Builder`; search-only transport types that mirror orchestration change-set contracts |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDto.IssueAuditChangeField` | new enum | value: `LABEL("label")`; methods: `String value()`, `static IssueAuditChangeField from(ChangeField field)` | Serialize as lower-case `label`; isolates search transport from existing get-single enum serialization |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesResponse` | unchanged record | `List<IssueDto> items`, `int count`, `int page` | Wrapper stays unchanged because item DTO remains `IssueDto` |
| `com.gitlabflow.floworchestrator.orchestration.issues.model.ChangeSet` / `Change` / `LabelChangeSet` / `LabelChange` / `ChangedBy` | unchanged interfaces / records | Existing accessor contracts remain exactly as implemented today | Reused orchestration model; no duplicate business models |

### Affected Classes

| Class Path | Status | Proposed Behavior |
|---|---|---|
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/config/IssuesApiProperties.java` | Change | Tighten config max to `40`; keep startup guard `default<=max` |
| `flow-orchestrator/src/main/resources/application.yml` | Change | Set `app.issues-api.default-page-size=20` and `max-page-size=40` |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/IssuesService.java` | Change | Accept `SearchIssuesInput`; validate `perPage`; call `issuesPort.getIssues(input.query())`; if `auditTypes` contains `LABEL`, fetch label events in parallel, fail fast, and merge `changeSets` into copied `Issue` records while preserving item order |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/IssuesPort.java` | Reuse unchanged | Keep `getIssues(IssueQuery)` and `getLabelEvents(long issueId)` separate; no audit field in the provider contract |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/rest/mapper/IssuesRequestMapper.java` | Change | Replace search mapping output with `SearchIssuesInput`; parse supported audit values; keep `IssueQuery` search-only |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/rest/mapper/IssuesResponseMapper.java` | Change | Map search results to `IssueDto`; omit `changeSets` when `null`; map orchestration `ChangeField.LABEL` to lower-case search transport enum |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/integration/gitlab/issues/mapper/GitLabIssuesMapper.java` | Change | Build base `Issue` objects with `changeSets=null` |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/rest/IssuesController.java` | Change | Search endpoint delegates `SearchIssuesInput`; controller logs normalized audit values and preserves existing create/delete/get-single flows |
| `flow-orchestrator/http/issues.http` | Change | Add audit-enabled search, invalid audit, omitted-`changeSets`, and updated default/max page examples |
| `documentation/capabilities/issues.md` | Change | Document `SearchIssuesInput`, `IssueDto.changeSets`, search audit transport subtypes, and fail-fast merge flow |

## Composition Strategy

- Dependent then parallel: `issuesPort.getIssues(input.query())` runs first because returned `issueId` values are required for enrichment
- Independent calls after search: if `auditTypes` contains `LABEL`, `issuesPort.getLabelEvents(issue.issueId())` runs in parallel for each returned item through `AsyncComposer.submit(...)` plus `joinFailFast(...)`
- Merge rule: preserve original search order by storing enrichment futures in item order and rebuilding the page by index
- No-op branch: if `auditTypes` is empty or search returns zero items, skip all label-event calls

## Shared Infrastructure Impact

- Reused shared mechanisms: `AsyncComposer`, `IssuesPort.getLabelEvents(long issueId)`, `GitLabLabelEventMapper`, shared GitLab error mapping, shared REST exception mapping
- New shared extractions: None; this remains a reuse of existing shared async composition rather than a second local implementation

## Implementation Slices

### Slice 1 - Search Boundary And Shared DTO Reuse

- Goal: add `filters.audit`, keep `IssueDto` as the shared item DTO, and preserve the current non-audit search wire shape by omitting only `changeSets` when it is `null`
- Affected scope: `IssuesApiProperties`, `application.yml`, `Issue`, `SearchIssuesInput`, `IssueAuditType`, `IssueFiltersRequest`, `IssueDto`, `IssuesRequestMapper`, `IssuesResponseMapper`, `IssuesController`
- Payload / contract impact: request accepts `filters.audit`; search/create/delete continue to use `IssueDto`; only audit-enabled search items include `changeSets`
- Validation boundary decisions: sanitize `audit` in `IssueFiltersRequest`; validate supported audit values in `IssuesRequestMapper`; keep max-page enforcement in `IssuesService`
- Unit tests: update `IssueFiltersRequestTest`, `IssuesRequestMapperTest`, `IssuesResponseMapperTest`, `IssueDtoTest`, and `IssueTest` for null-preserving `changeSets` defensive-copy behavior and null omission serialization
- Integration / Web tests: update `IssuesControllerIT` for `audit=["label"]`, `audit=[]`, `audit=null`, `audit=[null,"label"]`, unsupported audit, and default pagination `20`
- Edge / failure coverage: `audit=["milestone"]`, `perPage=41`, non-audit search item must not serialize `changeSets`
- INFO logging: controller logs page, perPage, normalized filters, and normalized audit values; completion logs count/page
- WARN logging: None; validation warnings remain centralized in the existing exception flow
- ERROR logging: None
- Documentation updates: `flow-orchestrator/http/issues.http`, `documentation/capabilities/issues.md`

### Slice 2 - Conditional Label-Event Enrichment

- Goal: enrich search results only for `audit=label`, using fail-fast parallel label-event calls and mapping enrichment into the shared `Issue` model
- Affected scope: `IssuesService`, `GitLabIssuesMapper`, search component stubs, and existing search Karate smoke assertions
- Payload / contract impact: audit-enabled items include `changeSets`; empty label history serializes as `[]`; `change.field` serializes as `"label"`
- Validation boundary decisions: none beyond consuming normalized `SearchIssuesInput.auditTypes`
- Unit tests: update `IssuesServiceTest` for no-audit no-call branch, zero-result branch, audit enrichment branch, empty-history branch, and single-failure full-failure propagation; update `GitLabIssuesMapperTest` for `changeSets=null`; keep adapter tests proving `IssueQuery` stays audit-free
- Integration / Web tests: extend `IssuesControllerIT` to verify omitted versus present `changeSets`; extend `IssuesApiComponentTest` to verify one label-event call per returned issue, zero calls when audit absent, empty `changeSets` when no history, and fail-fast error propagation when one enrichment call fails
- Edge / failure coverage: multiple results with one failing enrichment call, returned issue with no label events, search result order preserved after enrichment
- INFO logging: `IssuesService.getIssues` logs normalized audit types, search result count, enrichment branch taken, total change-set count, and durationMs
- WARN logging: None
- ERROR logging: None in orchestration; existing adapter transport-failure logging remains unchanged
- Documentation updates: `flow-orchestrator/http/issues.http`, `documentation/capabilities/issues.md`

## Testing Matrix And Verification

| Level | Required | Planned Coverage | Evidence Target |
|---|---|---|---|
| Unit | Yes | `IssueTest`, `IssueDtoTest`, `IssueFiltersRequestTest`, `IssuesRequestMapperTest`, `IssuesResponseMapperTest`, `IssuesServiceTest`, `GitLabIssuesMapperTest`, `GitLabIssuesAdapterTest` | Green `scripts/verify-quick.sh` |
| Integration / Web | Yes | `IssuesControllerIT` for request binding, validation, defaulting, omitted-vs-present `changeSets`, and error translation | Green `scripts/verify-quick.sh` |
| Component | Yes | `IssuesApiComponentTest` with search + label-event stubs for no-audit, audit success, empty history, and full-failure behavior | Green `scripts/verify-quick.sh` |
| Karate smoke | Yes | Existing `src/test/karate/resources/issues/issues-search.feature` scenarios for non-audit omission, audit enrichment, and invalid audit validation remain the smoke contract | Green `scripts/karate-test.sh` |
| ArchUnit | No new rule | Existing `FlowOrchestratorArchitectureTest` already covers the touched layers; no new package boundary or dependency direction is introduced | Existing architecture suite stays green |

## Final Verification Expectations

- `cd flow-orchestrator && ../scripts/verify-quick.sh`
- `cd flow-orchestrator && ../scripts/final-check.sh`
- `cd scripts && ./karate-test.sh`
- `cd flow-orchestrator && SPRING_PROFILES_ACTIVE=local mvn spring-boot:run`
- Evidence must record exact commands, observed outcomes, and current report paths under `flow-orchestrator/target/`# Implementation Plan: Search Enhancement API — Audit-Enriched Issue Search

**Artifact path:** `artifacts/implementation-plans/search-enhancement-api.plan.md`
**Task name:** `search-enhancement-api`
**Plan date:** `2026-04-09`
**Status:** `Revised`

## Business Goal

Extend `POST /api/issues/search` so clients can request label-audit enrichment in a single search call, while keeping GitLab search concerns isolated in the port contract and preserving the current non-audit response shape.

## Requirement Lock / Source Of Truth

- Original request source: `artifacts/user-prompts/search-enhancement-api.md`
- Story: `artifacts/user-stories/search-enhancement-api.story.md`
- Locked scope: update only `POST /api/issues/search`
- Locked request: add optional `filters.audit` array; only `"label"` is supported now; `null` elements are ignored; missing, `null`, and `[]` behave the same
- Locked orchestration rule: search first, then fetch per-issue label events in parallel via existing `AsyncComposer`, then merge into returned search items
- Locked model rule: extend orchestration `Issue` with optional `changeSets`; reuse existing orchestration `ChangeSet` / `Change` / label-event models
- Locked response rule: non-audit search must keep the current item shape; audit-enabled search adds `changeSets`; empty histories return `[]`
- Locked failure rule: any required label-event failure fails the whole request; no partial enrichment
- Locked pagination rule: default `perPage=20`, max `perPage=40`
- GitLab docs verified: `GET /projects/:id/issues` supports `page`, `per_page`, `state`, `labels`, `assignee_username`, `milestone`; `GET /projects/:id/issues/:issue_iid/resource_label_events` returns `user`, `created_at`, `label`, and `action`
- Explicit assumption: GitLab label-event order is preserved as returned; orchestration does not reorder events

## Payload Examples

```json
// Request: default search without audit
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

```json
// Request: search with label audit enrichment
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

```json
// Success response: audit requested and history found
{
  "items": [
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
  ],
  "count": 1,
  "page": 1
}
```

```json
// Success response: audit absent or empty
{
  "items": [
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
  ],
  "count": 1,
  "page": 1
}
```

```json
// Error response: enrichment call fails
{
  "code": "INTEGRATION_FAILURE",
  "message": "GitLab get label events operation failed",
  "details": []
}
```

```json
// Validation error response: unsupported audit value
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    "filters.audit must contain only supported values [label]"
  ]
}
```

## Validation Boundary Decision

| Validation Rule | Boundary | Why Here |
|---|---|---|
| `pagination.page` and `pagination.perPage` must be positive | REST DTO binding (`PaginationRequest`) | Standard scalar validation belongs at HTTP binding |
| `labels`, `assignee`, `milestone` remain single-value lists and drop `null` elements | `IssueFiltersRequest` compact constructor + existing `@Size(max=1)` | This is transport normalization, not orchestration behavior |
| `audit` may be missing, `null`, empty, or contain `null` elements | `IssueFiltersRequest` compact constructor | Request-shape normalization belongs at the boundary |
| `audit` may contain only supported values | `IssuesRequestMapper` when building orchestration input | This is project-specific semantic validation that still must ignore `null` entries |
| `perPage <= 40` | `IssuesService.validatePerPage` using `IssuesApiProperties.maxPageSize()` | Existing runtime guard remains the single owner of configurable page-size enforcement |

## Scope

### In Scope

- Add `filters.audit` request handling for search only
- Introduce orchestration-only audit instruction model separate from the adapter-facing search query
- Extend orchestration `Issue` with optional `changeSets`
- Reuse existing `ChangeSet`, `Change`, `LabelChangeSet`, `LabelChange`, and `IssuesPort.getLabelEvents(long issueId)`
- Keep non-audit search responses unchanged by using a search-only transport DTO that omits `changeSets` when `null`
- Serialize search `change.field` as lower-case `"label"`
- Update default/max page size to `20` / `40`
- Update `.http`, capability docs, and Karate smoke coverage

### Out of Scope

- Support for audit values beyond `label`
- New endpoints, new ports, or adapter-side composition
- Partial-success enrichment behavior
- Changing get-single label-event transport or create endpoint payloads

## Class Structure

### Model Definitions

| Model | Type | Fields / Contract | Notes |
|---|---|---|---|
| `com.gitlabflow.floworchestrator.orchestration.issues.model.Issue` | changed record | `long id`, `long issueId`, `String title`, `@Nullable String description`, `String state`, `List<String> labels`, `@Nullable String assignee`, `@Nullable String milestone`, `@Nullable Long parent`, `@Nullable List<ChangeSet> changeSets` | Keep `@Builder`; defensively copy `labels`; preserve `changeSets=null` when audit not requested, else `List.copyOf(changeSets)` |
| `com.gitlabflow.floworchestrator.orchestration.issues.model.SearchIssuesInput` | new record | `IssueQuery query`, `List<IssueAuditType> auditTypes` | Keep `@Builder`; `auditTypes` copied to immutable empty list; orchestration-only input for search use case |
| `com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery` | unchanged record | `int page`, `int perPage`, `@Nullable IssueState state`, `@Nullable String label`, `@Nullable String assignee`, `@Nullable String milestone` | Remains the adapter-facing GitLab search contract; no audit field |
| `com.gitlabflow.floworchestrator.orchestration.issues.model.IssueAuditType` | new enum | values: `LABEL("label")`; methods: `String value()`, `@Nullable static IssueAuditType fromValue(String raw)` | Future values append here without changing the port contract |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueFiltersRequest` | changed record | existing fields plus `List<String> audit` | Keep `@Builder`; sanitize all list fields by dropping `null` elements; return immutable copies |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssueDto` | new record | `long id`, `long issueId`, `String title`, `@Nullable String description`, `String state`, `List<String> labels`, `@Nullable String assignee`, `@Nullable String milestone`, `@Nullable Long parent`, `@Nullable List<SearchIssueChangeSetDto> changeSets` | Keep `@Builder`; `labels` copied to immutable list; `changeSets` preserved as `null` or copied; annotate `changeSets` with null omission so non-audit search stays unchanged |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssueDto.SearchIssueChangeSetDto` | new sealed interface | accessors: `String changeType()`, `SearchIssueChangedByDto changedBy()`, `SearchIssueChangeDto change()`, `OffsetDateTime changedAt()` | Permits `SearchIssueLabelChangeSetDto` |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssueDto.SearchIssueChangeDto` | new sealed interface | accessors: `SearchIssueChangeField field()`, `long id()`, `String name()` | Permits `SearchIssueLabelChangeDto` |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssueDto.SearchIssueLabelChangeSetDto` / `SearchIssueChangedByDto` / `SearchIssueLabelChangeDto` | new records | `SearchIssueLabelChangeSetDto(String changeType, SearchIssueChangedByDto changedBy, SearchIssueLabelChangeDto change, OffsetDateTime changedAt)`; `SearchIssueChangedByDto(long id, String username, String name)`; `SearchIssueLabelChangeDto(SearchIssueChangeField field, long id, String name)` | Keep `@Builder`; DTO-only transport types for search audit payload |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssueDto.SearchIssueChangeField` | new enum | value: `LABEL("label")`; methods: `String value()`, `static SearchIssueChangeField from(ChangeField field)` | Serialize as lower-case `label`; avoids changing get-single transport |
| `com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesResponse` | changed record | `List<SearchIssueDto> items`, `int count`, `int page` | Search wrapper switches to search-specific item DTO; create endpoint keeps `IssueDto` unchanged |
| `com.gitlabflow.floworchestrator.orchestration.issues.model.ChangeSet` / `Change` / `LabelChangeSet` / `LabelChange` / `ChangedBy` | unchanged interfaces / records | Existing accessor contracts remain exactly as implemented today | Reused orchestration model; no duplicate business models |

### Affected Classes

| Class Path | Status | Proposed Behavior |
|---|---|---|
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/config/IssuesApiProperties.java` | Change | Tighten config max to `40`; keep startup guard `default<=max` |
| `flow-orchestrator/src/main/resources/application.yml` | Change | Set `app.issues-api.default-page-size=20` and `max-page-size=40` |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/IssuesService.java` | Change | Accept `SearchIssuesInput`; validate `perPage`; call `issuesPort.getIssues(input.query())`; if `auditTypes` contains `LABEL`, fetch label events in parallel, fail fast, and merge `changeSets` into copied `Issue` records while preserving item order |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/IssuesPort.java` | Reuse unchanged | Keep `getIssues(IssueQuery)` and `getLabelEvents(long issueId)` separate; no audit pollution in the port contract |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/rest/mapper/IssuesRequestMapper.java` | Change | Map REST request to `SearchIssuesInput`; parse supported audit values; keep `IssueQuery` search-only |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/rest/mapper/IssuesResponseMapper.java` | Change | Map search `Issue` items to `SearchIssueDto`; omit `changeSets` when `null`; map orchestration `ChangeField.LABEL` to search transport `"label"` |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/integration/gitlab/issues/mapper/GitLabIssuesMapper.java` | Change | Build base `Issue` objects with `changeSets=null` |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/rest/IssuesController.java` | Change | Search endpoint remains `POST /api/issues/search`; controller logs normalized audit values and delegates `SearchIssuesInput` |
| `flow-orchestrator/http/issues.http` | Change | Add audit-enabled search, invalid audit, omitted-`changeSets` example, and updated default/max page examples |
| `documentation/capabilities/issues.md` | Change | Document `SearchIssuesInput`, search-only response DTO, orchestration merge flow, and non-audit omission rule |

## Composition Strategy

- **Dependent then parallel:** `issuesPort.getIssues(input.query())` runs first because returned `issueId` values are required for enrichment.
- **Independent calls after search:** if `auditTypes` contains `LABEL`, `issuesPort.getLabelEvents(issue.issueId())` runs in parallel for each returned item through `AsyncComposer.submit(...)` plus `joinFailFast(...)`.
- **Merge rule:** preserve original search order by storing enrichment futures in item order and rebuilding the `IssuePage` by index.
- **No-op branch:** if `auditTypes` is empty or the search returns zero items, skip all label-event calls.

## Shared Infrastructure Impact

- Reused shared mechanisms: `AsyncComposer`, `IssuesPort.getLabelEvents(long issueId)`, `GitLabLabelEventMapper`, shared GitLab error mapping, shared REST exception mapping
- New shared extractions: None; this is a second use of existing shared async composition, so reuse is correct

## Implementation Slices

### Slice 1 - Search Contract Boundary

- Goal: add `filters.audit`, preserve the existing non-audit search wire shape, and keep audit instructions out of the provider-facing query
- Affected scope: `IssuesApiProperties`, `application.yml`, `Issue`, `SearchIssuesInput`, `IssueAuditType`, `IssueFiltersRequest`, `SearchIssueDto`, `SearchIssuesResponse`, `IssuesRequestMapper`, `IssuesResponseMapper`, `IssuesController`
- Payload / contract impact: request accepts `filters.audit`; non-audit search omits `changeSets`; invalid audit returns `400`; create response remains unchanged
- Validation boundary decisions: sanitize `audit` in `IssueFiltersRequest`; validate supported audit values in `IssuesRequestMapper`; keep max-page enforcement in `IssuesService`
- Unit tests: update `IssueFiltersRequestTest`, `IssuesRequestMapperTest`, `IssuesResponseMapperTest`, `SearchIssuesResponseTest`; add `IssueTest` for null-preserving `changeSets` defensive copy
- Integration / Web tests: update `IssuesControllerIT` for `audit=["label"]`, `audit=[]`, `audit=null`, `audit=[null,"label"]`, unsupported audit, and default pagination `20`
- Edge / failure coverage: `audit=["milestone"]`, `perPage=41`, non-audit search item must not serialize `changeSets`
- INFO logging: controller logs page, perPage, normalized filters, and normalized audit values; response count/page logged on completion
- WARN logging: validation failures continue through existing exception handler logging only
- ERROR logging: None
- Documentation updates: `flow-orchestrator/http/issues.http`, `documentation/capabilities/issues.md`

### Slice 2 - Conditional Label-Event Enrichment

- Goal: enrich search results only for `audit=label`, using fail-fast parallel label-event calls and lower-case search transport for `change.field`
- Affected scope: `IssuesService`, `GitLabIssuesMapper`, component stubs for search + label events, and search Karate smoke coverage
- Payload / contract impact: audit-enabled items include `changeSets`; empty label history serializes as `[]`; `change.field` serializes as `"label"`
- Validation boundary decisions: none beyond consuming already-normalized `SearchIssuesInput.auditTypes`
- Unit tests: update `IssuesServiceTest` for no-audit no-call branch, zero-result branch, audit enrichment branch, empty-history branch, and single-failure full-failure propagation; update `GitLabIssuesMapperTest` for `changeSets=null`; keep adapter tests proving `IssueQuery` stays audit-free
- Integration / Web tests: extend `IssuesControllerIT` to verify omitted vs present `changeSets`; extend `IssuesApiComponentTest` to verify one label-event call per returned issue, zero calls when audit absent, empty `changeSets` when no history, and fail-fast error propagation when one enrichment call fails
- Edge / failure coverage: multiple results with one failing enrichment call, returned issue with no label events, search result order preserved after enrichment
- INFO logging: `IssuesService.getIssues` logs normalized audit types, search result count, enrichment branch taken, total change-set count, and durationMs
- WARN logging: no new WARN logs in orchestration; adapter logging remains unchanged
- ERROR logging: None in orchestration; adapter existing transport failure logging remains unchanged
- Documentation updates: `flow-orchestrator/http/issues.http`, `documentation/capabilities/issues.md`

## Testing Matrix And Verification

| Level | Required | Planned Coverage | Evidence Target |
|---|---|---|---|
| Unit | Yes | `IssueTest`, `IssueFiltersRequestTest`, `IssuesRequestMapperTest`, `IssuesResponseMapperTest`, `SearchIssuesResponseTest`, `IssuesServiceTest`, `GitLabIssuesMapperTest`, `GitLabIssuesAdapterTest` | Green `scripts/verify-quick.sh` |
| Integration / Web | Yes | `IssuesControllerIT` for request binding, validation, omitted-vs-present `changeSets`, and error translation | Green `scripts/verify-quick.sh` |
| Component | Yes | `IssuesApiComponentTest` with search + label-event stubs for no-audit, audit success, empty history, and full-failure behavior | Green `scripts/verify-quick.sh` |
| Karate smoke | Yes | `src/test/karate/resources/issues/issues-search.feature` for non-audit omission, audit enrichment, and invalid audit validation | Green `scripts/karate-test.sh` |
| ArchUnit | No new rule | Existing `FlowOrchestratorArchitectureTest` already covers the touched layers; no new package boundary or forbidden dependency is introduced | Existing architecture suite stays green |

### Karate checklist

- [x] `.feature` file updated under `src/test/karate/resources/issues/`
- [x] Scenario names, HTTP methods, endpoint path, expected status codes, and key response assertions are defined
- [x] Smoke scenarios remain tagged with `@smoke`
- [x] Karate runner update not required because the capability already exists

### ArchUnit checklist

- [x] Existing rules in `FlowOrchestratorArchitectureTest.java` reviewed; plan remains compliant
- [x] No new ArchUnit rule needed because no new package or boundary is introduced

## Final Verification Expectations

- `cd flow-orchestrator && ../scripts/verify-quick.sh`
- `cd flow-orchestrator && ../scripts/final-check.sh`
- `cd scripts && ./karate-test.sh`
- `cd flow-orchestrator && SPRING_PROFILES_ACTIVE=local mvn spring-boot:run`
- Evidence must record exact commands, observed outcomes, and current report paths under `flow-orchestrator/target/`