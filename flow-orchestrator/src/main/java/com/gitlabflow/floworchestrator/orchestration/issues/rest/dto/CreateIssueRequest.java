package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import com.gitlabflow.floworchestrator.common.util.ImmutableListCopies;
import java.util.List;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record CreateIssueRequest(
        @Nullable String title,
        @Nullable String description,
        @Nullable List<String> labels) {

    public CreateIssueRequest {
        labels = ImmutableListCopies.copyPreservingNullsOrNull(labels);
    }

    @Override
    @Nullable
    public List<String> labels() {
        return ImmutableListCopies.copyPreservingNullsOrNull(labels);
    }
}
