package com.gitlabflow.floworchestrator.orchestration.issues;

import com.gitlabflow.floworchestrator.orchestration.issues.model.ListIssuesQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.model.ListIssuesResult;

public interface IssuesProvider {

    ListIssuesResult listIssues(ListIssuesQuery query);
}
