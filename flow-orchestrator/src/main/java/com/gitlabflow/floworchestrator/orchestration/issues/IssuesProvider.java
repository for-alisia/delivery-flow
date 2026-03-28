package com.gitlabflow.floworchestrator.orchestration.issues;

import java.util.List;

import com.gitlabflow.floworchestrator.orchestration.issues.models.IssueSummary;
import com.gitlabflow.floworchestrator.orchestration.issues.models.GetIssuesRequest;

public interface IssuesProvider {
    List<IssueSummary> fetchIssues(GetIssuesRequest request);
}
