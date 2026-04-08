package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record SearchIssuesResponse(List<IssueDto> items, int count, int page) {

    public SearchIssuesResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
