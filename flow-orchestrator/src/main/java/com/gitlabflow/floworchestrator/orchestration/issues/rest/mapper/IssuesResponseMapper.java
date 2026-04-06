package com.gitlabflow.floworchestrator.orchestration.issues.rest.mapper;

import com.gitlabflow.floworchestrator.orchestration.issues.model.Issue;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesResponse;
import org.springframework.stereotype.Component;

@Component
public class IssuesResponseMapper {

    public SearchIssuesResponse toSearchIssuesResponse(final IssuePage issuePage) {
        return new SearchIssuesResponse(
                issuePage.items().stream().map(this::toIssueDto).toList(), issuePage.count(), issuePage.page());
    }

    public IssueDto toIssueDto(final Issue issue) {
        return new IssueDto(
                issue.id(),
                issue.issueId(),
                issue.title(),
                issue.description(),
                issue.state(),
                issue.labels(),
                issue.assignee(),
                issue.milestone(),
                issue.parent());
    }
}
