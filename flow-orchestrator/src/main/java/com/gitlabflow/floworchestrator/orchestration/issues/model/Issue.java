package com.gitlabflow.floworchestrator.orchestration.issues.model;

import java.util.List;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record Issue(
        long id,
        long issueId,
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
