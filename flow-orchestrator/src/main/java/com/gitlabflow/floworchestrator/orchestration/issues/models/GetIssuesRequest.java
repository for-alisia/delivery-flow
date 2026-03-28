package com.gitlabflow.floworchestrator.orchestration.issues.models;

public record GetIssuesRequest(
        Long assigneeId,
        Long authorId,
        String milestone,
        String state,
        String search,
        String labels,
        String orderBy,
        String sort,
        Integer page,
        Integer perPage
) {
}
