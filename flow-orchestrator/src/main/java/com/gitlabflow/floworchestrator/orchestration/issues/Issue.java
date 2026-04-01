package com.gitlabflow.floworchestrator.orchestration.issues;

import org.springframework.lang.Nullable;

import java.util.List;

public record Issue(
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
    public Issue {
        labels = labels == null ? List.of() : List.copyOf(labels);
    }
}