package com.gitlabflow.floworchestrator.integration.gitlab.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import com.gitlabflow.floworchestrator.integration.gitlab.GitlabIssuesClient;
import com.gitlabflow.floworchestrator.integration.gitlab.models.dto.GitLabIssueResponseDTO;
import com.gitlabflow.floworchestrator.orchestration.issues.models.ListProjectIssuesQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.models.ListProjectIssuesResult;

@ExtendWith(MockitoExtension.class)
@DisplayName("GitlabIssuesAdapter")
class GitlabIssuesAdapterTest {

    @Mock
    private GitlabIssuesClient gitlabIssuesClient;

    @InjectMocks
    private GitlabIssuesAdapter adapter;

    @Test
    @DisplayName("given query when listing project issues then maps paging params and issue fields")
    void givenQueryWhenListingProjectIssuesThenMapsPagingParamsAndIssueFields() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-next-page", "3");
        headers.add("x-prev-page", "1");
        headers.add("x-total", "44");
        headers.add("x-total-pages", "9");

        List<GitLabIssueResponseDTO> body = List.of(new GitLabIssueResponseDTO(
                900L,
                9L,
                101L,
                "Issue title",
                "Issue description",
                "opened",
                List.of(),
                null,
                List.of(),
                "https://gitlab.example.com/issue/9",
                OffsetDateTime.parse("2026-03-28T10:15:30Z"),
                OffsetDateTime.parse("2026-03-29T11:00:00Z"),
                null,
                null
        ));

        when(gitlabIssuesClient.listProjectIssues("group%2Fproject", 2, 5))
                .thenReturn(ResponseEntity.ok().headers(headers).body(body));

        ListProjectIssuesResult result = adapter.listProjectIssues(new ListProjectIssuesQuery("group/project", 2, 5));

        verify(gitlabIssuesClient).listProjectIssues("group%2Fproject", 2, 5);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).id()).isEqualTo(900L);
        assertThat(result.items().get(0).iid()).isEqualTo(9L);
        assertThat(result.items().get(0).title()).isEqualTo("Issue title");
        assertThat(result.pagination().currentPage()).isEqualTo(2);
        assertThat(result.pagination().pageSize()).isEqualTo(5);
        assertThat(result.pagination().nextPage()).isEqualTo(3);
        assertThat(result.pagination().previousPage()).isEqualTo(1);
        assertThat(result.pagination().totalItems()).isEqualTo(44L);
        assertThat(result.pagination().totalPages()).isEqualTo(9L);
    }

    @Test
    @DisplayName("given missing pagination totals when listing then returns nullable totals")
    void givenMissingPaginationTotalsWhenListingThenReturnsNullableTotals() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-next-page", "");
        headers.add("x-prev-page", "");

        when(gitlabIssuesClient.listProjectIssues("123", 1, 20))
                .thenReturn(ResponseEntity.ok().headers(headers).body(List.of()));

        ListProjectIssuesResult result = adapter.listProjectIssues(new ListProjectIssuesQuery("123", 1, 20));

        assertThat(result.pagination().nextPage()).isNull();
        assertThat(result.pagination().previousPage()).isNull();
        assertThat(result.pagination().totalItems()).isNull();
        assertThat(result.pagination().totalPages()).isNull();
    }
}
