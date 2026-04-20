package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record UpdateIssueRequest(
        @Nullable String title,
        @Nullable String description,
        @Nullable List<String> addLabels,
        @Nullable List<String> removeLabels) {

    public UpdateIssueRequest {
        addLabels = addLabels == null ? null : Collections.unmodifiableList(new ArrayList<>(addLabels));
        removeLabels = removeLabels == null ? null : Collections.unmodifiableList(new ArrayList<>(removeLabels));
    }
}
