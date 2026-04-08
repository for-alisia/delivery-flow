package com.gitlabflow.floworchestrator.orchestration.issues;

import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.Issue;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery;

public interface IssuesPort {

    IssuePage getIssues(IssueQuery query);

    Issue createIssue(CreateIssueInput input);

    void deleteIssue(long issueId);

    IssueDetail getIssueDetail(long issueId);
}
