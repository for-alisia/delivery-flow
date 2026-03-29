package com.gitlabflow.floworchestrator.orchestration.controllers.models;

public record ListProjectIssuesRequestBody(
        Integer page,
        Integer pageSize
) {
}
