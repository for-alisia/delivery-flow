package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record CreateIssueRequest(
        @Nullable String title,
        @Nullable String description,
        @Nullable List<String> labels) {

    public CreateIssueRequest {
        labels = labels == null ? null : Collections.unmodifiableList(new ArrayList<>(labels));
    }
}
