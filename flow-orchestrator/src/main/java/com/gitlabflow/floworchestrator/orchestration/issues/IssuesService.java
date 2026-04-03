package com.gitlabflow.floworchestrator.orchestration.issues;

import com.gitlabflow.floworchestrator.common.error.ValidationException;
import com.gitlabflow.floworchestrator.config.IssuesApiProperties;
import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.Issue;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssuesService {

    private final IssuesPort issuesPort;
    private final IssuesApiProperties issuesApiProperties;

    public IssuePage getIssues(final IssueQuery query) {
        validatePerPage(query.perPage());

        log.info(
                "Requesting issues page={} perPage={} filters=[state:{},labels:{},assignee:{},milestone:{}]",
                query.page(),
                query.perPage(),
                query.state() != null,
                query.label() != null,
                query.assignee() != null,
                query.milestone() != null
        );

        final IssuePage issuePage = issuesPort.getIssues(query);
        log.info("Issues retrieved count={} page={}", issuePage.count(), issuePage.page());
        return issuePage;
    }

    public Issue createIssue(final CreateIssueInput input) {
        log.info("Creating issue");

        final Issue issue = issuesPort.createIssue(input);
        log.info("Issue created id={}", issue.id());
        return issue;
    }

    private void validatePerPage(final int perPage) {
        if (perPage > issuesApiProperties.maxPageSize()) {
            throw new ValidationException(
                    "Request validation failed",
                    List.of("pagination.perPage must be less than or equal to " + issuesApiProperties.maxPageSize())
            );
        }
    }
}