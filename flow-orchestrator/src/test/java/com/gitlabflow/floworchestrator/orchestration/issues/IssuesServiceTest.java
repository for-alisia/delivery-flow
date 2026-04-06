package com.gitlabflow.floworchestrator.orchestration.issues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gitlabflow.floworchestrator.common.error.ErrorCode;
import com.gitlabflow.floworchestrator.common.error.IntegrationException;
import com.gitlabflow.floworchestrator.common.error.ValidationException;
import com.gitlabflow.floworchestrator.config.IssuesApiProperties;
import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.Issue;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueState;
import java.util.List;
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

    private IssuesService issuesService;

    @BeforeEach
    void setUp() {
        issuesService = new IssuesService(issuesPort, new IssuesApiProperties(40, 100));
    }

    @Test
    @DisplayName("throws validation error when perPage exceeds configured max")
    void throwsValidationWhenPerPageTooLarge() {
        final IssueQuery query = new IssueQuery(1, 101, null, null, null, null);

        assertThatThrownBy(() -> issuesService.getIssues(query))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Request validation failed");
    }

    @Test
    @DisplayName("delegates to port for valid query")
    void delegatesToPortForValidQuery() {
        final IssueQuery query = new IssueQuery(1, 40, null, null, null, null);
        final IssuePage page = new IssuePage(List.of(), 0, 1);
        when(issuesPort.getIssues(query)).thenReturn(page);

        final IssuePage response = issuesService.getIssues(query);

        assertThat(response).isEqualTo(page);
        verify(issuesPort).getIssues(query);
    }

    @Test
    @DisplayName("accepts perPage equal to configured max")
    void acceptsPerPageEqualToConfiguredMax() {
        final IssueQuery query = new IssueQuery(1, 100, null, null, null, null);
        final IssuePage page = new IssuePage(List.of(), 0, 1);
        when(issuesPort.getIssues(query)).thenReturn(page);

        final IssuePage response = issuesService.getIssues(query);

        assertThat(response).isEqualTo(page);
        verify(issuesPort).getIssues(query);
    }

    @Test
    @DisplayName("delegates query with populated filters")
    void delegatesQueryWithPopulatedFilters() {
        final IssueQuery query = new IssueQuery(2, 25, IssueState.OPENED, "bug", "jane", "M1");
        final IssuePage page = new IssuePage(List.of(), 0, 2);
        when(issuesPort.getIssues(query)).thenReturn(page);

        final IssuePage response = issuesService.getIssues(query);

        assertThat(response).isEqualTo(page);
        verify(issuesPort).getIssues(query);
    }

    @Test
    @DisplayName("delegates create input to port and returns issue")
    void delegatesCreateInputToPortAndReturnsIssue() {
        final CreateIssueInput input =
                new CreateIssueInput("Deploy failure", "Step 3 failed", List.of("bug", "deploy"));
        final Issue issue = new Issue(
                84L, 10L, "Deploy failure", "Step 3 failed", "opened", List.of("bug", "deploy"), null, null, null);
        when(issuesPort.createIssue(input)).thenReturn(issue);

        final Issue response = issuesService.createIssue(input);

        assertThat(response).isEqualTo(issue);
        verify(issuesPort).createIssue(input);
    }

    @Test
    @DisplayName("returns null description unchanged when port returns null description")
    void returnsNullDescriptionUnchanged() {
        final CreateIssueInput input = new CreateIssueInput("Reporting bug", null, List.of());
        final Issue issue = new Issue(85L, 11L, "Reporting bug", null, "opened", List.of(), null, null, null);
        when(issuesPort.createIssue(input)).thenReturn(issue);

        final Issue response = issuesService.createIssue(input);

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
}
