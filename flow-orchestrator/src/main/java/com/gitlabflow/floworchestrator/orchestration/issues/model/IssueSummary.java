package com.gitlabflow.floworchestrator.orchestration.issues.model;

import com.gitlabflow.floworchestrator.orchestration.common.model.ChangeSet;
import java.util.List;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record IssueSummary(
        long id,
        long issueId,
        String title,
        @Nullable String description,
        String state,
        List<String> labels,
        @Nullable String assignee,
        @Nullable String milestone,
        @Nullable Long parent,
        @Nullable List<ChangeSet<?>> changeSets) {

    public IssueSummary {
        labels = labels == null ? List.of() : List.copyOf(labels);
        changeSets = changeSets == null ? null : List.copyOf(changeSets);
    }
}
