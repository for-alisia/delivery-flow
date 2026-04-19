# Capability: issues

Search, create, delete, and get-single GitLab issues through a provider-agnostic orchestration layer.

Endpoints: POST /api/issues/search, POST /api/issues, DELETE /api/issues/{issueId}, GET /api/issues/{issueId}

All paths are relative to flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/ unless marked otherwise.

---

## Orchestration

| File | Role |
|------|------|
| orchestration/issues/IssuesService.java | Service that coordinates issue search/create/delete and parallel issue-detail composition |
| orchestration/issues/IssuesPort.java | Provider-agnostic port implemented by integration adapters |

## Orchestration Models (orchestration/issues/model/)

| File | Role |
|------|------|
| IssueSummary.java | Lightweight issue projection for search/create; includes nullable changeSets for enriched search |
| IssueDetail.java | Full issue detail model for get-single; uses shared User and Milestone models |
| EnrichedIssueDetail.java | Composition model combining IssueDetail with changeSets |
| LabelChange.java | Label-specific change payload implementing shared Change contract |
| LabelChangeSet.java | Label-event change set implementing shared ChangeSet<LabelChange> |
| IssuePage.java | Paginated search result model |
| IssueQuery.java | Search query parameters and optional audit enrichment flags |
| IssueState.java | Enum: opened, closed, all |
| IssueAuditType.java | Enum for enrichment types (currently LABEL) |
| CreateIssueInput.java | Input model for issue creation |

Shared model dependencies used by issues:
- orchestration/common/model/User.java
- orchestration/common/model/Change.java
- orchestration/common/model/ChangeSet.java
- orchestration/common/model/ChangeField.java
- orchestration/milestones/model/Milestone.java

## REST Layer

| File | Role |
|------|------|
| orchestration/issues/rest/IssuesController.java | REST controller for issue endpoints |
| orchestration/issues/rest/mapper/IssuesRequestMapper.java | Request DTO to orchestration-model mapping |
| orchestration/issues/rest/mapper/IssuesResponseMapper.java | Orchestration-model to response DTO mapping |

## REST DTOs (orchestration/issues/rest/dto/)

| File | Role |
|------|------|
| SearchIssuesRequest.java | Request body for search |
| SearchIssuesResponse.java | Search response wrapper (items, count, page) |
| IssueSummaryDto.java | Unified item DTO for search and create responses |
| CreateIssueRequest.java | Request body for create |
| IssueDetailDto.java | Get-single response DTO |
| IssueFiltersRequest.java | Search filter fields |
| PaginationRequest.java | Search pagination fields |

Shared DTO dependencies used by issues:
- orchestration/common/rest/dto/UserDto.java
- orchestration/common/rest/dto/ChangeDto.java
- orchestration/common/rest/dto/ChangeSetDto.java
- orchestration/common/rest/dto/LabelChangeDto.java
- orchestration/common/rest/dto/LabelChangeSetDto.java
- orchestration/milestones/rest/dto/MilestoneDto.java

## Integration - GitLab

| File | Role |
|------|------|
| integration/gitlab/issues/GitLabIssuesAdapter.java | IssuesPort adapter using RestClient for search/create/delete/get-single/label-events |
| integration/gitlab/issues/mapper/GitLabIssuesMapper.java | Maps GitLab list/create responses to IssueSummary |
| integration/gitlab/issues/mapper/GitLabIssueDetailMapper.java | Maps GitLab single issue response to IssueDetail |
| integration/gitlab/issues/mapper/GitLabLabelEventMapper.java | Maps GitLab label events to List<ChangeSet<?>> |
| integration/gitlab/issues/dto/GitLabIssueResponse.java | GitLab list/create response DTO |
| integration/gitlab/issues/dto/GitLabCreateIssueRequest.java | GitLab create request DTO |
| integration/gitlab/issues/dto/GitLabSingleIssueResponse.java | GitLab get-single response DTO |
| integration/gitlab/issues/dto/GitLabLabelEventResponse.java | GitLab label-events response DTO |

## Tests

Paths are relative to flow-orchestrator/src/test/.

- java/.../orchestration/issues/IssuesServiceTest.java
- java/.../orchestration/issues/model/IssuePageTest.java
- java/.../orchestration/issues/model/IssueDetailTest.java
- java/.../orchestration/issues/model/LabelChangeSetTest.java
- java/.../orchestration/issues/rest/dto/CreateIssueRequestTest.java
- java/.../orchestration/issues/rest/dto/IssueFiltersRequestTest.java
- java/.../orchestration/issues/rest/dto/IssueDetailDtoTest.java
- java/.../orchestration/issues/rest/dto/SearchIssuesResponseTest.java
- java/.../orchestration/issues/rest/mapper/IssuesRequestMapperTest.java
- java/.../orchestration/issues/rest/mapper/IssuesResponseMapperTest.java
- java/.../integration/gitlab/issues/GitLabIssuesAdapterTest.java
- java/.../integration/gitlab/issues/mapper/GitLabIssuesMapperTest.java
- java/.../integration/gitlab/issues/mapper/GitLabIssueDetailMapperTest.java
- java/.../integration/gitlab/issues/mapper/GitLabLabelEventMapperTest.java
- integration/java/.../orchestration/issues/rest/IssuesControllerIT.java
- component/java/.../issues/IssuesApiComponentTest.java
- karate/resources/issues/*.feature

## Design Notes

- IssueSummary is the orchestration output for both search and create.
- IssueSummaryDto is the shared transport DTO for search items and create responses.
- IssueDetail remains a separate full-detail model and is mapped to IssueDetailDto.
- Search and get-single change-set payloads both serialize change.field as lowercase label.
- IssuesService performs parallel composition for get-single and audit enrichment using AsyncComposer.
