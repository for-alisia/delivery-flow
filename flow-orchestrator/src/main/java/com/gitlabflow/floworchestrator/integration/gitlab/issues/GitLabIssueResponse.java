package com.gitlabflow.floworchestrator.integration.gitlab.issues;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitLabIssueResponse(
        long id,
        String title,
        String description,
        String state,
        List<String> labels,
        List<GitLabAssignee> assignees,
        GitLabAssignee assignee,
        GitLabMilestone milestone,
        GitLabEpic epic
) {
    public GitLabIssueResponse {
        labels = labels == null ? List.of() : List.copyOf(labels);
        assignees = assignees == null ? List.of() : List.copyOf(assignees);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitLabAssignee(String username) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitLabMilestone(String title) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitLabEpic(Long id) {
    }
}