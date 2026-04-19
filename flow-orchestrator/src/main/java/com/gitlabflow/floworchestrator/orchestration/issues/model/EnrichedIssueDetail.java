package com.gitlabflow.floworchestrator.orchestration.issues.model;

import com.gitlabflow.floworchestrator.orchestration.common.model.ChangeSet;
import java.util.List;
import lombok.Builder;

@Builder
public record EnrichedIssueDetail(IssueDetail issueDetail, List<ChangeSet<?>> changeSets) {

    public EnrichedIssueDetail {
        changeSets = changeSets == null ? List.of() : List.copyOf(changeSets);
    }
}
