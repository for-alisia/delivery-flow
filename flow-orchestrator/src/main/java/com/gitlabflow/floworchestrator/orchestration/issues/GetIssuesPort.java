package com.gitlabflow.floworchestrator.orchestration.issues;

public interface GetIssuesPort {

    IssuePage getIssues(IssueQuery query);
}