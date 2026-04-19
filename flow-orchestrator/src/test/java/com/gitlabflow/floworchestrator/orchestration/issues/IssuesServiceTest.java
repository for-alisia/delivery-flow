package com.gitlabflow.floworchestrator.orchestration.issues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gitlabflow.floworchestrator.common.error.ErrorCode;
import com.gitlabflow.floworchestrator.common.error.IntegrationException;
import com.gitlabflow.floworchestrator.common.error.ValidationException;
import com.gitlabflow.floworchestrator.config.IssuesApiProperties;
import com.gitlabflow.floworchestrator.orchestration.common.async.AsyncComposer;
import com.gitlabflow.floworchestrator.orchestration.common.model.User;
import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.EnrichedIssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueAuditType;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueState;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueSummary;
import com.gitlabflow.floworchestrator.orchestration.issues.model.LabelChange;
import com.gitlabflow.floworchestrator.orchestration.issues.model.LabelChangeSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssuesServiceTest {

    @Mock
    private IssuesPort issuesPort;

    private ExecutorService asyncExecutor;
    private IssuesService issuesService;

    @BeforeEach
    void setUp() {
        asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
        issuesService =
                new IssuesService(issuesPort, new IssuesApiProperties(20, 40), new AsyncComposer(asyncExecutor));
    }

    @AfterEach
    void tearDown() {
        asyncExecutor.close();
    }

    @Test
    @DisplayName("throws validation error when perPage exceeds configured max")
    void throwsValidationWhenPerPageTooLarge() {
        final IssueQuery query = new IssueQuery(1, 41, null, null, null, null, List.of());

        assertThatThrownBy(() -> issuesService.getIssues(query))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Request validation failed");
    }

    @Test
    @DisplayName("delegates to port for valid query")
    void delegatesToPortForValidQuery() {
        final IssueQuery query = new IssueQuery(1, 40, null, null, null, null, List.of());
        final IssuePage page = new IssuePage(List.of(), 0, 1);
        when(issuesPort.getIssues(query)).thenReturn(page);

        final IssuePage response = issuesService.getIssues(query);

        assertThat(response).isEqualTo(page);
        verify(issuesPort).getIssues(query);
    }

    @Test
    @DisplayName("accepts perPage equal to configured max")
    void acceptsPerPageEqualToConfiguredMax() {
        final IssueQuery query = new IssueQuery(1, 40, null, null, null, null, List.of());
        final IssuePage page = new IssuePage(List.of(), 0, 1);
        when(issuesPort.getIssues(query)).thenReturn(page);

        final IssuePage response = issuesService.getIssues(query);

        assertThat(response).isEqualTo(page);
        verify(issuesPort).getIssues(query);
    }

    @Test
    @DisplayName("delegates query with populated filters")
    void delegatesQueryWithPopulatedFilters() {
        final IssueQuery query = new IssueQuery(2, 25, IssueState.OPENED, "bug", "jane", "M1", List.of());
        final IssuePage page = new IssuePage(List.of(), 0, 2);
        when(issuesPort.getIssues(query)).thenReturn(page);

        final IssuePage response = issuesService.getIssues(query);

        assertThat(response).isEqualTo(page);
        verify(issuesPort).getIssues(query);
    }

    @Test
    @DisplayName("returns base issue page and skips enrichment when audit types are empty")
    void returnsBaseIssuePageAndSkipsEnrichmentWhenAuditTypesAreEmpty() {
        final IssueQuery query = new IssueQuery(1, 20, null, null, null, null, List.of());
        final IssuePage page = new IssuePage(
                List.of(new IssueSummary(10L, 7L, "Title", null, "opened", List.of("bug"), null, null, null, null)),
                1,
                1);
        when(issuesPort.getIssues(query)).thenReturn(page);

        final IssuePage response = issuesService.getIssues(query);

        assertThat(response).isEqualTo(page);
        verify(issuesPort).getIssues(query);
        verify(issuesPort, never()).getLabelEvents(anyLong());
    }

    @Test
    @DisplayName("returns empty page without enrichment when search result has no items")
    void returnsEmptyPageWithoutEnrichmentWhenSearchResultHasNoItems() {
        final IssueQuery query = new IssueQuery(1, 20, null, null, null, null, List.of(IssueAuditType.LABEL));
        final IssuePage page = new IssuePage(List.of(), 0, 1);
        when(issuesPort.getIssues(query)).thenReturn(page);

        final IssuePage response = issuesService.getIssues(query);

        assertThat(response).isEqualTo(page);
        verify(issuesPort).getIssues(query);
        verify(issuesPort, never()).getLabelEvents(anyLong());
    }

    @Test
    @DisplayName("enriches issues with label events in original item order when label audit is requested")
    void enrichesIssuesWithLabelEventsInOriginalItemOrderWhenLabelAuditIsRequested() {
        final IssueQuery query = new IssueQuery(1, 20, null, null, null, null, List.of(IssueAuditType.LABEL));
        final IssueSummary firstIssue =
                new IssueSummary(10L, 7L, "First", null, "opened", List.of("bug"), null, null, null, null);
        final IssueSummary secondIssue =
                new IssueSummary(11L, 8L, "Second", null, "opened", List.of("backend"), null, null, null, null);
        final IssuePage page = new IssuePage(List.of(firstIssue, secondIssue), 2, 1);
        final LabelChangeSet firstChangeSet = LabelChangeSet.builder()
                .changeType("add")
                .changedBy(User.builder()
                        .id(1L)
                        .username("root")
                        .name("Administrator")
                        .build())
                .change(LabelChange.builder().id(73L).name("bug").build())
                .changedAt(OffsetDateTime.parse("2026-01-15T09:30:00Z"))
                .build();
        final LabelChangeSet secondChangeSet = LabelChangeSet.builder()
                .changeType("remove")
                .changedBy(
                        User.builder().id(2L).username("jdoe").name("Jane Doe").build())
                .change(LabelChange.builder().id(99L).name("backend").build())
                .changedAt(OffsetDateTime.parse("2026-01-16T10:00:00Z"))
                .build();
        when(issuesPort.getIssues(query)).thenReturn(page);
        when(issuesPort.getLabelEvents(7L)).thenReturn(List.of(firstChangeSet));
        when(issuesPort.getLabelEvents(8L)).thenReturn(List.of(secondChangeSet));

        final IssuePage response = issuesService.getIssues(query);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().getFirst().issueId()).isEqualTo(7L);
        assertThat(response.items().getFirst().changeSets()).containsExactly(firstChangeSet);
        assertThat(response.items().get(1).issueId()).isEqualTo(8L);
        assertThat(response.items().get(1).changeSets()).containsExactly(secondChangeSet);
        verify(issuesPort).getLabelEvents(7L);
        verify(issuesPort).getLabelEvents(8L);
    }

    @Test
    @DisplayName("propagates first enrichment failure and returns no partially enriched page")
    void propagatesFirstEnrichmentFailureAndReturnsNoPartiallyEnrichedPage() {
        final IssueQuery query = new IssueQuery(1, 20, null, null, null, null, List.of(IssueAuditType.LABEL));
        final IssueSummary firstIssue =
                new IssueSummary(10L, 7L, "First", null, "opened", List.of("bug"), null, null, null, null);
        final IssueSummary secondIssue =
                new IssueSummary(11L, 8L, "Second", null, "opened", List.of("backend"), null, null, null, null);
        final IssuePage page = new IssuePage(List.of(firstIssue, secondIssue), 2, 1);
        final IntegrationException failure = new IntegrationException(
                ErrorCode.INTEGRATION_FAILURE, "GitLab get label events operation failed", "gitlab");
        when(issuesPort.getIssues(query)).thenReturn(page);
        when(issuesPort.getLabelEvents(7L)).thenReturn(List.of());
        when(issuesPort.getLabelEvents(8L)).thenThrow(failure);

        assertThatThrownBy(() -> issuesService.getIssues(query)).isSameAs(failure);

        verify(issuesPort).getIssues(query);
        verify(issuesPort).getLabelEvents(7L);
        verify(issuesPort).getLabelEvents(8L);
    }

    @Test
    @DisplayName("delegates create input to port and returns issue")
    void delegatesCreateInputToPortAndReturnsIssue() {
        final CreateIssueInput input =
                new CreateIssueInput("Deploy failure", "Step 3 failed", List.of("bug", "deploy"));
        final IssueSummary issue = new IssueSummary(
                84L,
                10L,
                "Deploy failure",
                "Step 3 failed",
                "opened",
                List.of("bug", "deploy"),
                null,
                null,
                null,
                null);
        when(issuesPort.createIssue(input)).thenReturn(issue);

        final IssueSummary response = issuesService.createIssue(input);

        assertThat(response).isEqualTo(issue);
        verify(issuesPort).createIssue(input);
    }

    @Test
    @DisplayName("returns null description unchanged when port returns null description")
    void returnsNullDescriptionUnchanged() {
        final CreateIssueInput input = new CreateIssueInput("Reporting bug", null, List.of());
        final IssueSummary issue =
                new IssueSummary(85L, 11L, "Reporting bug", null, "opened", List.of(), null, null, null, null);
        when(issuesPort.createIssue(input)).thenReturn(issue);

        final IssueSummary response = issuesService.createIssue(input);

        assertThat(response.description()).isNull();
        assertThat(response).isEqualTo(issue);
        verify(issuesPort).createIssue(input);
    }

    @Test
    @DisplayName("propagates integration exception without wrapping")
    void propagatesIntegrationExceptionWithoutWrapping() {
        final CreateIssueInput input = new CreateIssueInput("Reporting bug", null, List.of());
        final IntegrationException exception = new IntegrationException(
                ErrorCode.INTEGRATION_FAILURE, "GitLab create issue operation failed", "gitlab");
        when(issuesPort.createIssue(input)).thenThrow(exception);

        assertThatThrownBy(() -> issuesService.createIssue(input)).isSameAs(exception);
    }

    @Test
    @DisplayName("delegates delete issue to port")
    void delegatesDeleteIssueToPort() {
        issuesService.deleteIssue(42L);

        verify(issuesPort).deleteIssue(42L);
    }

    @Test
    @DisplayName("getIssueDetail populates changeSets from port")
    void getIssueDetailPopulatesChangeSetsFromPort() {
        final IssueDetail detail = new IssueDetail(
                42L,
                "Fix login bug",
                null,
                "opened",
                List.of(),
                List.of(),
                null,
                OffsetDateTime.parse("2026-01-04T15:31:51.081Z"),
                OffsetDateTime.parse("2026-03-12T09:00:00.000Z"),
                null);
        final LabelChangeSet firstChangeSet = LabelChangeSet.builder()
                .changeType("add")
                .changedBy(User.builder()
                        .id(1L)
                        .username("root")
                        .name("Administrator")
                        .build())
                .change(LabelChange.builder().id(73L).name("bug").build())
                .changedAt(OffsetDateTime.parse("2026-01-15T09:30:00.000Z"))
                .build();
        final LabelChangeSet secondChangeSet = LabelChangeSet.builder()
                .changeType("remove")
                .changedBy(
                        User.builder().id(2L).username("jdoe").name("Jane Doe").build())
                .change(LabelChange.builder().id(73L).name("bug").build())
                .changedAt(OffsetDateTime.parse("2026-02-01T11:00:00.000Z"))
                .build();
        when(issuesPort.getIssueDetail(42L)).thenReturn(detail);
        when(issuesPort.getLabelEvents(42L)).thenReturn(List.of(firstChangeSet, secondChangeSet));

        final EnrichedIssueDetail result = issuesService.getIssueDetail(42L);

        assertThat(result.issueDetail()).isEqualTo(detail);
        assertThat(result.changeSets()).containsExactly(firstChangeSet, secondChangeSet);
        verify(issuesPort).getIssueDetail(42L);
        verify(issuesPort).getLabelEvents(42L);
    }

    @Test
    @DisplayName("getIssueDetail propagates IntegrationException from port unchanged")
    void getIssueDetailPropagatesIntegrationExceptionUnchanged() {
        final IntegrationException exception = new IntegrationException(
                ErrorCode.INTEGRATION_FAILURE, "GitLab get issue detail operation failed", "gitlab");
        when(issuesPort.getIssueDetail(42L)).thenThrow(exception);

        final IntegrationException thrown =
                assertThrowsExactly(IntegrationException.class, () -> issuesService.getIssueDetail(42L));

        assertThat(thrown).isSameAs(exception);
    }

    @Test
    @DisplayName("getIssueDetail requests label events in parallel even when detail call fails")
    void getIssueDetailRequestsLabelEventsInParallelEvenWhenDetailCallFails() {
        final IntegrationException exception = new IntegrationException(
                ErrorCode.INTEGRATION_FAILURE, "GitLab get issue detail operation failed", "gitlab");
        when(issuesPort.getIssueDetail(42L)).thenThrow(exception);
        when(issuesPort.getLabelEvents(42L)).thenReturn(List.of());

        final IntegrationException thrown =
                assertThrowsExactly(IntegrationException.class, () -> issuesService.getIssueDetail(42L));

        assertThat(thrown).isSameAs(exception);
        verify(issuesPort).getIssueDetail(42L);
        verify(issuesPort).getLabelEvents(42L);
    }

    @Test
    @DisplayName("getIssueDetail propagates IntegrationException when label events call fails")
    void getIssueDetailPropagatesIntegrationExceptionWhenLabelEventsCallFails() {
        final IssueDetail detail = new IssueDetail(
                42L,
                "Fix login bug",
                null,
                "opened",
                List.of(),
                List.of(),
                null,
                OffsetDateTime.parse("2026-01-04T15:31:51.081Z"),
                OffsetDateTime.parse("2026-03-12T09:00:00.000Z"),
                null);
        final IntegrationException exception = new IntegrationException(
                ErrorCode.INTEGRATION_FAILURE, "GitLab label events operation failed", "gitlab");
        when(issuesPort.getIssueDetail(42L)).thenReturn(detail);
        when(issuesPort.getLabelEvents(42L)).thenThrow(exception);

        final IntegrationException thrown =
                assertThrowsExactly(IntegrationException.class, () -> issuesService.getIssueDetail(42L));

        assertThat(thrown).isSameAs(exception);
        verify(issuesPort).getIssueDetail(42L);
        verify(issuesPort).getLabelEvents(42L);
    }
}
