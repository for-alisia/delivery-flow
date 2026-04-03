package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import jakarta.validation.constraints.Positive;

public record PaginationRequest(
        @Positive Integer page,
        @Positive Integer perPage
) {
}