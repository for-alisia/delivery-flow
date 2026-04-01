package com.gitlabflow.floworchestrator.integration.gitlab.issues;

import com.gitlabflow.floworchestrator.common.errors.ErrorCode;
import com.gitlabflow.floworchestrator.common.errors.IntegrationException;
import com.gitlabflow.floworchestrator.config.GitLabProperties;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabExceptionMapper;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabProjectLocator;
import com.gitlabflow.floworchestrator.orchestration.issues.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.IssueQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.IssueState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class GitLabIssuesAdapterTest {

    private static final String PROJECT_URL = "https://gitlab.example.com/group/project";
    private static final String API_URL = "https://gitlab.example.com/api/v4/projects/group%2Fproject/issues";
    private static final IssueQuery DEFAULT_QUERY = new IssueQuery(1, 40, null, null, null, null);

    private MockRestServiceServer server;
    private GitLabIssuesAdapter adapter;

    private static GitLabIssuesAdapter createAdapter(final RestClient.Builder builder, final GitLabProjectLocator locator) {
        return new GitLabIssuesAdapter(
                builder.baseUrl(Objects.requireNonNull(locator.projectReference().apiBaseUrl())).build(),
                locator,
                new GitLabIssueMapper(),
                new GitLabExceptionMapper()
        );
    }

    @BeforeEach
    void setUp() {
        final RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        final GitLabProjectLocator locator = new GitLabProjectLocator(new GitLabProperties(PROJECT_URL, "redacted"));
        adapter = createAdapter(builder, locator);
    }

    @Test
    @DisplayName("sends expected GitLab query parameters")
    void sendsExpectedQueryParameters() {
        server.expect(requestTo(API_URL + "?page=2&per_page=20&state=opened&labels=bug&assignee_username=jane&milestone=M1"))
                .andExpect(method(Objects.requireNonNull(HttpMethod.GET)))
                .andExpect(queryParam("page", "2"))
                .andExpect(queryParam("per_page", "20"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.OK)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .body("[]"));

        final var result = adapter.getIssues(new IssueQuery(2, 20, IssueState.OPENED, "bug", "jane", "M1"));

        assertThat(result.count()).isZero();
        assertThat(result.page()).isEqualTo(2);
        server.verify();
    }

    @Test
    @DisplayName("maps 401 to authentication integration error")
    void maps401ToAuthenticationIntegrationError() {
        assertStatusIsMappedToError(HttpStatus.UNAUTHORIZED, ErrorCode.INTEGRATION_AUTHENTICATION_FAILED);
    }

    @Test
    @DisplayName("maps 404 to not found integration error")
    void maps404ToNotFoundIntegrationError() {
        assertStatusIsMappedToError(HttpStatus.NOT_FOUND, ErrorCode.INTEGRATION_NOT_FOUND);
    }

    @Test
    @DisplayName("maps 429 to rate limited integration error")
    void maps429ToRateLimitedIntegrationError() {
        assertStatusIsMappedToError(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.INTEGRATION_RATE_LIMITED);
    }

    @Test
    @DisplayName("maps 500 to generic integration error")
    void maps500ToGenericIntegrationError() {
        assertStatusIsMappedToError(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTEGRATION_FAILURE);
    }

    @Test
    @DisplayName("maps transport failures to generic integration error")
    void mapsTransportFailuresToGenericIntegrationError() {
        server.expect(requestTo(API_URL + "?page=1&per_page=40"))
                .andRespond(request -> {
                    throw new IOException("connection reset");
                });

        assertThatThrownBy(() -> adapter.getIssues(DEFAULT_QUERY))
                .isInstanceOfSatisfying(IntegrationException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_FAILURE));
    }

    @Test
    @DisplayName("returns empty page when gitlab response body is null")
    void returnsEmptyPageWhenGitLabResponseBodyIsNull() {
        server.expect(requestTo(API_URL + "?page=1&per_page=40"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("null"));

        final IssuePage result = adapter.getIssues(DEFAULT_QUERY);

        assertThat(result.items()).isEmpty();
        assertThat(result.count()).isZero();
        assertThat(result.page()).isEqualTo(1);
    }

    @Test
    @DisplayName("maps multiple gitlab issues into issue page")
    void mapsMultipleGitLabIssuesIntoIssuePage() {
        server.expect(requestTo(API_URL + "?page=1&per_page=40"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                [
                                  {
                                    "id": 101,
                                    "title": "First",
                                    "description": "D1",
                                    "state": "opened",
                                    "labels": ["bug"],
                                    "assignees": [{"username": "alice"}],
                                    "milestone": {"title": "M1"},
                                    "epic": {"id": 77}
                                  },
                                  {
                                    "id": 102,
                                    "title": "Second",
                                    "description": "D2",
                                    "state": "closed",
                                    "labels": ["infra"],
                                    "assignee": {"username": "bob"}
                                  }
                                ]
                                """));

        final IssuePage result = adapter.getIssues(DEFAULT_QUERY);

        assertThat(result.count()).isEqualTo(2);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.items()).extracting("id").containsExactly(101L, 102L);
        assertThat(result.items().getFirst().assignee()).isEqualTo("alice");
        assertThat(result.items().get(1).assignee()).isEqualTo("bob");
    }

    private void assertStatusIsMappedToError(final HttpStatus status, final ErrorCode expectedErrorCode) {
        server.expect(requestTo(API_URL + "?page=1&per_page=40"))
                .andRespond(withStatus(status));

        assertThatThrownBy(() -> adapter.getIssues(DEFAULT_QUERY))
                .isInstanceOfSatisfying(IntegrationException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(expectedErrorCode));
    }
}
