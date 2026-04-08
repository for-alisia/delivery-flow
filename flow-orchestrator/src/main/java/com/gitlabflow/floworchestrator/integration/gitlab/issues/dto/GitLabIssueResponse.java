package com.gitlabflow.floworchestrator.integration.gitlab.issues.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record GitLabIssueResponse(
        long id,
        long iid,
        String title,
        String description,
        String state,
        List<String> labels,
        List<GitLabAssignee> assignees,
        GitLabAssignee assignee,
        GitLabMilestone milestone,
        GitLabEpic epic) {
    public GitLabIssueResponse {
        labels = labels == null ? List.of() : List.copyOf(labels);
        assignees = assignees == null ? List.of() : List.copyOf(assignees);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Builder
    public record GitLabAssignee(String username) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Builder
    public record GitLabMilestone(String title) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Builder
    public record GitLabEpic(Long id) {}
}
