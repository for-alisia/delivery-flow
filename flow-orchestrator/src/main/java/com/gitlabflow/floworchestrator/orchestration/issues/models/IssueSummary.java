package com.gitlabflow.floworchestrator.orchestration.issues.models;

import java.time.OffsetDateTime;
import java.util.List;

public record IssueSummary(
        Long projectId,
        Long id,
        Long iid,
        String title,
        String description,
        String state,
        List<String> labels,
        String authorUsername,
        List<String> assigneeUsernames,
        String webUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime closedAt,
        String milestone
) {
}
