package com.gitlabflow.floworchestrator.orchestration.issues.model;

import com.gitlabflow.floworchestrator.orchestration.common.model.User;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.Milestone;
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
        List<User> assignees,
        @Nullable Milestone milestone,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        @Nullable OffsetDateTime closedAt) {

    public IssueDetail {
        labels = labels == null ? List.of() : List.copyOf(labels);
        assignees = assignees == null ? List.of() : List.copyOf(assignees);
    }
}
