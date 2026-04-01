package com.gitlabflow.floworchestrator.orchestration.issues.api;

import java.util.List;

public record IssuesResponse(
        List<IssueResponseItem> items,
        int count,
        int page
) {
    public IssuesResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}