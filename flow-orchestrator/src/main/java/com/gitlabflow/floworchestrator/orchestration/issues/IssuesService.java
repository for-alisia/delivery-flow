package com.gitlabflow.floworchestrator.orchestration.issues;

import com.gitlabflow.floworchestrator.common.error.ValidationException;
import com.gitlabflow.floworchestrator.config.IssuesApiProperties;
import com.gitlabflow.floworchestrator.orchestration.issues.model.ChangeSet;
import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.EnrichedIssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.Issue;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssuesService {

    private final IssuesPort issuesPort;
    private final IssuesApiProperties issuesApiProperties;

    public IssuePage getIssues(final IssueQuery query) {
        validatePerPage(query.perPage());

        log.info(
                "Requesting issues page={} perPage={} filters=[state:{},label:{},assignee:{},milestone:{}]",
                query.page(),
                query.perPage(),
                query.state(),
                query.label(),
                query.assignee(),
                query.milestone());

        final IssuePage issuePage = issuesPort.getIssues(query);
        log.info("Issues retrieved count={} page={}", issuePage.count(), issuePage.page());
        return issuePage;
    }

    public Issue createIssue(final CreateIssueInput input) {
        log.info(
                "Creating issue titleLength={} labelCount={} descriptionPresent={}",
                input.title().length(),
                input.labels().size(),
                input.description() != null);

        final Issue issue = issuesPort.createIssue(input);
        log.info("Issue created id={}", issue.id());
        return issue;
    }

    public void deleteIssue(final long issueId) {
        log.info("Deleting issue issueId={}", issueId);
        issuesPort.deleteIssue(issueId);
        log.info("Issue deleted issueId={}", issueId);
    }

    public EnrichedIssueDetail getIssueDetail(final long issueId) {
        log.info("Fetching issue detail and label events issueId={}", issueId);
        final long startedAt = System.nanoTime();

        final CompletableFuture<IssueDetail> issueDetailFuture =
                CompletableFuture.supplyAsync(() -> issuesPort.getIssueDetail(issueId));
        final CompletableFuture<List<ChangeSet>> changeSetsFuture =
                CompletableFuture.supplyAsync(() -> issuesPort.getLabelEvents(issueId));

        try {
            CompletableFuture.allOf(issueDetailFuture, changeSetsFuture).join();
        } catch (final CompletionException exception) {
            issueDetailFuture.cancel(true);
            changeSetsFuture.cancel(true);
            throw unwrapCompletionFailure(exception);
        }

        final IssueDetail issueDetail = issueDetailFuture.join();
        final List<ChangeSet> changeSets = changeSetsFuture.join();
        final long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
        log.info(
                "Issue detail composed issueId={} changeSetCount={} durationMs={}",
                issueId,
                changeSets.size(),
                durationMs);

        return EnrichedIssueDetail.builder()
                .issueDetail(issueDetail)
                .changeSets(changeSets)
                .build();
    }

    private RuntimeException unwrapCompletionFailure(final CompletionException exception) {
        final Throwable cause = exception.getCause();
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        return new IllegalStateException("Issue detail composition failed", cause);
    }

    private void validatePerPage(final int perPage) {
        if (perPage > issuesApiProperties.maxPageSize()) {
            throw new ValidationException(
                    "Request validation failed",
                    List.of("pagination.perPage must be less than or equal to " + issuesApiProperties.maxPageSize()));
        }
    }
}
