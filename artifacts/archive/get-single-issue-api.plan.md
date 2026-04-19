# Implementation Plan: Get Single Issue API

**Artifact path:** `artifacts/implementation-plans/get-single-issue-api.plan.md`
**Task name:** `get-single-issue-api`
**Plan date:** `2026-04-08`
**Status:** `Draft`

## Business Goal

Provide clients with a `GET /api/issues/{issueId}` endpoint returning full single-issue detail from GitLab (title, description, state, labels, full assignee objects, full milestone object, timestamps), with `changeSets: []` reserved for Story 2, enabling drill-down from flow summaries without requiring direct GitLab access.

## Requirement Lock / Source Of Truth

- Original request source: `artifacts/user-prompts/get-single-issue-api.md`; story: `artifacts/user-stories/get-single-issue-api.story.md`
- Implement only Story 1 (`GET /api/issues/{issueId}`); label events / changeSets population is Story 2 — **locked out of scope**.
- Public `issueId` maps exclusively to GitLab `issue_iid` on the configured project; clients supply no project identifier.
- Pass-through only; two layers (integration + orchestration); no domain logic.
- Dedicated single-issue types required; must not overload `Issue`, `IssueDto`, or `GitLabIssueResponse`.
- `changeSets` must be present as `[]` in every response; never populated in Story 1.
- Integration fetches raw data only; mapping to client-facing response happens in orchestration layer or orchestration-facing mapper.
- Defaults: `labels: []`, `assignees: []`, `milestone: null`, `description: null`, `closedAt: null`.
- If GitLab fetch fails, return standard sanitized error; no partial response.
- Architecture must allow Story 2 `changeSets` addition without backward-incompatible response change.

## Payload Examples

```
// Request
GET /api/issues/42

// Success Response — HTTP 200, all fields populated
{
  "issueId": 42,
  "title": "Fix login bug",
  "description": "Users cannot log in with SSO",
  "state": "opened",
  "labels": ["bug", "high-priority"],
  "assignees": [
    { "id": 10, "username": "john.doe", "name": "John Doe" }
  ],
  "milestone": {
    "id": 5,
    "milestoneId": 3,
    "title": "Sprint 12",
    "state": "active",
    "dueDate": "2026-04-30"
  },
  "createdAt": "2026-01-04T15:31:51.081Z",
  "updatedAt": "2026-03-12T09:00:00.000Z",
  "closedAt": null,
  "changeSets": []
}

// Success Response — HTTP 200, null/empty defaults
{
  "issueId": 7,
  "title": "No description",
  "description": null,
  "state": "closed",
  "labels": [],
  "assignees": [],
  "milestone": null,
  "createdAt": "2026-02-01T00:00:00.000Z",
  "updatedAt": "2026-03-01T00:00:00.000Z",
  "closedAt": "2026-03-01T00:00:00.000Z",
  "changeSets": []
}

// Error Response — GitLab fetch failure, HTTP 502
{ "code": "INTEGRATION_ERROR", "message": "Integration error calling gitlab", "details": [] }

// Validation Error Response — non-positive issueId, HTTP 400
{ "code": "VALIDATION_ERROR", "message": "Request validation failed", "details": ["issueId must be a positive number"] }
```

## Validation Boundary Decision

| Validation Rule | Boundary | Why Here |
|---|---|---|
| `issueId > 0` | Controller (path variable) | System boundary — reject bad numeric input before reaching service or integration; same pattern as `deleteIssue` |
| `issueId is numeric` | Spring path-variable type coercion | Non-numeric values fail type conversion; handled by existing `MethodArgumentTypeMismatchException` handler in `GlobalExceptionHandler` |

## Scope

### In Scope

- `GET /api/issues/{issueId}` endpoint.
- New orchestration model `IssueDetail` with nested `AssigneeDetail` and `MilestoneDetail` records.
- New REST DTO `IssueDetailDto` with nested `AssigneeDto` and `MilestoneDto` records.
- New integration DTO `GitLabSingleIssueResponse` with full fields (timestamps, full assignees, full milestone).
- New integration mapper `GitLabIssueDetailMapper`.
- Extension of `IssuesPort` and `IssuesService` with `getIssueDetail(long issueId)`.
- Response mapper method `toIssueDetailDto(IssueDetail)` added to `IssuesResponseMapper`.
- New component fixture, stub support class, and test scenarios.
- Karate smoke feature `issues-get-single.feature`.
- `.http` and context-map documentation updates.

### Out of Scope

- Story 2 label history / changeSets population.
- Domain logic, aging metrics, flow state inference.
- Issue update, create, or multi-issue retrieval.
- Any new `*Properties` configuration class — no new config keys required.

## Class Structure

### Affected Classes

