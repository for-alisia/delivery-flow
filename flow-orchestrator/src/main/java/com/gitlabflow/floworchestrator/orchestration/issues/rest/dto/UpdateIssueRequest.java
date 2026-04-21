package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import com.gitlabflow.floworchestrator.common.util.ImmutableListCopies;
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
        addLabels = ImmutableListCopies.copyPreservingNullsOrNull(addLabels);
        removeLabels = ImmutableListCopies.copyPreservingNullsOrNull(removeLabels);
    }

    @Override
    @Nullable
    public List<String> addLabels() {
        return ImmutableListCopies.copyPreservingNullsOrNull(addLabels);
    }

    @Override
    @Nullable
    public List<String> removeLabels() {
        return ImmutableListCopies.copyPreservingNullsOrNull(removeLabels);
    }
}
