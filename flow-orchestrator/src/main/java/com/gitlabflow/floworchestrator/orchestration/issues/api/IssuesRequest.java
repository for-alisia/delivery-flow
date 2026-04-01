package com.gitlabflow.floworchestrator.orchestration.issues.api;

import jakarta.validation.Valid;

public record IssuesRequest(
        @Valid PaginationRequest pagination,
        @Valid IssueFiltersRequest filters
) {
}