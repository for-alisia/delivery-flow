package com.gitlabflow.floworchestrator.orchestration.issues;

import com.gitlabflow.floworchestrator.common.error.ValidationException;
import com.gitlabflow.floworchestrator.config.IssuesApiProperties;
import com.gitlabflow.floworchestrator.orchestration.common.async.AsyncComposer;
import com.gitlabflow.floworchestrator.orchestration.common.model.ChangeSet;
import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.EnrichedIssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueAuditType;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueSummary;
import com.gitlabflow.floworchestrator.orchestration.issues.model.UpdateIssueInput;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssuesService {

    private final IssuesPort issuesPort;
    private final IssuesApiProperties issuesApiProperties;
    private final AsyncComposer asyncComposer;

    public IssuePage getIssues(final IssueQuery query) {
        validatePerPage(query.perPage());
        final long startedAt = System.nanoTime();

        log.info(
                "Requesting issues page={} perPage={} auditTypes={} filters=[state:{},label:{},assignee:{},milestone:{}]",
                query.page(),
                query.perPage(),
                query.auditTypes(),
                query.state(),
                query.label(),
                query.assignee(),
                query.milestone());

        final IssuePage issuePage = issuesPort.getIssues(query);
        if (!query.auditTypes().contains(IssueAuditType.LABEL)
                || issuePage.items().isEmpty()) {
            log.info(
                    "Issues retrieved count={} page={} auditTypes={} enrichmentApplied=false durationMs={}",
                    issuePage.count(),
                    issuePage.page(),
                    query.auditTypes(),
                    toDurationMs(startedAt));
            return issuePage;
        }

        final IssuePage enrichedIssuePage = enrichWithLabelEvents(issuePage);
        log.info(
                "Issues retrieved count={} page={} auditTypes={} enrichmentApplied=true durationMs={}",
                enrichedIssuePage.count(),
                enrichedIssuePage.page(),
                query.auditTypes(),
                toDurationMs(startedAt));
        return enrichedIssuePage;
    }

    public IssueSummary createIssue(final CreateIssueInput input) {
        log.info(
                "Creating issue titleLength={} labelCount={} descriptionPresent={}",
                input.title().length(),
                input.labels().size(),
                input.description() != null);

        final IssueSummary issue = issuesPort.createIssue(input);
        log.info("Issue created id={}", issue.id());
        return issue;
    }

    public IssueSummary updateIssue(final UpdateIssueInput input) {
        final int effectiveFieldCount = countEffectiveUpdateFields(input);
        log.info(
                "Updating issue issueId={} effectiveFieldCount={} addLabelCount={} removeLabelCount={}",
                input.issueId(),
                effectiveFieldCount,
                input.addLabels().size(),
                input.removeLabels().size());

        final IssueSummary issue = issuesPort.updateIssue(input);
        log.info("Issue updated issueId={}", input.issueId());
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
                asyncComposer.submit(() -> issuesPort.getIssueDetail(issueId));
        final CompletableFuture<List<ChangeSet<?>>> changeSetsFuture =
                asyncComposer.submit(() -> issuesPort.getLabelEvents(issueId));

        asyncComposer.joinFailFast(List.of(issueDetailFuture, changeSetsFuture));

        final IssueDetail issueDetail = issueDetailFuture.join();
        final List<ChangeSet<?>> changeSets = changeSetsFuture.join();
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

    private void validatePerPage(final int perPage) {
        if (perPage > issuesApiProperties.maxPageSize()) {
            throw new ValidationException(
                    "Request validation failed",
                    List.of("pagination.perPage must be less than or equal to " + issuesApiProperties.maxPageSize()));
        }
    }

    private IssuePage enrichWithLabelEvents(final IssuePage issuePage) {
        final List<CompletableFuture<List<ChangeSet<?>>>> changeSetFutures = issuePage.items().stream()
                .map(issue -> asyncComposer.submit(() -> issuesPort.getLabelEvents(issue.issueId())))
                .toList();

        asyncComposer.joinFailFast(changeSetFutures);

        final List<IssueSummary> enrichedItems =
                new ArrayList<>(issuePage.items().size());
        for (int index = 0; index < issuePage.items().size(); index++) {
            enrichedItems.add(enrichIssue(
                    issuePage.items().get(index), changeSetFutures.get(index).join()));
        }

        return IssuePage.builder()
                .items(enrichedItems)
                .count(issuePage.count())
                .page(issuePage.page())
                .build();
    }

    private IssueSummary enrichIssue(final IssueSummary issue, final List<ChangeSet<?>> changeSets) {
        return issue.toBuilder().changeSets(changeSets).build();
    }

    private long toDurationMs(final long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private int countEffectiveUpdateFields(final UpdateIssueInput input) {
        int count = 0;
        if (input.title() != null) {
            count++;
        }
        if (input.description() != null) {
            count++;
        }
        if (!input.addLabels().isEmpty()) {
            count++;
        }
        if (!input.removeLabels().isEmpty()) {
            count++;
        }
        return count;
    }
}
