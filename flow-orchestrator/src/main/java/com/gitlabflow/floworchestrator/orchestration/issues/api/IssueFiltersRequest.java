package com.gitlabflow.floworchestrator.orchestration.issues.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Objects;

public record IssueFiltersRequest(
        @Pattern(regexp = "opened|closed|all", message = "must be one of [opened, closed, all]")
        String state,
        @Size(max = 1, message = "must contain at most 1 value")
        List<String> labels,
        @Size(max = 1, message = "must contain at most 1 value")
        List<String> assignee,
        @Size(max = 1, message = "must contain at most 1 value")
        List<String> milestone
) {
    public IssueFiltersRequest {
        labels = sanitize(labels);
        assignee = sanitize(assignee);
        milestone = sanitize(milestone);
    }

    private static List<String> sanitize(final List<String> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
    }
}
