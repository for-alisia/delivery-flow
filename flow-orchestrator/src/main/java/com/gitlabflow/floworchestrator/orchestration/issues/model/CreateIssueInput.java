package com.gitlabflow.floworchestrator.orchestration.issues.model;

import org.springframework.lang.Nullable;

import java.util.List;

public record CreateIssueInput(
        String title,
        @Nullable
        String description,
        List<String> labels
) {

    public CreateIssueInput {
        labels = labels == null ? List.of() : List.copyOf(labels);
    }
}