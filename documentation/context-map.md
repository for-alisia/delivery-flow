# Context Map

Quick-reference map of the `flow-orchestrator` codebase.
Agents and humans use this to locate code by capability without scanning the full tree.

All paths are relative to `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/` unless marked otherwise.

**Maintenance rule:** Java Coder updates this file during implementation when packages, classes, endpoints, models, or configuration change. Reviewer Phase 2 validates freshness. Team Lead verifies at sign-off.

---

## Application Entry Point

- `FlowOrchestratorApplication.java`

## Configuration Files

- `src/main/resources/application.yml` — default profile (server port, GitLab connection, issues-api defaults)
- `src/main/resources/application-local.yml` — local profile overrides

---

## Capabilities

### issues

Search, create, delete, and get-single GitLab issues through a provider-agnostic orchestration layer.

**Endpoints:** `POST /api/issues/search`, `POST /api/issues`, `DELETE /api/issues/{issueId}`, `GET /api/issues/{issueId}`

#### Orchestration

| File | Role |
|------|------|
| `orchestration/issues/IssuesService.java` | Service — coordinates port calls, returns `EnrichedIssueDetail` for get-single |
| `orchestration/issues/IssuesPort.java` | Port interface — implemented by integration adapter |

#### Orchestration Models (`orchestration/issues/model/`)

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

#### REST Layer

| File | Role |
|------|------|
| `orchestration/issues/rest/IssuesController.java` | REST controller for all issue endpoints |
| `orchestration/issues/rest/mapper/IssuesRequestMapper.java` | Maps request DTOs → orchestration models |
| `orchestration/issues/rest/mapper/IssuesResponseMapper.java` | Maps orchestration models → response DTOs |

#### REST DTOs (`orchestration/issues/rest/dto/`)

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

#### Integration — GitLab

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

#### Config

| File | Role |
|------|------|
| `config/IssuesApiProperties.java` | `app.issues-api.*` — `default-page-size`, `max-page-size` |

#### Tests

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

#### Karate Smoke Tests (`src/test/karate/`)

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

#### Design Notes

- `Issue` is the unified output model for both search and create
- `IssueDto` fields: `id` (GitLab global id), `issueId` (project-scoped `iid`), `title`, `description`, `state`, `labels`, `assignee`, `milestone`, `parent`
- `IssueDetailDto` fields: `issueId`, `title`, `description`, `state`, `labels` (`[]`), `assignees` (`[]`), `milestone` (nullable), `createdAt`, `updatedAt`, `closedAt` (nullable), `changeSets` (typed label-event change sets when present)
- `EnrichedIssueDetail` composes `IssueDetail` + `changeSets`. `IssuesService.getIssueDetail()` returns `EnrichedIssueDetail`; `IssuesResponseMapper` maps it to `IssueDetailDto`
- `IssueDetail` maps 1:1 from `GitLabSingleIssueResponse` — no cross-source composition
- Pagination is GitLab header-based, mapped in adapter

---

## Shared Infrastructure

### common

Cross-cutting error handling and web concerns. Shared by all capabilities.

| File | Role |
|------|------|
| `common/error/ErrorCode.java` | Error code enum |
| `common/error/IntegrationException.java` | Integration failure exception |
| `common/error/ValidationException.java` | Validation failure exception |
| `common/web/GlobalExceptionHandler.java` | REST exception handler |
| `common/web/ErrorResponse.java` | Error response DTO |

Test: `java/.../common/web/GlobalExceptionHandlerTest.java`

### config

Application and provider configuration. Shared by all capabilities.

| File | Role |
|------|------|
| `config/GitLabProperties.java` | `app.gitlab.*` — `url`, `token` |

### integration/gitlab (shared)

Reusable GitLab client infrastructure for all capability adapters.

| File | Role |
|------|------|
| `integration/gitlab/GitLabRestClientConfig.java` | `RestClient` bean with base URL and auth header |
| `integration/gitlab/GitLabProjectLocator.java` | Resolves GitLab project path to encoded project ID |
| `integration/gitlab/GitLabExceptionMapper.java` | Maps HTTP errors to `IntegrationException` |
| `integration/gitlab/GitLabUriFactory.java` | Builds GitLab API URIs with project path and optional query params |

Tests:
- `java/.../integration/gitlab/GitLabExceptionMapperTest.java`
- `java/.../integration/gitlab/GitLabProjectLocatorTest.java`
- `java/.../integration/gitlab/GitLabUriFactoryTest.java`

### scripts

Verification and quality tooling (paths relative to repo root).

| Script | Purpose |
|--------|---------|
| `scripts/verify-quick.sh` | Compile + tests (fast gate) |
| `scripts/final-check.sh` | Formatting + full quality gate |
| `scripts/quality-check.sh` | Maven clean verify |
| `scripts/format-code.sh` | Spotless apply/check |
| `scripts/karate-test.sh` | Karate API smoke tests (`-Pkarate`); reuses a healthy local app or starts one automatically |
| `scripts/smoke-test.sh` | Legacy curl-based verification (deprecated) |

### test infrastructure

| File | Role |
|------|------|
| `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` | Enables inline mock maker |

---

## Key Patterns

When adding a new capability (e.g., `merge-requests`, `pipelines`), follow the `issues` structure:

1. `orchestration/<capability>/` — `<Capability>Service`, `<Capability>Port`
2. `orchestration/<capability>/model/` — orchestration models
3. `orchestration/<capability>/rest/` — controller
4. `orchestration/<capability>/rest/dto/` — request/response records
5. `orchestration/<capability>/rest/mapper/` — API mappers
6. `integration/gitlab/<capability>/` — adapter implementing the port
7. `integration/gitlab/<capability>/dto/` — GitLab-specific records
8. `integration/gitlab/<capability>/mapper/` — GitLab-to-orchestration mapper
9. `config/<Capability>ApiProperties` — capability-specific configuration

Shared GitLab infrastructure (`GitLabRestClientConfig`, `GitLabProjectLocator`, `GitLabExceptionMapper`, `GitLabUriFactory`) lives at `integration/gitlab/` and is reused by all capability adapters.
