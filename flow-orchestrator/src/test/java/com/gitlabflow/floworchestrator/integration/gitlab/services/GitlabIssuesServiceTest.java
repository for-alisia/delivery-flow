package com.gitlabflow.floworchestrator.integration.gitlab.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gitlabflow.floworchestrator.common.errors.ErrorCode;
import com.gitlabflow.floworchestrator.common.errors.IntegrationException;
import com.gitlabflow.floworchestrator.integration.gitlab.GitlabIssuesClient;
import com.gitlabflow.floworchestrator.integration.gitlab.GitlabProperties;
import com.gitlabflow.floworchestrator.integration.gitlab.models.dto.GitLabIssueResponseDTO;
import com.gitlabflow.floworchestrator.integration.gitlab.models.dto.GitLabMilestoneDTO;
import com.gitlabflow.floworchestrator.integration.gitlab.models.dto.GitLabUserDTO;
import com.gitlabflow.floworchestrator.orchestration.issues.models.GetIssuesRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.models.IssueSummary;

@ExtendWith(MockitoExtension.class)
@DisplayName("GitlabIssuesService")
class GitlabIssuesServiceTest {

    @Mock
    private GitlabIssuesClient client;

    @Spy
    private GitlabIssuesMapper mapper;

    private GitlabIssuesService service;

    private GitlabProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GitlabProperties("https://gitlab.com/group/project", "test-token");
        service = new GitlabIssuesService(client, mapper, properties);
        assertThat(properties.getEncodedProjectPath()).isEqualTo("group%2Fproject");
    }

    @Test
    @DisplayName("given gitlab response when fetch issues then maps and returns summaries")
    void givenGitlabResponseWhenFetchIssuesThenMapsAndReturnsSummaries() {
        GetIssuesRequest request = new GetIssuesRequest(null, null, null, "opened", null, null, null, null, null, null);
        Map<String, Object> queryParams = Map.of("state", "opened");
        GitLabIssueResponseDTO dto1 = new GitLabIssueResponseDTO(
            101L,
            11L,
            1001L,
            "Issue one",
            "First issue",
            "opened",
            List.of("bug"),
            new GitLabUserDTO(1L, "alice"),
            List.of(new GitLabUserDTO(2L, "bob")),
            "https://gitlab.com/group/project/-/issues/11",
            OffsetDateTime.parse("2026-03-26T10:15:30Z"),
            OffsetDateTime.parse("2026-03-26T11:15:30Z"),
            null,
            new GitLabMilestoneDTO(9L, "Sprint 9")
        );
        GitLabIssueResponseDTO dto2 = new GitLabIssueResponseDTO(
            102L,
            12L,
            1001L,
            "Issue two",
            "Second issue",
            "opened",
            List.of("feature"),
            new GitLabUserDTO(3L, "charlie"),
            List.of(),
            "https://gitlab.com/group/project/-/issues/12",
            OffsetDateTime.parse("2026-03-25T09:00:00Z"),
            OffsetDateTime.parse("2026-03-25T09:30:00Z"),
            null,
            new GitLabMilestoneDTO(10L, "Sprint 10")
        );
        IssueSummary expected1 = new IssueSummary(
            1001L,
            101L,
            11L,
            "Issue one",
            "First issue",
            "opened",
            List.of("bug"),
            "alice",
            List.of("bob"),
            "https://gitlab.com/group/project/-/issues/11",
            OffsetDateTime.parse("2026-03-26T10:15:30Z"),
            OffsetDateTime.parse("2026-03-26T11:15:30Z"),
            null,
            "Sprint 9"
        );
        IssueSummary expected2 = new IssueSummary(
            1001L,
            102L,
            12L,
            "Issue two",
            "Second issue",
            "opened",
            List.of("feature"),
            "charlie",
            List.of(),
            "https://gitlab.com/group/project/-/issues/12",
            OffsetDateTime.parse("2026-03-25T09:00:00Z"),
            OffsetDateTime.parse("2026-03-25T09:30:00Z"),
            null,
            "Sprint 10"
        );

        when(mapper.buildQueryParams(request)).thenReturn(queryParams);
        when(client.getIssues(queryParams)).thenReturn(List.of(dto1, dto2));

        List<IssueSummary> result = service.fetchIssues(request);

        assertThat(result).containsExactly(expected1, expected2);

        verify(mapper).buildQueryParams(request);
        verify(client).getIssues(queryParams);
        verify(mapper).toIssueSummary(dto1);
        verify(mapper).toIssueSummary(dto2);
    }

    @Test
    @DisplayName("given empty gitlab response when fetch issues then returns empty list")
    void givenEmptyGitlabResponseWhenFetchIssuesThenReturnsEmptyList() {
        GetIssuesRequest request = new GetIssuesRequest(null, null, null, null, null, null, null, null, null, null);
        Map<String, Object> queryParams = Map.of();

        when(mapper.buildQueryParams(request)).thenReturn(queryParams);
        when(client.getIssues(queryParams)).thenReturn(List.of());

        List<IssueSummary> result = service.fetchIssues(request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("given integration exception when fetch issues then rethrows same exception")
    void givenIntegrationExceptionWhenFetchIssuesThenRethrowsSameException() {
        GetIssuesRequest request = new GetIssuesRequest(null, null, null, null, null, null, null, null, null, null);
        Map<String, Object> queryParams = Map.of();
        IntegrationException expected = new IntegrationException(
                ErrorCode.INTEGRATION_UNAUTHORIZED,
                "gitlab",
                401,
                "unauthorized",
                null,
                null
        );

        when(mapper.buildQueryParams(request)).thenReturn(queryParams);
        when(client.getIssues(queryParams)).thenThrow(expected);

        assertThatThrownBy(() -> service.fetchIssues(request))
                .isSameAs(expected);
    }

    @Test
    @DisplayName("given runtime exception when fetch issues then wraps as integration unavailable")
    void givenRuntimeExceptionWhenFetchIssuesThenWrapsAsIntegrationUnavailable() {
        GetIssuesRequest request = new GetIssuesRequest(null, null, null, null, null, null, null, null, null, null);
        Map<String, Object> queryParams = Map.of();

        when(mapper.buildQueryParams(request)).thenReturn(queryParams);
        when(client.getIssues(queryParams)).thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> service.fetchIssues(request))
                .isInstanceOf(IntegrationException.class)
                .satisfies(throwable -> {
                    IntegrationException exception = (IntegrationException) throwable;
                    assertThat(exception.getCode()).isEqualTo(ErrorCode.INTEGRATION_UNAVAILABLE);
                    assertThat(exception.getSource()).isEqualTo("gitlab");
                    assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
                });
    }
}
