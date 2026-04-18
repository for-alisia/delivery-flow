package com.gitlabflow.floworchestrator.orchestration.issues.model;

import java.util.List;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record IssueQuery(
        int page,
        int perPage,
        @Nullable IssueState state,
        @Nullable String label,
        @Nullable String assignee,
        @Nullable String milestone,
        List<IssueAuditType> auditTypes) {

    public IssueQuery {
        auditTypes = auditTypes == null ? List.of() : List.copyOf(auditTypes);
    }
}
