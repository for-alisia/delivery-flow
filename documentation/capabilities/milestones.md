# Capability: milestones

Create and search GitLab project milestones through a provider-agnostic orchestration layer.

Endpoints:
- POST /api/milestones — create a milestone; returns HTTP 201 with milestoneId (GitLab iid) and title
- POST /api/milestones/search — search milestones by state, title, and IID filters

All paths are relative to flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/ unless marked otherwise.

---

## Orchestration

| File | Role |
|------|------|
| orchestration/milestones/MilestonesService.java | Service that coordinates milestone search and create flows and logs request/response summary |
| orchestration/milestones/MilestonesPort.java | Provider-agnostic port implemented by integration adapters; defines searchMilestones and createMilestone |

## Orchestration Models (orchestration/milestones/model/)

| File | Role |
|------|------|
| Milestone.java | Shared milestone model used by issue detail, milestone search, and milestone create responses |
| MilestoneState.java | Enum: active, closed, all |
| SearchMilestonesInput.java | Provider-agnostic milestone search input |
| CreateMilestoneInput.java | Provider-agnostic milestone create input (title required; description, startDate, dueDate optional) |

## REST Layer

| File | Role |
|------|------|
| orchestration/milestones/rest/MilestonesController.java | REST controller for POST /api/milestones (create) and POST /api/milestones/search |
| orchestration/milestones/rest/MilestonesRequestValidator.java | Boundary validation: create request (title, date format, date ordering) and search milestoneIds list rules |
| orchestration/milestones/rest/mapper/MilestonesRequestMapper.java | Request DTO to orchestration-model mapping with default ACTIVE behavior |
| orchestration/milestones/rest/mapper/MilestonesResponseMapper.java | Orchestration-model to response DTO mapping |

## REST DTOs (orchestration/milestones/rest/dto/)

| File | Role |
|------|------|
| CreateMilestoneRequest.java | Create milestone request body (title required; description, dueDate, startDate optional) |
| CreateMilestoneResponse.java | Create milestone response (milestoneId, title only — milestoneId is GitLab iid) |
| MilestoneDto.java | Shared milestone DTO reused by issue detail and milestone search responses |
| SearchMilestonesRequest.java | Request body wrapper for milestone search |
| MilestoneFiltersRequest.java | Optional milestone search filter fields |
| SearchMilestonesResponse.java | Milestone search response wrapper (milestones) |

## Integration - GitLab

All paths are relative to flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/.

| File | Role |
|------|------|
| integration/gitlab/milestones/GitLabMilestonesAdapter.java | MilestonesPort adapter; builds GitLab milestones URI, delegates page traversal to GitLabOffsetPaginationLoader, POSTs create milestone, maps DTOs to shared Milestone model |
| integration/gitlab/milestones/mapper/GitLabMilestonesMapper.java | Maps CreateMilestoneInput to GitLabCreateMilestoneRequest and GitLabMilestoneResponse to shared Milestone model |
| integration/gitlab/milestones/dto/GitLabCreateMilestoneRequest.java | GitLab create milestone request DTO (title, description, start_date, due_date); null optional fields omitted via @JsonInclude |
| integration/gitlab/milestones/dto/GitLabMilestoneResponse.java | GitLab project milestones response DTO (id, iid, title, description, start_date, due_date, state) |

## Shared Milestone Reuse

- Issue detail mapping uses shared Milestone model:
  - integration/gitlab/issues/mapper/GitLabIssueDetailMapper.java
  - orchestration/issues/model/IssueDetail.java
- Issue detail response mapping uses shared MilestoneDto:
  - orchestration/issues/rest/mapper/IssuesResponseMapper.java
  - orchestration/issues/rest/dto/IssueDetailDto.java

## Tests

Paths are relative to flow-orchestrator/src/test/.

- java/.../orchestration/milestones/model/MilestoneTest.java
- java/.../orchestration/milestones/model/MilestoneStateTest.java
- java/.../orchestration/milestones/model/SearchMilestonesInputTest.java
- java/.../orchestration/milestones/rest/dto/MilestoneDtoTest.java
- java/.../orchestration/milestones/rest/dto/MilestoneFiltersRequestTest.java
- java/.../orchestration/milestones/rest/dto/SearchMilestonesResponseTest.java
- java/.../orchestration/milestones/model/CreateMilestoneInputTest.java
- java/.../orchestration/milestones/rest/dto/CreateMilestoneRequestTest.java
- java/.../orchestration/milestones/rest/dto/CreateMilestoneResponseTest.java
- java/.../orchestration/milestones/rest/MilestonesRequestValidatorTest.java
- java/.../orchestration/milestones/rest/mapper/MilestonesRequestMapperTest.java
- java/.../orchestration/milestones/rest/mapper/MilestonesResponseMapperTest.java
- java/.../orchestration/milestones/MilestonesServiceTest.java
- java/.../integration/gitlab/milestones/GitLabMilestonesAdapterTest.java
- java/.../integration/gitlab/milestones/mapper/GitLabMilestonesMapperTest.java
- integration/java/.../orchestration/milestones/rest/MilestonesControllerIT.java
- component/java/.../milestones/MilestonesApiComponentTest.java
- component/java/.../milestones/support/GitLabMilestonesStubSupport.java
- karate/resources/milestones/milestones-create.feature
- karate/resources/milestones/milestones-search.feature
- java/.../integration/gitlab/issues/mapper/GitLabIssueDetailMapperTest.java
- java/.../orchestration/issues/rest/mapper/IssuesResponseMapperTest.java
- integration/java/.../orchestration/issues/rest/IssuesControllerIT.java
