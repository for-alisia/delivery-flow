package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import java.util.List;
import org.springframework.lang.Nullable;

public record IssueDto(
        long id,
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
