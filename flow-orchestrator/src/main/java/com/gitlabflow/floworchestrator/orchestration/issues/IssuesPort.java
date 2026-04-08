package com.gitlabflow.floworchestrator.orchestration.issues;

import com.gitlabflow.floworchestrator.orchestration.issues.model.ChangeSet;
import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.Issue;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery;
import java.util.List;

public interface IssuesPort {

    IssuePage getIssues(IssueQuery query);

    Issue createIssue(CreateIssueInput input);

    void deleteIssue(long issueId);

    IssueDetail getIssueDetail(long issueId);

    List<ChangeSet> getLabelEvents(long issueId);
}
