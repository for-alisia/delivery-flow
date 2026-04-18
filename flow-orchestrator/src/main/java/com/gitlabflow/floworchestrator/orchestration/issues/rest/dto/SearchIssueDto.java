package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record SearchIssueDto(
        long id,
        long issueId,
        String title,
        @Nullable String description,
        String state,
        List<String> labels,
        @Nullable String assignee,
        @Nullable String milestone,
        @Nullable Long parent,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Nullable List<ChangeSetDto> changeSets) {

    public SearchIssueDto {
        labels = labels == null ? List.of() : List.copyOf(labels);
        changeSets = changeSets == null ? null : List.copyOf(changeSets);
    }

    @Builder
    public record ChangeSetDto(
            String changeType, ChangedByDto changedBy, LabelChangeDto change, OffsetDateTime changedAt) {}

    @Builder
    public record ChangedByDto(long id, String username, String name) {}

    @Builder
    public record LabelChangeDto(String field, long id, String name) {}
}