| Class (under `com.gitlabflow.floworchestrator`) | Status | Proposed Behavior |
|---|---|---|
| `orchestration/issues/model/IssueDetail` | Create | Record: `long issueId`, `String title`, `@Nullable String description`, `String state`, `List<String> labels`, `List<AssigneeDetail> assignees`, `@Nullable MilestoneDetail milestone`, `OffsetDateTime createdAt`, `OffsetDateTime updatedAt`, `@Nullable OffsetDateTime closedAt`, `List<Object> changeSets`; compact constructor normalizes null labels/assignees/changeSets to `List.of()`; inner records `AssigneeDetail(long id, String username, String name)` and `MilestoneDetail(long id, long milestoneId, String title, String state, @Nullable String dueDate)` |
| `orchestration/issues/rest/dto/IssueDetailDto` | Create | REST response record parallel to `IssueDetail`; same compact constructor null-normalization; inner records `AssigneeDto(long id, String username, String name)` and `MilestoneDto(long id, long milestoneId, String title, String state, @Nullable String dueDate)` |
| `integration/gitlab/issues/dto/GitLabSingleIssueResponse` | Create | `@JsonIgnoreProperties(ignoreUnknown = true)` record: `long id`, `long iid`, `String title`, `@Nullable String description`, `String state`, `List<String> labels`, `List<GitLabAssigneeDetail> assignees`, `@Nullable GitLabMilestoneDetail milestone`, `@JsonProperty("created_at") OffsetDateTime createdAt`, `@JsonProperty("updated_at") OffsetDateTime updatedAt`, `@JsonProperty("closed_at") @Nullable OffsetDateTime closedAt`; compact constructor normalizes null labels/assignees; inner records `GitLabAssigneeDetail(long id, String username, String name)` and `GitLabMilestoneDetail(long id, long iid, String title, String state, @JsonProperty("due_date") @Nullable String dueDate)` |
| `integration/gitlab/issues/mapper/GitLabIssueDetailMapper` | Create | `@Slf4j @Component`; maps `GitLabSingleIssueResponse` → `IssueDetail`; null-safe milestone mapping (`iid` → `milestoneId`); DEBUG log with `id`, `issueId`, `state`, `assigneeCount`, `milestonePresent` |
| `orchestration/issues/IssuesPort` | Modify | Add `IssueDetail getIssueDetail(long issueId)` |
| `orchestration/issues/IssuesService` | Modify | Add `getIssueDetail(long issueId)`: delegate to port; INFO log on entry and result |
| `orchestration/issues/rest/IssuesController` | Modify | Add `@GetMapping("/{issueId}")`: validate `issueId > 0`, call `issuesService.getIssueDetail`, map via `issuesResponseMapper.toIssueDetailDto`, return 200 with `IssueDetailDto` body |
| `orchestration/issues/rest/mapper/IssuesResponseMapper` | Modify | Add `IssueDetailDto toIssueDetailDto(IssueDetail issueDetail)`: maps all fields including nested assignees stream and null-safe milestone |
| `integration/gitlab/issues/GitLabIssuesAdapter` | Modify | Implement `getIssueDetail(long issueId)`: `executeGitLabOperation("get issue detail", () -> ...)` with `gitLabRestClient.get().uri(uriBuilder -> uriBuilder.path(...+ "/{issueId}").build(projectPath, issueId)).retrieve().body(GitLabSingleIssueResponse.class)`, null-body guard throws `IntegrationException`, map via `gitLabIssueDetailMapper.toIssueDetail(response)` |

## Implementation Slices

### Slice 1 — Get single issue detail (end-to-end pass-through)

- **Goal:** Deliver the complete `GET /api/issues/{issueId}` flow: new dedicated types, port method, adapter fetch, service delegation, mapping, controller endpoint.
- **Affected scope:** All classes in the Class Structure table.
- **Payload / contract impact:** New endpoint only — no change to existing endpoints or DTOs.
- **Validation boundary decisions:** Controller validates `issueId > 0`; throws `ValidationException("Request validation failed", List.of("issueId must be a positive number"))` matching `deleteIssue` pattern exactly.
- **Unit tests (`src/test/java`):**
  - `GitLabIssueDetailMapperTest` (CREATE): full response → all `IssueDetail` fields mapped correctly; null `description` → null; null `closedAt` → null; null `labels` → `[]`; null `assignees` → `[]`; null `milestone` → null; milestone with null `dueDate` maps cleanly; each test case is a single `@Test`.
  - `IssuesServiceTest` (MODIFY): add — `getIssueDetail` delegates to port and returns result; `getIssueDetail` propagates `IntegrationException` from port unchanged.
  - `IssuesResponseMapperTest` (MODIFY): add — `toIssueDetailDto` maps all fields; null `milestone` maps to null; empty `assignees` list maps to `[]`; `changeSets` is always `[]`.
  - `GitLabIssuesAdapterTest` (MODIFY): add — `getIssueDetail` calls correct GitLab URI and returns mapped `IssueDetail`; null body from GitLab client throws `IntegrationException`.
