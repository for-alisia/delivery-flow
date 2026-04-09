package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import com.gitlabflow.floworchestrator.orchestration.issues.model.ChangeField;
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
        List<ChangeSetDto> changeSets) {

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

    public sealed interface ChangeSetDto permits LabelChangeSetDto {

        String changeType();

        ChangedByDto changedBy();

        ChangeDto change();

        OffsetDateTime changedAt();
    }

    @Builder
    public record LabelChangeSetDto(
            String changeType, ChangedByDto changedBy, LabelChangeDto change, OffsetDateTime changedAt)
            implements ChangeSetDto {}

    public sealed interface ChangeDto permits LabelChangeDto {

        ChangeField field();

        long id();

        String name();
    }

    @Builder
    public record ChangedByDto(long id, String username, String name) {}

    @Builder
    public record LabelChangeDto(ChangeField field, long id, String name) implements ChangeDto {}
}
