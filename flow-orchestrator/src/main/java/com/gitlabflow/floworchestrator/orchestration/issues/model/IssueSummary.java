package com.gitlabflow.floworchestrator.orchestration.issues.model;

import java.time.OffsetDateTime;
import java.util.List;

public record IssueSummary(
        long issueNumber,
        String title,
        String state,
        List<String> labels,
        List<AssigneeSummary> assignees,
        String webUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
