package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gitlabflow.floworchestrator.orchestration.common.rest.dto.ChangeSetDto;
import java.util.List;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record IssueSummaryDto(
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

    public IssueSummaryDto {
        labels = labels == null ? List.of() : List.copyOf(labels);
        changeSets = changeSets == null ? null : List.copyOf(changeSets);
    }
}
