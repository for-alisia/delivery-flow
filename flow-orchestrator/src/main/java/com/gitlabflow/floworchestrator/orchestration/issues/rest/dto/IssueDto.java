package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import java.util.List;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record IssueDto(
        long id,
        long issueId,
        String title,
        @Nullable String description,
        String state,
        List<String> labels,
        @Nullable String assignee,
        @Nullable String milestone,
        @Nullable Long parent) {
    public IssueDto {
        labels = labels == null ? List.of() : List.copyOf(labels);
    }
}
