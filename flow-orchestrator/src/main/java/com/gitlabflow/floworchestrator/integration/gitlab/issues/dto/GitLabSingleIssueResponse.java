package com.gitlabflow.floworchestrator.integration.gitlab.issues.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import org.springframework.lang.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record GitLabSingleIssueResponse(
        long id,
        long iid,
        String title,
        @Nullable String description,
        String state,
        List<String> labels,
        List<GitLabAssigneeDetail> assignees,
        @Nullable GitLabMilestoneDetail milestone,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt,
        @JsonProperty("closed_at") @Nullable OffsetDateTime closedAt) {

    public GitLabSingleIssueResponse {
        labels = labels == null ? List.of() : List.copyOf(labels);
        assignees = assignees == null ? List.of() : List.copyOf(assignees);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Builder
    public record GitLabAssigneeDetail(long id, String username, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Builder
    public record GitLabMilestoneDetail(
            long id,
            long iid,
            String title,
            String state,
            @JsonProperty("due_date") @Nullable String dueDate) {}
}
