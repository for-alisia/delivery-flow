package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import jakarta.validation.Valid;

public record SearchIssuesRequest(
        @Valid PaginationRequest pagination,
        @Valid IssueFiltersRequest filters
) {
}