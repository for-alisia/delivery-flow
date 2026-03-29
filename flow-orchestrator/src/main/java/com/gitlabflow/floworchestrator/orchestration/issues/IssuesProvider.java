package com.gitlabflow.floworchestrator.orchestration.issues;

import com.gitlabflow.floworchestrator.orchestration.issues.models.ListProjectIssuesQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.models.ListProjectIssuesResult;

public interface IssuesProvider {

    ListProjectIssuesResult listProjectIssues(ListProjectIssuesQuery query);
}
