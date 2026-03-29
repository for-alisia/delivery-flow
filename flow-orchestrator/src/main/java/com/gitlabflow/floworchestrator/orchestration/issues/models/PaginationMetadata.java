package com.gitlabflow.floworchestrator.orchestration.issues.models;

public record PaginationMetadata(
        int currentPage,
        int pageSize,
        Integer previousPage,
        Integer nextPage,
        Long totalItems,
        Long totalPages
) {
}
