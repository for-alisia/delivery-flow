package com.gitlabflow.floworchestrator.orchestration.issues.model;

import java.util.List;
import lombok.Builder;

@Builder
public record EnrichedIssueDetail(IssueDetail issueDetail, List<Object> changeSets) {

    public EnrichedIssueDetail {
        changeSets = changeSets == null ? List.of() : List.copyOf(changeSets);
    }
}
