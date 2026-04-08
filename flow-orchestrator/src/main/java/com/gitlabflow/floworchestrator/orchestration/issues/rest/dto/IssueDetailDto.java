package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record IssueDetailDto(
        long issueId,
        String title,
        @Nullable String description,
        String state,
        List<String> labels,
        List<AssigneeDto> assignees,
        @Nullable MilestoneDto milestone,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        @Nullable OffsetDateTime closedAt,
        List<Object> changeSets) {

    public IssueDetailDto {
        labels = labels == null ? List.of() : List.copyOf(labels);
        assignees = assignees == null ? List.of() : List.copyOf(assignees);
        changeSets = changeSets == null ? List.of() : List.copyOf(changeSets);
    }

    @Builder
    public record AssigneeDto(long id, String username, String name) {}

    @Builder
    public record MilestoneDto(
            long id,
            long milestoneId,
            String title,
            String state,
            @Nullable String dueDate) {}
}
