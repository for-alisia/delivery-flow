package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import java.util.List;

public record SearchIssuesResponse(
        List<IssueDto> items,
        int count,
        int page
) {

        public SearchIssuesResponse {
                items = items == null ? List.of() : List.copyOf(items);
        }
}
