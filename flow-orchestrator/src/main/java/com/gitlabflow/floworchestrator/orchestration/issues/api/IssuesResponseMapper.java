package com.gitlabflow.floworchestrator.orchestration.issues.api;

import com.gitlabflow.floworchestrator.orchestration.issues.Issue;
import com.gitlabflow.floworchestrator.orchestration.issues.IssuePage;
import org.springframework.stereotype.Component;

@Component
public class IssuesResponseMapper {

    public IssuesResponse toResponse(final IssuePage issuePage) {
        return new IssuesResponse(
                issuePage.items().stream().map(this::toItem).toList(),
                issuePage.count(),
                issuePage.page()
        );
    }

    private IssueResponseItem toItem(final Issue issue) {
        return new IssueResponseItem(
                issue.id(),
                issue.title(),
                issue.description(),
                issue.state(),
                issue.labels(),
                issue.assignee(),
                issue.milestone(),
                issue.parent()
        );
    }
}