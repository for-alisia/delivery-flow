package com.gitlabflow.floworchestrator.orchestration.issues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gitlabflow.floworchestrator.orchestration.issues.models.IssueSummary;
import com.gitlabflow.floworchestrator.orchestration.issues.models.ListProjectIssuesQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.models.ListProjectIssuesResult;
import com.gitlabflow.floworchestrator.orchestration.issues.models.PaginationMetadata;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListProjectIssuesUseCase")
class ListProjectIssuesUseCaseTest {

    @Mock
    private IssuesProvider issuesProvider;

    @Mock
    private ListProjectIssuesRequestValidator requestValidator;

    @InjectMocks
    private ListProjectIssuesUseCase useCase;

    @Test
    @DisplayName("given omitted paging when listing then defaults to first page and default page size")
    void givenOmittedPagingWhenListingThenDefaultsToFirstPageAndDefaultPageSize() {
        ListProjectIssuesResult providerResult = sampleResult(1, 20, null, 2, null, null);
        when(issuesProvider.listProjectIssues(org.mockito.ArgumentMatchers.any())).thenReturn(providerResult);

        ListProjectIssuesResult result = useCase.listProjectIssues("123", null, null);

        ArgumentCaptor<ListProjectIssuesQuery> queryCaptor = ArgumentCaptor.forClass(ListProjectIssuesQuery.class);
        verify(requestValidator).validate(queryCaptor.capture());
        assertThat(queryCaptor.getValue()).isEqualTo(new ListProjectIssuesQuery("123", 1, 20));
        assertThat(result).isEqualTo(providerResult);
    }

    @Test
    @DisplayName("given explicit paging when listing then passes page values through")
    void givenExplicitPagingWhenListingThenPassesPageValuesThrough() {
        ListProjectIssuesResult providerResult = sampleResult(3, 15, 2, 4, 45L, 3L);
        when(issuesProvider.listProjectIssues(org.mockito.ArgumentMatchers.any())).thenReturn(providerResult);

        useCase.listProjectIssues("project", 3, 15);

        ArgumentCaptor<ListProjectIssuesQuery> queryCaptor = ArgumentCaptor.forClass(ListProjectIssuesQuery.class);
        verify(requestValidator).validate(queryCaptor.capture());
        assertThat(queryCaptor.getValue()).isEqualTo(new ListProjectIssuesQuery("project", 3, 15));
    }

    @Test
    @DisplayName("given only page when listing then defaults missing page size")
    void givenOnlyPageWhenListingThenDefaultsMissingPageSize() {
        ListProjectIssuesResult providerResult = sampleResult(2, 20, 1, 3, 40L, 2L);
        when(issuesProvider.listProjectIssues(org.mockito.ArgumentMatchers.any())).thenReturn(providerResult);

        useCase.listProjectIssues("project", 2, null);

        ArgumentCaptor<ListProjectIssuesQuery> queryCaptor = ArgumentCaptor.forClass(ListProjectIssuesQuery.class);
        verify(requestValidator).validate(queryCaptor.capture());
        assertThat(queryCaptor.getValue()).isEqualTo(new ListProjectIssuesQuery("project", 2, 20));
    }

    @Test
    @DisplayName("given only page size when listing then defaults missing page")
    void givenOnlyPageSizeWhenListingThenDefaultsMissingPage() {
        ListProjectIssuesResult providerResult = sampleResult(1, 25, null, 2, 100L, 4L);
        when(issuesProvider.listProjectIssues(org.mockito.ArgumentMatchers.any())).thenReturn(providerResult);

        useCase.listProjectIssues("project", null, 25);

        ArgumentCaptor<ListProjectIssuesQuery> queryCaptor = ArgumentCaptor.forClass(ListProjectIssuesQuery.class);
        verify(requestValidator).validate(queryCaptor.capture());
        assertThat(queryCaptor.getValue()).isEqualTo(new ListProjectIssuesQuery("project", 1, 25));
    }

    @Test
    @DisplayName("given provider result with nullable totals when listing then returns nullable totals")
    void givenProviderResultWithNullableTotalsWhenListingThenReturnsNullableTotals() {
        ListProjectIssuesResult providerResult = sampleResult(1, 20, null, 2, null, null);
        when(issuesProvider.listProjectIssues(org.mockito.ArgumentMatchers.any())).thenReturn(providerResult);

        ListProjectIssuesResult result = useCase.listProjectIssues("project", null, null);

        assertThat(result.pagination().totalItems()).isNull();
        assertThat(result.pagination().totalPages()).isNull();
    }

    private static ListProjectIssuesResult sampleResult(
            int currentPage,
            int pageSize,
            Integer previousPage,
            Integer nextPage,
            Long totalItems,
            Long totalPages
    ) {
        return new ListProjectIssuesResult(
                List.of(new IssueSummary(11L, 7L, "Issue", "opened", "https://gitlab.example.com/issues/7")),
                new PaginationMetadata(currentPage, pageSize, previousPage, nextPage, totalItems, totalPages)
        );
    }
}