- **Integration / web tests (`src/test/integration/java`):**
  - `IssuesControllerIT` (MODIFY): add — GET with valid `issueId` returns 200 + `IssueDetailDto` JSON fields; `issueId = 0` returns 400 `VALIDATION_ERROR`; negative `issueId` returns 400 `VALIDATION_ERROR`; `IntegrationException` from service returns 502.
- **Component tests (`src/test/component/java`):**
  - Create `stubs/issues/gitlab-single-issue-detail.json` — full GitLab single-issue response including timestamps, full assignee objects (`id`, `username`, `name`), milestone with `due_date`.
  - Create `issues/support/GitLabSingleIssueStubSupport` (CREATE): `stubGetIssueDetail(server, issueId)` and `stubGetIssueDetailNotFound(server, issueId)` stub methods; `verifyGetIssueDetailRequest(server, issueId)`.
  - `IssuesApiComponentTest` (MODIFY): add — GET returns 200 with mapped `IssueDetailDto` fields (issueId, title, assignees array, milestone object, changeSets array); GET with GitLab 404 returns 502.
- **Edge / failure coverage:** null `description`, null `closedAt`, empty `labels`, empty `assignees`, null `milestone`, milestone with null `dueDate`, `issueId = 0`, negative `issueId`, integration exception from adapter.
- **INFO logging (IssuesController):** `"Get issue detail request received issueId={}"` on entry; `"Get issue detail response returned issueId={}"` before return.
- **INFO logging (IssuesService):** `"Fetching issue detail issueId={}"` on entry; `"Issue detail fetched issueId={}"` on result.
- **INFO logging (GitLabIssuesAdapter):** `"Fetching GitLab issue detail issueId={}"` on entry; `"GitLab issue detail fetched issueId={}"` on success.
- **DEBUG logging (GitLabIssueDetailMapper):** `"Mapped GitLab single issue id={} issueId={} state={} assigneeCount={} milestonePresent={}"`.
- **WARN logging:** None.
- **ERROR logging:** None — errors propagate via `executeGitLabOperation` and `GlobalExceptionHandler`.
- **Documentation updates:**
  - `flow-orchestrator/http/issues.http` — add `GET {{baseUrl}}/api/issues/{{issueId}}` example.
  - `documentation/context-map.md` — add `GET /api/issues/{issueId}` to endpoints list; add `IssueDetail`, `IssueDetailDto`, `GitLabSingleIssueResponse`, `GitLabIssueDetailMapper` to model/mapper tables.

## Testing Matrix

| Level | Required | Planned Coverage | Evidence Target |
|---|---|---|---|
| Unit | Yes | `GitLabIssueDetailMapperTest` (new); additions to `IssuesServiceTest`, `IssuesResponseMapperTest`, `GitLabIssuesAdapterTest` | `scripts/verify-quick.sh` PASS; coverage ≥ 85% |
| Integration (WebMvcTest) | Yes | Additions to `IssuesControllerIT` | `scripts/verify-quick.sh` PASS |
| Component | Yes | Additions to `IssuesApiComponentTest` + `GitLabSingleIssueStubSupport` | `scripts/verify-quick.sh` PASS |
| Karate smoke | Yes | `issues-get-single.feature` | `scripts/karate-test.sh` PASS (requires running app) |

## Acceptance Criteria Traceability

| AC | Implementation Evidence |
|---|---|
| AC1: endpoint returns 200 | `IssuesController.getIssueDetail` + `IssuesControllerIT` |
| AC2: correct field mapping | `GitLabIssueDetailMapper` + `IssuesResponseMapper.toIssueDetailDto` + mapper unit tests |
| AC3: changeSets present empty | `IssueDetail` / `IssueDetailDto` compact constructors enforce `List.of()` |
| AC4: null/default handling | Mapper null-checks + compact constructor defaults + unit tests |
| AC5: assignees array with id/username/name | `AssigneeDetail` / `AssigneeDto` records with full fields |
| AC6: milestone object with id/milestoneId/title/state/dueDate | `MilestoneDetail` / `MilestoneDto` records |
| AC7: no label events call | `GitLabIssuesAdapter.getIssueDetail` fetches only `/issues/{issueId}` |
| AC8: error response on failure | `executeGitLabOperation` + `GlobalExceptionHandler` + `IssuesControllerIT` |
| AC9: dedicated types | `IssueDetail`, `IssueDetailDto`, `GitLabSingleIssueResponse` are all new distinct types |
| AC10: integration boundary respected | Adapter fetches only; `GitLabIssueDetailMapper` maps; `IssuesResponseMapper` assembles |

## Final Verification

- `scripts/verify-quick.sh` — compile + all unit, integration, component tests; must PASS.
- `scripts/final-check.sh` — format + full quality gate; must PASS before Reviewer Phase 2.
- `scripts/karate-test.sh` — smoke tests against running app; `issues-get-single.feature @smoke` scenario must PASS.
- Report paths: `target/checkstyle-result.xml`, `target/pmd.xml`, `target/cpd.xml`, `target/spotbugsXml.xml`, `target/site/jacoco/jacoco.xml`.
