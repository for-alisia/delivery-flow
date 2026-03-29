package com.gitlabflow.floworchestrator.orchestration.controllers.models;

import java.util.List;

public record ListProjectIssuesResponse(
        List<ProjectIssueResponseItem> items,
        PaginationResponse pagination
) {
}
