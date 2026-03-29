package com.gitlabflow.floworchestrator.orchestration.issues.model;

import java.util.List;

public record ListIssuesResult(
        List<IssueSummary> issues,
        int page,
        int pageSize
) {
    public ListIssuesResult {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
