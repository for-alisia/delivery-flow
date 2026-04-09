package com.gitlabflow.floworchestrator.orchestration.issues.model;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record IssueDetail(
        long issueId,
        String title,
        @Nullable String description,
        String state,
        List<String> labels,
        List<AssigneeDetail> assignees,
        @Nullable MilestoneDetail milestone,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        @Nullable OffsetDateTime closedAt) {

    public IssueDetail {
        labels = labels == null ? List.of() : List.copyOf(labels);
        assignees = assignees == null ? List.of() : List.copyOf(assignees);
    }

    @Builder
    public record AssigneeDetail(long id, String username, String name) {}

    @Builder
    public record MilestoneDetail(
            long id,
            long milestoneId,
            String title,
            String state,
            @Nullable String dueDate) {}
}
