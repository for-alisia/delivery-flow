package com.gitlabflow.floworchestrator.orchestration.issues.api;

import org.springframework.lang.Nullable;

import java.util.List;

public record IssueResponseItem(
        long id,
        String title,
    @Nullable
        String description,
        String state,
        List<String> labels,
    @Nullable
        String assignee,
    @Nullable
        String milestone,
    @Nullable
        Long parent
) {
    public IssueResponseItem {
        labels = labels == null ? List.of() : List.copyOf(labels);
    }
}