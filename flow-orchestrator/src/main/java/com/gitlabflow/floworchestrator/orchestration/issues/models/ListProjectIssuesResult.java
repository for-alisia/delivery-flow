package com.gitlabflow.floworchestrator.orchestration.issues.models;

import java.util.List;

public record ListProjectIssuesResult(
        List<IssueSummary> items,
        PaginationMetadata pagination
) {
}
