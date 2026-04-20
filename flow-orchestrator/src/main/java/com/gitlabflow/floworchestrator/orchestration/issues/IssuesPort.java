package com.gitlabflow.floworchestrator.orchestration.issues;

import com.gitlabflow.floworchestrator.orchestration.common.model.ChangeSet;
import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueSummary;
import com.gitlabflow.floworchestrator.orchestration.issues.model.UpdateIssueInput;
import java.util.List;

public interface IssuesPort {

    IssuePage getIssues(IssueQuery query);

    IssueSummary createIssue(CreateIssueInput input);

    IssueSummary updateIssue(UpdateIssueInput input);

    void deleteIssue(long issueId);

    IssueDetail getIssueDetail(long issueId);

    List<ChangeSet<?>> getLabelEvents(long issueId);
}
