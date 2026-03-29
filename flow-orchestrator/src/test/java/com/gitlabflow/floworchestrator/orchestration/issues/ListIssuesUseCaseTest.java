package com.gitlabflow.floworchestrator.orchestration.issues;

import com.gitlabflow.floworchestrator.common.errors.ValidationException;
import com.gitlabflow.floworchestrator.config.IssuesApiProperties;
import com.gitlabflow.floworchestrator.orchestration.issues.model.ListIssuesQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.model.ListIssuesResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListIssuesUseCaseTest {

    @Mock
    private IssuesProvider issuesProvider;

    private ListIssuesUseCase listIssuesUseCase;

    @BeforeEach
    void setUp() {
        final var issuesApiProperties = new IssuesApiProperties(40, 100);
        listIssuesUseCase = new ListIssuesUseCase(issuesProvider, issuesApiProperties);
    }

    @Test
    @DisplayName("applies default page and configured default page size when omitted")
    void appliesDefaultPageAndConfiguredDefaultPageSizeWhenOmitted() {
        when(issuesProvider.listIssues(new ListIssuesQuery(null, null, 1, 40)))
                .thenReturn(new ListIssuesResult(List.of(), 1, 40));

        final var result = listIssuesUseCase.listIssues(null, null, null, null);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(40);

        final var queryCaptor = ArgumentCaptor.forClass(ListIssuesQuery.class);
        verify(issuesProvider).listIssues(queryCaptor.capture());
        assertThat(queryCaptor.getValue()).isEqualTo(new ListIssuesQuery(null, null, 1, 40));
    }

    @Test
    @DisplayName("rejects page size larger than configured maximum")
    void rejectsPageSizeLargerThanConfiguredMaximum() {
        assertThatThrownBy(() -> listIssuesUseCase.listIssues(null, null, 1, 101))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Invalid payload");
    }

    @Test
    @DisplayName("returns empty list and echoes resolved pagination")
    void returnsEmptyListAndEchoesResolvedPagination() {
        when(issuesProvider.listIssues(new ListIssuesQuery("bug", "alice", 2, 20)))
                .thenReturn(new ListIssuesResult(List.of(), 2, 20));

        final var result = listIssuesUseCase.listIssues(List.of("bug"), "alice", 2, 20);

        assertThat(result.issues()).isEmpty();
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.pageSize()).isEqualTo(20);
    }
}