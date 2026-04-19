# Capability: milestones

Model and DTO foundation extracted for cross-capability reuse.

No milestone endpoints are exposed yet.

All paths are relative to flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/ unless marked otherwise.

---

## Orchestration Models (orchestration/milestones/model/)

| File | Role |
|------|------|
| Milestone.java | Shared milestone model reused by issues get-single detail and future milestone APIs |

## REST DTOs (orchestration/milestones/rest/dto/)

| File | Role |
|------|------|
| MilestoneDto.java | Shared milestone DTO reused by IssueDetailDto and future milestone REST responses |

## Current Usage

- Issue detail mapping uses Milestone in orchestration model:
  - integration/gitlab/issues/mapper/GitLabIssueDetailMapper.java
  - orchestration/issues/model/IssueDetail.java
- Issue detail response mapping uses MilestoneDto:
  - orchestration/issues/rest/mapper/IssuesResponseMapper.java
  - orchestration/issues/rest/dto/IssueDetailDto.java
