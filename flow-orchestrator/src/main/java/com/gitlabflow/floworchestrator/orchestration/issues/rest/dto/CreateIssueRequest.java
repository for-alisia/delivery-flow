package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import java.util.List;
import java.util.Objects;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record CreateIssueRequest(
        @Nullable String title,
        @Nullable String description,
        @Nullable List<String> labels) {

    public CreateIssueRequest {
        labels = labels == null
                ? null
                : List.copyOf(labels.stream().filter(Objects::nonNull).toList());
    }
}
