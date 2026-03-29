package com.gitlabflow.floworchestrator.orchestration.controllers.models;

public record PaginationResponse(
        int currentPage,
        int pageSize,
        Integer previousPage,
        Integer nextPage,
        Long totalItems,
        Long totalPages
) {
}
