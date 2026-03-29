package com.gitlabflow.floworchestrator.orchestration.controllers.dto;

import java.util.List;

public record ListIssuesResponseBody(
        List<IssueResponseBody> issues,
        int page,
        int pageSize
) {
}
