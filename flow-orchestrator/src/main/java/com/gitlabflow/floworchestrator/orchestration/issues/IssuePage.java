package com.gitlabflow.floworchestrator.orchestration.issues;

import java.util.List;

public record IssuePage(
        List<Issue> items,
        int count,
        int page
) {
    public IssuePage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}