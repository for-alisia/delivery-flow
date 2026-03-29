package com.gitlabflow.floworchestrator.orchestration.issues.models;

public record ListProjectIssuesQuery(
        String projectId,
        int page,
        int pageSize
) {
}
