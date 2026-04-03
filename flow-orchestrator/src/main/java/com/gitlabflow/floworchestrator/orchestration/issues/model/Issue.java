package com.gitlabflow.floworchestrator.orchestration.issues.model;

import java.util.List;
import org.springframework.lang.Nullable;

public record Issue(
        long id,
        String title,
        @Nullable String description,
        String state,
        List<String> labels,
        @Nullable String assignee,
        @Nullable String milestone,
        @Nullable Long parent) {

    public Issue {
        labels = labels == null ? List.of() : List.copyOf(labels);
    }
}
