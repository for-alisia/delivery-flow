package com.gitlabflow.floworchestrator.integration.gitlab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

public record GitLabIssueResponse(
        @JsonProperty("iid") long issueNumber,
        String title,
        String state,
        List<String> labels,
        List<GitLabUserResponse> assignees,
        @JsonProperty("web_url") String webUrl,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt
) {
}
