# Capability: milestones

Search GitLab project milestones through a provider-agnostic orchestration layer.

Endpoint: POST /api/milestones/search

All paths are relative to flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/ unless marked otherwise.

---

## Orchestration

| File | Role |
|------|------|
| orchestration/milestones/MilestonesService.java | Service that coordinates milestone search flow and logs request/response summary |
| orchestration/milestones/MilestonesPort.java | Provider-agnostic port implemented by integration adapters |

## Orchestration Models (orchestration/milestones/model/)

| File | Role |
|------|------|
| Milestone.java | Shared milestone model used by issue detail and milestone search responses |
| MilestoneState.java | Enum: active, closed, all |
| SearchMilestonesInput.java | Provider-agnostic milestone search input |

## REST Layer

| File | Role |
|------|------|
| orchestration/milestones/rest/MilestonesController.java | REST controller for POST /api/milestones/search |
| orchestration/milestones/rest/MilestonesRequestValidator.java | Boundary validation for milestoneIds list rules |
| orchestration/milestones/rest/mapper/MilestonesRequestMapper.java | Request DTO to orchestration-model mapping with default ACTIVE behavior |
| orchestration/milestones/rest/mapper/MilestonesResponseMapper.java | Orchestration-model to response DTO mapping |

## REST DTOs (orchestration/milestones/rest/dto/)

| File | Role |
|------|------|
| MilestoneDto.java | Shared milestone DTO reused by issue detail and milestone search responses |
| SearchMilestonesRequest.java | Request body wrapper for milestone search |
| MilestoneFiltersRequest.java | Optional milestone search filter fields |
| SearchMilestonesResponse.java | Milestone search response wrapper (milestones) |

## Integration - GitLab

All paths are relative to flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/.

| File | Role |
|------|------|
| integration/gitlab/milestones/GitLabMilestonesAdapter.java | MilestonesPort adapter; builds GitLab milestones URI, delegates page traversal to GitLabOffsetPaginationLoader, maps DTOs to shared Milestone model |
| integration/gitlab/milestones/mapper/GitLabMilestonesMapper.java | Maps GitLabMilestoneResponse to shared Milestone model |
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
- java/.../orchestration/milestones/rest/MilestonesRequestValidatorTest.java
- java/.../orchestration/milestones/rest/mapper/MilestonesRequestMapperTest.java
- java/.../orchestration/milestones/rest/mapper/MilestonesResponseMapperTest.java
- java/.../orchestration/milestones/MilestonesServiceTest.java
- java/.../integration/gitlab/milestones/GitLabMilestonesAdapterTest.java
- java/.../integration/gitlab/milestones/mapper/GitLabMilestonesMapperTest.java
- integration/java/.../orchestration/milestones/rest/MilestonesControllerIT.java
- component/java/.../milestones/MilestonesApiComponentTest.java
- component/java/.../milestones/support/GitLabMilestonesStubSupport.java
- karate/resources/milestones/milestones-search.feature
- java/.../integration/gitlab/issues/mapper/GitLabIssueDetailMapperTest.java
- java/.../orchestration/issues/rest/mapper/IssuesResponseMapperTest.java
- integration/java/.../orchestration/issues/rest/IssuesControllerIT.java
