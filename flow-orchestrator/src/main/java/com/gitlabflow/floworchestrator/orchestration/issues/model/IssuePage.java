package com.gitlabflow.floworchestrator.orchestration.issues.model;

import java.util.List;
import lombok.Builder;

@Builder
public record IssuePage(List<IssueSummary> items, int count, int page) {

    public IssuePage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
