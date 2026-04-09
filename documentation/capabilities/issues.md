# Capability: issues

Search, create, delete, and get-single GitLab issues through a provider-agnostic orchestration layer.

**Endpoints:** `POST /api/issues/search`, `POST /api/issues`, `DELETE /api/issues/{issueId}`, `GET /api/issues/{issueId}`

All paths relative to `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/` unless marked otherwise.

---

## Orchestration

| File | Role |
|------|------|
| `orchestration/issues/IssuesService.java` | Service — coordinates port calls, returns `EnrichedIssueDetail` for get-single |
| `orchestration/issues/IssuesPort.java` | Port interface — implemented by integration adapter |

## Orchestration Models (`orchestration/issues/model/`)

| File | Role |
|------|------|
| `Issue.java` | Unified output model for search and create |
| `IssuePage.java` | Paginated search result |
| `IssueQuery.java` | Search query parameters |
| `IssueState.java` | Enum: `opened`, `closed`, `all` |
| `CreateIssueInput.java` | Input for issue creation |
| `IssueDetail.java` | Single-issue detail — pure integration-mapped model (10 fields), maps 1:1 from GitLab |
| `ChangeSet.java` | Change-set contract for issue history entries |
| `Change.java` | Change payload contract for issue history entries |
| `ChangeField.java` | Enum discriminator for change payloads (currently `LABEL`) |
| `ChangedBy.java` | Actor metadata for issue history changes |
| `LabelChange.java` | Label-specific change payload implementing `Change` (`field` is `ChangeField.LABEL`) |
| `LabelChangeSet.java` | Label-event change set implementing `ChangeSet` |
| `EnrichedIssueDetail.java` | Orchestration composition model — combines `IssueDetail` + `changeSets` (mapped from GitLab label events) |

Inner records: `IssueDetail.AssigneeDetail`, `IssueDetail.MilestoneDetail`

## REST Layer

| File | Role |
|------|------|
| `orchestration/issues/rest/IssuesController.java` | REST controller for all issue endpoints |
| `orchestration/issues/rest/mapper/IssuesRequestMapper.java` | Maps request DTOs → orchestration models |
| `orchestration/issues/rest/mapper/IssuesResponseMapper.java` | Maps orchestration models → response DTOs |

## REST DTOs (`orchestration/issues/rest/dto/`)

| File | Role |
|------|------|
| `SearchIssuesRequest.java` | Request body for search |
| `SearchIssuesResponse.java` | Paginated search response |
| `CreateIssueRequest.java` | Request body for create |
| `IssueDto.java` | Response for search, create, delete |
| `IssueDetailDto.java` | Response for get-single including `changeSets` mapped from GitLab label-event history |
| `IssueFiltersRequest.java` | Filter fields inside search request |
| `PaginationRequest.java` | Pagination fields inside search request |

Inner records: `IssueDetailDto.AssigneeDto`, `IssueDetailDto.MilestoneDto`, `IssueDetailDto.ChangeSetDto`, `IssueDetailDto.ChangeDto`, `IssueDetailDto.LabelChangeSetDto`, `IssueDetailDto.ChangedByDto`, `IssueDetailDto.LabelChangeDto`

## Integration — GitLab

| File | Role |
|------|------|
| `integration/gitlab/issues/GitLabIssuesAdapter.java` | Adapter implementing `IssuesPort` via `RestClient` (search/create/delete/get-single + label events) |
| `integration/gitlab/issues/dto/GitLabIssueResponse.java` | GitLab search/create response DTO |
| `integration/gitlab/issues/dto/GitLabCreateIssueRequest.java` | GitLab create request DTO |
| `integration/gitlab/issues/dto/GitLabSingleIssueResponse.java` | GitLab get-single response DTO |
| `integration/gitlab/issues/dto/GitLabLabelEventResponse.java` | GitLab `resource_label_events` response DTO |
| `integration/gitlab/issues/mapper/GitLabIssuesMapper.java` | Maps GitLab search/create responses → `Issue` |
| `integration/gitlab/issues/mapper/GitLabIssueDetailMapper.java` | Maps `GitLabSingleIssueResponse` → `IssueDetail` |
| `integration/gitlab/issues/mapper/GitLabLabelEventMapper.java` | Maps GitLab label events → `List<ChangeSet>` |

