package com.gitlabflow.floworchestrator.integration.gitlab;

import com.gitlabflow.floworchestrator.common.errors.ErrorCode;
import com.gitlabflow.floworchestrator.common.errors.IntegrationException;
import com.gitlabflow.floworchestrator.config.GitLabProjectCoordinates;
import com.gitlabflow.floworchestrator.integration.gitlab.dto.GitLabIssueResponse;
import com.gitlabflow.floworchestrator.integration.gitlab.dto.GitLabUserResponse;
import com.gitlabflow.floworchestrator.orchestration.issues.model.ListIssuesQuery;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitLabIssuesAdapterTest {

    private static final String PROJECT_ID = "group/project";

    @Mock
    private GitLabIssuesClient gitLabIssuesClient;

    @Test
    @DisplayName("maps single label to gitlab labels query parameter")
    void mapsSingleLabelToGitLabLabelsQueryParameter() {
        final var adapter = new GitLabIssuesAdapter(gitLabIssuesClient, new GitLabProjectCoordinates("https://gitlab.com/api/v4", PROJECT_ID));
        when(gitLabIssuesClient.listIssues(PROJECT_ID, "bug", null, 1, 40)).thenReturn(List.of());

        adapter.listIssues(new ListIssuesQuery("bug", null, 1, 40));

        final var projectIdCaptor = ArgumentCaptor.forClass(String.class);
        final var labelsCaptor = ArgumentCaptor.forClass(String.class);
        verify(gitLabIssuesClient).listIssues(projectIdCaptor.capture(), labelsCaptor.capture(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(1), org.mockito.ArgumentMatchers.eq(40));
        assertThat(projectIdCaptor.getValue()).isEqualTo(PROJECT_ID);
        assertThat(labelsCaptor.getValue()).isEqualTo("bug");
    }

    @Test
    @DisplayName("maps assignee to single-value assignee username array for gitlab")
    void mapsAssigneeToSingleValueAssigneeArrayForGitLab() {
        final var adapter = new GitLabIssuesAdapter(gitLabIssuesClient, new GitLabProjectCoordinates("https://gitlab.com/api/v4", PROJECT_ID));
        when(gitLabIssuesClient.listIssues(PROJECT_ID, null, List.of("alice"), 2, 20)).thenReturn(List.of());

        adapter.listIssues(new ListIssuesQuery(null, "alice", 2, 20));

        final var assigneeCaptor = ArgumentCaptor.forClass(List.class);
        verify(gitLabIssuesClient).listIssues(org.mockito.ArgumentMatchers.eq(PROJECT_ID), org.mockito.ArgumentMatchers.isNull(), assigneeCaptor.capture(), org.mockito.ArgumentMatchers.eq(2), org.mockito.ArgumentMatchers.eq(20));
        assertThat(assigneeCaptor.getValue()).containsExactly("alice");
    }

    @Test
    @DisplayName("maps gitlab assignees array to provider-agnostic assignee summaries")
    void mapsGitLabAssigneesArrayToProviderAgnosticAssigneeSummaries() {
        final var adapter = new GitLabIssuesAdapter(gitLabIssuesClient, new GitLabProjectCoordinates("https://gitlab.com/api/v4", PROJECT_ID));
        final var response = new GitLabIssueResponse(
                12L,
                "Issue title",
                "opened",
                List.of("bug"),
                List.of(new GitLabUserResponse("alice", "Alice", "https://gitlab.com/alice")),
                "https://gitlab.com/group/project/-/issues/12",
                OffsetDateTime.parse("2026-03-29T12:00:00Z"),
                OffsetDateTime.parse("2026-03-29T13:00:00Z")
        );
        when(gitLabIssuesClient.listIssues(PROJECT_ID, null, null, 1, 40)).thenReturn(List.of(response));

        final var result = adapter.listIssues(new ListIssuesQuery(null, null, 1, 40));

        assertThat(result.issues()).hasSize(1);
        assertThat(result.issues().getFirst().assignees()).hasSize(1);
        assertThat(result.issues().getFirst().assignees().getFirst().username()).isEqualTo("alice");
        assertThat(result.issues().getFirst().assignees().getFirst().name()).isEqualTo("Alice");
        assertThat(result.issues().getFirst().assignees().getFirst().webUrl()).isEqualTo("https://gitlab.com/alice");
    }

    @Test
    @DisplayName("translates authentication failures to typed integration exception")
    void translatesAuthenticationFailuresToTypedIntegrationException() {
        final var adapter = new GitLabIssuesAdapter(gitLabIssuesClient, new GitLabProjectCoordinates("https://gitlab.com/api/v4", PROJECT_ID));
        final var query = new ListIssuesQuery(null, null, 1, 40);
        when(gitLabIssuesClient.listIssues(PROJECT_ID, null, null, 1, 40)).thenThrow(feignException(401));

        final IntegrationException exception = assertThrows(IntegrationException.class, () -> adapter.listIssues(query));

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_AUTHENTICATION_FAILED);
    }

    @Test
    @DisplayName("translates not found failures to typed integration exception")
    void translatesNotFoundFailuresToTypedIntegrationException() {
        final var adapter = new GitLabIssuesAdapter(gitLabIssuesClient, new GitLabProjectCoordinates("https://gitlab.com/api/v4", PROJECT_ID));
        final var query = new ListIssuesQuery(null, null, 1, 40);
        when(gitLabIssuesClient.listIssues(PROJECT_ID, null, null, 1, 40)).thenThrow(feignException(404));

        final IntegrationException exception = assertThrows(IntegrationException.class, () -> adapter.listIssues(query));

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_NOT_FOUND);
    }

    @Test
    @DisplayName("translates rate limit failures to typed integration exception")
    void translatesRateLimitFailuresToTypedIntegrationException() {
        final var adapter = new GitLabIssuesAdapter(gitLabIssuesClient, new GitLabProjectCoordinates("https://gitlab.com/api/v4", PROJECT_ID));
        final var query = new ListIssuesQuery(null, null, 1, 40);
        when(gitLabIssuesClient.listIssues(PROJECT_ID, null, null, 1, 40)).thenThrow(feignException(429));

        final IntegrationException exception = assertThrows(IntegrationException.class, () -> adapter.listIssues(query));

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_RATE_LIMITED);
    }

    @Test
    @DisplayName("translates generic provider failures to integration failure category")
    void translatesGenericProviderFailuresToIntegrationFailureCategory() {
        final var adapter = new GitLabIssuesAdapter(gitLabIssuesClient, new GitLabProjectCoordinates("https://gitlab.com/api/v4", PROJECT_ID));
        final var query = new ListIssuesQuery(null, null, 1, 40);
        when(gitLabIssuesClient.listIssues(PROJECT_ID, null, null, 1, 40)).thenThrow(feignException(500));

        final IntegrationException exception = assertThrows(IntegrationException.class, () -> adapter.listIssues(query));

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_FAILURE);
    }

    private FeignException feignException(final int status) {
        final Request request = Request.create(Request.HttpMethod.GET, "/projects/group%2Fproject/issues", Map.of(), null, StandardCharsets.UTF_8, null);
        final Response response = Response.builder()
                .status(status)
                .reason("status-" + status)
                .request(request)
                .headers(Map.of())
                .body("{}", StandardCharsets.UTF_8)
                .build();
        return FeignException.errorStatus("GitLabIssuesClient#listIssues", response);
    }
}
