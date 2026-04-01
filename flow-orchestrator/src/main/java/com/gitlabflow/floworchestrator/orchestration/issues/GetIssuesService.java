package com.gitlabflow.floworchestrator.orchestration.issues;

import com.gitlabflow.floworchestrator.common.errors.ValidationException;
import com.gitlabflow.floworchestrator.config.IssuesApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetIssuesService {

    private final GetIssuesPort getIssuesPort;
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

        final IssuePage issuePage = getIssuesPort.getIssues(query);
        log.info("Issues retrieved count={} page={}", issuePage.count(), issuePage.page());
        return issuePage;
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