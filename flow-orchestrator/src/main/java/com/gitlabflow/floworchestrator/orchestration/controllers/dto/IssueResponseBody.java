package com.gitlabflow.floworchestrator.orchestration.controllers.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record IssueResponseBody(
        long issueNumber,
        String title,
        String state,
        List<String> labels,
        List<AssigneeResponseBody> assignees,
        String webUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
