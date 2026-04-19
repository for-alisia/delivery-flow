package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import com.gitlabflow.floworchestrator.orchestration.common.rest.dto.ChangeSetDto;
import com.gitlabflow.floworchestrator.orchestration.common.rest.dto.UserDto;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.MilestoneDto;
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
        List<UserDto> assignees,
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
}
