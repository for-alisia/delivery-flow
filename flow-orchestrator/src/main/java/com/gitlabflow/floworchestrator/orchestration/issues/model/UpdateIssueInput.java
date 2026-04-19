package com.gitlabflow.floworchestrator.orchestration.issues.model;

import java.util.List;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record UpdateIssueInput(
        long issueId,
        @Nullable String title,
        @Nullable String description,
        List<String> addLabels,
        List<String> removeLabels) {

    public UpdateIssueInput {
        addLabels = addLabels == null ? List.of() : List.copyOf(addLabels);
        removeLabels = removeLabels == null ? List.of() : List.copyOf(removeLabels);
    }
}
