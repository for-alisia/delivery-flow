package com.gitlabflow.floworchestrator.integration.gitlab.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gitlabflow.floworchestrator.integration.gitlab.models.dto.GitLabIssueResponseDTO;
import com.gitlabflow.floworchestrator.integration.gitlab.models.dto.GitLabMilestoneDTO;
import com.gitlabflow.floworchestrator.integration.gitlab.models.dto.GitLabUserDTO;
import com.gitlabflow.floworchestrator.orchestration.issues.models.GetIssuesRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.models.IssueSummary;

@DisplayName("GitlabIssuesMapper")
class GitlabIssuesMappingTest {

    private final GitlabIssuesMapper mapper = new GitlabIssuesMapper();

    @Test
    @DisplayName("given full dto when map to issue summary then all fields are mapped")
    void givenFullDtoWhenMapToIssueSummaryThenAllFieldsAreMapped() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-27T10:00:00Z");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-03-27T11:00:00Z");
        OffsetDateTime closedAt = OffsetDateTime.parse("2026-03-27T12:00:00Z");

        GitLabIssueResponseDTO dto = new GitLabIssueResponseDTO(
                100L,
                10L,
                1L,
                "Issue title",
                "Issue description",
                "opened",
                List.of("bug", "backend"),
                new GitLabUserDTO(7L, "author-user"),
                List.of(new GitLabUserDTO(8L, "assignee-1"), new GitLabUserDTO(9L, "assignee-2")),
                "https://gitlab.example.com/group/project/-/issues/10",
                createdAt,
                updatedAt,
                closedAt,
                new GitLabMilestoneDTO(11L, "Sprint 42")
        );

        IssueSummary summary = mapper.toIssueSummary(dto);

        assertThat(summary.id()).isEqualTo(100L);
        assertThat(summary.iid()).isEqualTo(10L);
        assertThat(summary.projectId()).isEqualTo(1L);
        assertThat(summary.title()).isEqualTo("Issue title");
        assertThat(summary.description()).isEqualTo("Issue description");
        assertThat(summary.state()).isEqualTo("opened");
        assertThat(summary.labels()).containsExactly("bug", "backend");
        assertThat(summary.authorUsername()).isEqualTo("author-user");
        assertThat(summary.assigneeUsernames()).containsExactly("assignee-1", "assignee-2");
        assertThat(summary.webUrl()).isEqualTo("https://gitlab.example.com/group/project/-/issues/10");
        assertThat(summary.createdAt()).isEqualTo(createdAt);
        assertThat(summary.updatedAt()).isEqualTo(updatedAt);
        assertThat(summary.closedAt()).isEqualTo(closedAt);
        assertThat(summary.milestone()).isEqualTo("Sprint 42");
    }

    @Test
    @DisplayName("given null assignees labels author milestone and closedAt when map then handles nulls")
    void givenNullAssigneesLabelsAuthorMilestoneAndClosedAtWhenMapThenHandlesNulls() {
        GitLabIssueResponseDTO dto = new GitLabIssueResponseDTO(
                1L,
                2L,
                3L,
                "Title",
                null,
                "closed",
                null,
                null,
                null,
                "url",
                OffsetDateTime.parse("2026-03-27T10:00:00Z"),
                OffsetDateTime.parse("2026-03-27T11:00:00Z"),
                null,
                null
        );

        IssueSummary summary = mapper.toIssueSummary(dto);

        assertThat(summary.labels()).isEmpty();
        assertThat(summary.assigneeUsernames()).isEmpty();
        assertThat(summary.authorUsername()).isNull();
        assertThat(summary.milestone()).isNull();
        assertThat(summary.closedAt()).isNull();
    }

    @Test
    @DisplayName("given all request fields when build query params then map contains all snake case keys")
    void givenAllRequestFieldsWhenBuildQueryParamsThenMapContainsAllSnakeCaseKeys() {
        GetIssuesRequest request = new GetIssuesRequest(
                1L,
                2L,
                "v1",
                "opened",
                "search",
                "bug",
                "created_at",
                "asc",
                2,
                20
        );

        Map<String, Object> queryParams = mapper.buildQueryParams(request);

        assertThat(queryParams)
                .containsEntry("assignee_id", 1L)
                .containsEntry("author_id", 2L)
                .containsEntry("milestone", "v1")
                .containsEntry("state", "opened")
                .containsEntry("search", "search")
                .containsEntry("labels", "bug")
                .containsEntry("order_by", "created_at")
                .containsEntry("sort", "asc")
                .containsEntry("page", 2)
                .containsEntry("per_page", 20);
    }

    @Test
    @DisplayName("given all null request fields when build query params then returns empty map")
    void givenAllNullRequestFieldsWhenBuildQueryParamsThenReturnsEmptyMap() {
        GetIssuesRequest request = new GetIssuesRequest(null, null, null, null, null, null, null, null, null, null);

        Map<String, Object> queryParams = mapper.buildQueryParams(request);

        assertThat(queryParams).isEmpty();
    }

    @Test
    @DisplayName("given mixed request fields when build query params then includes only non null values")
    void givenMixedRequestFieldsWhenBuildQueryParamsThenIncludesOnlyNonNullValues() {
        GetIssuesRequest request = new GetIssuesRequest(null, 9L, null, "all", null, null, null, "desc", null, 100);

        Map<String, Object> queryParams = mapper.buildQueryParams(request);

        assertThat(queryParams)
                .containsOnlyKeys("author_id", "state", "sort", "per_page")
                .containsEntry("author_id", 9L)
                .containsEntry("state", "all")
                .containsEntry("sort", "desc")
                .containsEntry("per_page", 100);
    }
}
