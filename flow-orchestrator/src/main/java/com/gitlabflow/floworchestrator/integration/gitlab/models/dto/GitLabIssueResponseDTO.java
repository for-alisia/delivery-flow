package com.gitlabflow.floworchestrator.integration.gitlab.models.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitLabIssueResponseDTO(
        @JsonProperty("id") Long id,
        @JsonProperty("iid") Long iid,
        @JsonProperty("project_id") Long projectId,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("state") String state,
        @JsonProperty("labels") List<String> labels,
        @JsonProperty("author") GitLabUserDTO author,
        @JsonProperty("assignees") List<GitLabUserDTO> assignees,
        @JsonProperty("web_url") String webUrl,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt,
        @JsonProperty("closed_at") OffsetDateTime closedAt,
        @JsonProperty("milestone") GitLabMilestoneDTO milestone
) {
}
