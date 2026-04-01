package com.gitlabflow.floworchestrator.orchestration.issues;

import org.springframework.lang.Nullable;

public record IssueQuery(
        int page,
        int perPage,
        @Nullable
        IssueState state,
        @Nullable
        String label,
        @Nullable
        String assignee,
        @Nullable
        String milestone
) {
}