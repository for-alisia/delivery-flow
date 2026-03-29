package com.gitlabflow.floworchestrator.orchestration.issues.models;

public record IssueSummary(
        Long id,
        Long iid,
        String title,
        String state,
        String webUrl
) {
}
