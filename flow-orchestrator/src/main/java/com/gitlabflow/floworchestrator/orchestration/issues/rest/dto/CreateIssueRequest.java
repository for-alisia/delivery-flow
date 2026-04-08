package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record CreateIssueRequest(
        @NotBlank String title,
        @Nullable String description,
        @Nullable List<@NotBlank String> labels) {
    public CreateIssueRequest {
        labels = labels == null
                ? null
                : List.copyOf(labels.stream().filter(Objects::nonNull).toList());
    }
}
