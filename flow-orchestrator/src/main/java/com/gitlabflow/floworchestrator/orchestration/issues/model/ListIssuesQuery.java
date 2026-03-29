package com.gitlabflow.floworchestrator.orchestration.issues.model;

public record ListIssuesQuery(
        String label,
        String assignee,
        int page,
        int pageSize
) {
}
