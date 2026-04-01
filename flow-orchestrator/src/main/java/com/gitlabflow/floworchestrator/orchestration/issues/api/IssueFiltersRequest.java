package com.gitlabflow.floworchestrator.orchestration.issues.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collections;
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
        labels = labels == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(labels.stream().filter(Objects::nonNull).toList()));
        assignee = assignee == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(assignee.stream().filter(Objects::nonNull).toList()));
        milestone = milestone == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(milestone.stream().filter(Objects::nonNull).toList()));
    }

    @Override
    public List<String> labels() {
        return List.copyOf(labels);
    }

    @Override
    public List<String> assignee() {
        return List.copyOf(assignee);
    }

    @Override
    public List<String> milestone() {
        return List.copyOf(milestone);
    }
}
