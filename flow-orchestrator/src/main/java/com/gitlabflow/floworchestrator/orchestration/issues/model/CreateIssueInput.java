package com.gitlabflow.floworchestrator.orchestration.issues.model;

import java.util.List;
import org.springframework.lang.Nullable;

public record CreateIssueInput(String title, @Nullable String description, List<String> labels) {

    public CreateIssueInput {
        labels = labels == null ? List.of() : List.copyOf(labels);
    }
}