## Config

| File | Role |
|------|------|
| `config/IssuesApiProperties.java` | `app.issues-api.*` — `default-page-size`, `max-page-size` |

## Tests

Paths relative to `flow-orchestrator/src/test/`.

| File | Level |
|------|-------|
| `java/.../orchestration/issues/IssuesServiceTest.java` | Unit |
| `java/.../orchestration/issues/model/IssuePageTest.java` | Unit |
| `java/.../orchestration/issues/rest/dto/CreateIssueRequestTest.java` | Unit |
| `java/.../orchestration/issues/rest/dto/IssueFiltersRequestTest.java` | Unit |
| `java/.../orchestration/issues/rest/dto/SearchIssuesResponseTest.java` | Unit |
| `java/.../orchestration/issues/rest/mapper/IssuesRequestMapperTest.java` | Unit |
| `java/.../orchestration/issues/rest/mapper/IssuesResponseMapperTest.java` | Unit |
| `java/.../orchestration/issues/model/LabelChangeSetTest.java` | Unit |
| `java/.../integration/gitlab/issues/GitLabIssuesAdapterTest.java` | Unit |
| `java/.../integration/gitlab/issues/mapper/GitLabIssuesMapperTest.java` | Unit |
| `java/.../integration/gitlab/issues/mapper/GitLabIssueDetailMapperTest.java` | Unit |
| `java/.../integration/gitlab/issues/mapper/GitLabLabelEventMapperTest.java` | Unit |
| `integration/java/.../orchestration/issues/rest/IssuesControllerIT.java` | Integration |
| `component/java/.../issues/IssuesApiComponentTest.java` | Component |
| `component/java/.../issues/support/GitLabIssuesStubSupport.java` | Component support |
| `component/java/.../issues/support/GitLabCreateIssueStubSupport.java` | Component support |
| `component/java/.../issues/support/GitLabSingleIssueStubSupport.java` | Component support |
| `component/java/.../issues/support/GitLabLabelEventsStubSupport.java` | Component support |

## Karate Smoke Tests (`src/test/karate/`)

| File | Coverage |
|------|----------|
| `java/.../karate/issues/IssuesKarateTest.java` | Runner (activates `-Pkarate`) |
| `resources/karate-config.js` | Env-aware `baseUrl` config |
| `resources/issues/health-check.feature` | Health endpoint |
| `resources/issues/issues-search.feature` | Search endpoint |
| `resources/issues/issues-create.feature` | Create endpoint |
| `resources/issues/issues-delete.feature` | Delete endpoint |
| `resources/issues/issues-get-single.feature` | Get-single endpoint |
| `resources/issues/issues-lifecycle.feature` | Create → search → delete lifecycle |

## Design Notes

- `Issue` is the unified output model for both search and create
- `IssueDto` fields: `id` (GitLab global id), `issueId` (project-scoped `iid`), `title`, `description`, `state`, `labels`, `assignee`, `milestone`, `parent`
- `IssueDetailDto` fields: `issueId`, `title`, `description`, `state`, `labels` (`[]`), `assignees` (`[]`), `milestone` (nullable), `createdAt`, `updatedAt`, `closedAt` (nullable), `changeSets` (typed label-event change sets when present)
- `EnrichedIssueDetail` composes `IssueDetail` + `changeSets`. `IssuesService.getIssueDetail()` returns `EnrichedIssueDetail`; `IssuesResponseMapper` maps it to `IssueDetailDto`
- `IssueDetail` maps 1:1 from `GitLabSingleIssueResponse` — no cross-source composition
- Pagination is GitLab header-based, mapped in adapter
