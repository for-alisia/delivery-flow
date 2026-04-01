package com.gitlabflow.floworchestrator.orchestration.issues;

import com.gitlabflow.floworchestrator.common.errors.ValidationException;
import com.gitlabflow.floworchestrator.config.IssuesApiProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetIssuesServiceTest {

    @Mock
    private GetIssuesPort getIssuesPort;

    private GetIssuesService getIssuesService;

    @BeforeEach
    void setUp() {
        getIssuesService = new GetIssuesService(
                getIssuesPort,
                new IssuesApiProperties(40, 100)
        );
    }

    @Test
    @DisplayName("throws validation error when perPage exceeds configured max")
    void throwsValidationWhenPerPageTooLarge() {
        final IssueQuery query = new IssueQuery(1, 101, null, null, null, null);

        assertThatThrownBy(() -> getIssuesService.getIssues(query))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Request validation failed");
    }

    @Test
    @DisplayName("delegates to port for valid query")
    void delegatesToPortForValidQuery() {
        final IssueQuery query = new IssueQuery(1, 40, null, null, null, null);
        final IssuePage page = new IssuePage(List.of(), 0, 1);
        when(getIssuesPort.getIssues(query)).thenReturn(page);

        final IssuePage response = getIssuesService.getIssues(query);

        assertThat(response).isEqualTo(page);
        verify(getIssuesPort).getIssues(query);
    }

    @Test
    @DisplayName("accepts perPage equal to configured max")
    void acceptsPerPageEqualToConfiguredMax() {
        final IssueQuery query = new IssueQuery(1, 100, null, null, null, null);
        final IssuePage page = new IssuePage(List.of(), 0, 1);
        when(getIssuesPort.getIssues(query)).thenReturn(page);

        final IssuePage response = getIssuesService.getIssues(query);

        assertThat(response).isEqualTo(page);
        verify(getIssuesPort).getIssues(query);
    }

    @Test
    @DisplayName("delegates query with populated filters")
    void delegatesQueryWithPopulatedFilters() {
        final IssueQuery query = new IssueQuery(2, 25, IssueState.OPENED, "bug", "jane", "M1");
        final IssuePage page = new IssuePage(List.of(), 0, 2);
        when(getIssuesPort.getIssues(query)).thenReturn(page);

        final IssuePage response = getIssuesService.getIssues(query);

        assertThat(response).isEqualTo(page);
        verify(getIssuesPort).getIssues(query);
    }
}
