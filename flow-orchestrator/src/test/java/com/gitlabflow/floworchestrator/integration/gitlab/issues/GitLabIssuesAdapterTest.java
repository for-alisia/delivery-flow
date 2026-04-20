package com.gitlabflow.floworchestrator.integration.gitlab.issues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.gitlabflow.floworchestrator.common.error.ErrorCode;
import com.gitlabflow.floworchestrator.common.error.IntegrationException;
import com.gitlabflow.floworchestrator.config.GitLabProperties;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabExceptionMapper;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabProjectLocator;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabUriFactory;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.mapper.GitLabIssueDetailMapper;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.mapper.GitLabIssuesMapper;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.mapper.GitLabLabelEventMapper;
import com.gitlabflow.floworchestrator.orchestration.common.model.ChangeField;
import com.gitlabflow.floworchestrator.orchestration.common.model.ChangeSet;
import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueState;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueSummary;
import com.gitlabflow.floworchestrator.orchestration.issues.model.LabelChangeSet;
import com.gitlabflow.floworchestrator.orchestration.issues.model.UpdateIssueInput;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

class GitLabIssuesAdapterTest {

    private static final String PROJECT_URL = "https://gitlab.example.com/group/project";
    private static final String API_URL = "https://gitlab.example.com/api/v4/projects/group%2Fproject/issues";
    private static final String DELETE_API_URL = API_URL + "/12";
    private static final String UPDATE_API_URL = API_URL + "/12";
    private static final String GET_SINGLE_API_URL = API_URL + "/42";
    private static final String GET_LABEL_EVENTS_API_URL = GET_SINGLE_API_URL + "/resource_label_events";
    private static final @NonNull MediaType APPLICATION_JSON = Objects.requireNonNull(MediaType.APPLICATION_JSON);
    private static final IssueQuery DEFAULT_QUERY = new IssueQuery(1, 40, null, null, null, null, List.of());
    private static final CreateIssueInput CREATE_INPUT =
            new CreateIssueInput("Deploy failure", "Step 3 failed", List.of("bug", "deploy"));
    private static final UpdateIssueInput UPDATE_INPUT =
            new UpdateIssueInput(12L, "Deploy failure updated", "", List.of("backend", "triaged"), List.of("bug"));

    private MockRestServiceServer server;
    private GitLabIssuesAdapter adapter;

    private static GitLabIssuesAdapter createAdapter(
            final RestClient.Builder builder, final GitLabProjectLocator locator) {
        return new GitLabIssuesAdapter(
                builder.baseUrl(Objects.requireNonNull(
                                locator.projectReference().apiBaseUrl()))
                        .build(),
                new GitLabUriFactory(locator),
                new GitLabIssuesMapper(),
                new GitLabIssueDetailMapper(),
                new GitLabLabelEventMapper(),
                new GitLabExceptionMapper());
    }

    @BeforeEach
    void setUp() {
        final RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        final GitLabProjectLocator locator =
                new GitLabProjectLocator(new GitLabProperties(PROJECT_URL, "redacted", 5, 30));
        adapter = createAdapter(builder, locator);
    }

    @Test
    @DisplayName("sends expected GitLab query parameters")
    void sendsExpectedQueryParameters() {
        server.expect(requestTo(
                        API_URL + "?page=2&per_page=20&state=opened&labels=bug&assignee_username=jane&milestone=M1"))
                .andExpect(method(Objects.requireNonNull(HttpMethod.GET)))
                .andExpect(queryParam("page", "2"))
                .andExpect(queryParam("per_page", "20"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.OK)
                        .contentType(APPLICATION_JSON)
                        .body("[]"));

        final var result = adapter.getIssues(new IssueQuery(2, 20, IssueState.OPENED, "bug", "jane", "M1", List.of()));

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
        assertStatusIsMappedToError(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND);
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
        server.expect(requestTo(API_URL + "?page=1&per_page=40")).andRespond(request -> {
            throw new IOException("connection reset");
        });

        assertThatThrownBy(() -> adapter.getIssues(DEFAULT_QUERY))
                .isInstanceOfSatisfying(
                        IntegrationException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_FAILURE));
    }

    @Test
    @DisplayName("maps timeout transport failures to generic integration error")
    void mapsTimeoutTransportFailuresToGenericIntegrationError() {
        server.expect(requestTo(API_URL + "?page=1&per_page=40")).andRespond(request -> {
            throw new ResourceAccessException("read timed out", new SocketTimeoutException("Read timed out"));
        });

        assertThatThrownBy(() -> adapter.getIssues(DEFAULT_QUERY))
                .isInstanceOfSatisfying(
                        IntegrationException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_FAILURE));
    }

    @Test
    @DisplayName("returns empty page when gitlab response body is null")
    void returnsEmptyPageWhenGitLabResponseBodyIsNull() {
        server.expect(requestTo(API_URL + "?page=1&per_page=40"))
                .andRespond(
                        withStatus(HttpStatus.OK).contentType(APPLICATION_JSON).body("null"));

        final IssuePage result = adapter.getIssues(DEFAULT_QUERY);

        assertThat(result.items()).isEmpty();
        assertThat(result.count()).isZero();
        assertThat(result.page()).isEqualTo(1);
    }

    @Test
    @DisplayName("maps multiple gitlab issues into issue page")
    void mapsMultipleGitLabIssuesIntoIssuePage() {
        server.expect(requestTo(API_URL + "?page=1&per_page=40"))
                .andRespond(
                        withStatus(HttpStatus.OK).contentType(APPLICATION_JSON).body("""
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

    @Test
    @DisplayName("posts create payload and maps issue response")
    void postsCreatePayloadAndMapsIssueResponse() {
        server.expect(requestTo(API_URL))
                .andExpect(method(Objects.requireNonNull(HttpMethod.POST)))
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "title": "Deploy failure",
                          "description": "Step 3 failed",
                          "labels": "bug,deploy"
                        }
                        """))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(APPLICATION_JSON)
                        .body("""
                                {
                                  "id": 700,
                                  "iid": 12,
                                  "title": "Deploy failure",
                                  "description": "Step 3 failed",
                                  "labels": ["bug", "deploy"],
                                  "state": "opened"
                                }
                                """));

        final IssueSummary issue = adapter.createIssue(CREATE_INPUT);

        assertThat(issue.id()).isEqualTo(700L);
        assertThat(issue.title()).isEqualTo("Deploy failure");
        assertThat(issue.description()).isEqualTo("Step 3 failed");
        assertThat(issue.state()).isEqualTo("opened");
        assertThat(issue.labels()).containsExactly("bug", "deploy");
        server.verify();
    }

    @Test
    @DisplayName("maps create issue 401 to authentication integration error")
    void mapsCreateIssue401ToAuthenticationIntegrationError() {
        assertCreateStatusIsMappedToError(HttpStatus.UNAUTHORIZED, ErrorCode.INTEGRATION_AUTHENTICATION_FAILED);
    }

    @Test
    @DisplayName("maps create issue 404 to not found integration error")
    void mapsCreateIssue404ToNotFoundIntegrationError() {
        assertCreateStatusIsMappedToError(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("maps create issue 429 to rate limited integration error")
    void mapsCreateIssue429ToRateLimitedIntegrationError() {
        assertCreateStatusIsMappedToError(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.INTEGRATION_RATE_LIMITED);
    }

    @Test
    @DisplayName("maps create issue 500 to generic integration error")
    void mapsCreateIssue500ToGenericIntegrationError() {
        assertCreateStatusIsMappedToError(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTEGRATION_FAILURE);
    }

    @Test
    @DisplayName("maps create issue transport failures to generic integration error")
    void mapsCreateIssueTransportFailuresToGenericIntegrationError() {
        server.expect(requestTo(API_URL)).andRespond(request -> {
            throw new IOException("connection reset");
        });

        assertThatThrownBy(() -> adapter.createIssue(CREATE_INPUT))
                .isInstanceOfSatisfying(
                        IntegrationException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_FAILURE));
    }

    @Test
    @DisplayName("puts update payload and maps updated issue response")
    void putsUpdatePayloadAndMapsUpdatedIssueResponse() {
        server.expect(requestTo(UPDATE_API_URL))
                .andExpect(method(Objects.requireNonNull(HttpMethod.PUT)))
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "title": "Deploy failure updated",
                          "description": "",
                          "add_labels": "backend,triaged",
                          "remove_labels": "bug"
                        }
                        """))
                .andRespond(
                        withStatus(HttpStatus.OK).contentType(APPLICATION_JSON).body("""
                                {
                                  "id": 700,
                                  "iid": 12,
                                  "title": "Deploy failure updated",
                                  "description": "",
                                  "labels": ["backend", "triaged"],
                                  "state": "opened"
                                }
                                """));

        final IssueSummary issue = adapter.updateIssue(UPDATE_INPUT);

        assertThat(issue.id()).isEqualTo(700L);
        assertThat(issue.issueId()).isEqualTo(12L);
        assertThat(issue.title()).isEqualTo("Deploy failure updated");
        assertThat(issue.description()).isEmpty();
        assertThat(issue.labels()).containsExactly("backend", "triaged");
        server.verify();
    }

    @Test
    @DisplayName("maps update issue 404 to not found integration error")
    void mapsUpdateIssue404ToNotFoundIntegrationError() {
        assertUpdateStatusIsMappedToError(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("maps update issue 500 to generic integration error")
    void mapsUpdateIssue500ToGenericIntegrationError() {
        assertUpdateStatusIsMappedToError(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTEGRATION_FAILURE);
    }

    @Test
    @DisplayName("maps update issue transport failures to generic integration error")
    void mapsUpdateIssueTransportFailuresToGenericIntegrationError() {
        server.expect(requestTo(UPDATE_API_URL)).andRespond(request -> {
            throw new IOException("connection reset");
        });

        assertThatThrownBy(() -> adapter.updateIssue(UPDATE_INPUT))
                .isInstanceOfSatisfying(
                        IntegrationException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_FAILURE));
    }

    @Test
    @DisplayName("deletes issue with expected endpoint and returns without error")
    void deletesIssueWithExpectedEndpointAndReturnsWithoutError() {
        server.expect(requestTo(DELETE_API_URL))
                .andExpect(method(Objects.requireNonNull(HttpMethod.DELETE)))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        adapter.deleteIssue(12L);

        server.verify();
    }

    @Test
    @DisplayName("maps delete issue 404 to not found integration error")
    void mapsDeleteIssue404ToNotFoundIntegrationError() {
        assertDeleteStatusIsMappedToError(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("maps delete issue 403 to forbidden integration error")
    void mapsDeleteIssue403ToForbiddenIntegrationError() {
        assertDeleteStatusIsMappedToError(HttpStatus.FORBIDDEN, ErrorCode.INTEGRATION_FORBIDDEN);
    }

    @Test
    @DisplayName("maps delete issue transport failures to generic integration error")
    void mapsDeleteIssueTransportFailuresToGenericIntegrationError() {
        server.expect(requestTo(DELETE_API_URL)).andRespond(request -> {
            throw new IOException("connection reset");
        });

        assertThatThrownBy(() -> adapter.deleteIssue(12L))
                .isInstanceOfSatisfying(
                        IntegrationException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_FAILURE));
    }

    private void assertStatusIsMappedToError(final HttpStatus status, final ErrorCode expectedErrorCode) {
        server.expect(requestTo(API_URL + "?page=1&per_page=40"))
                .andRespond(withStatus(Objects.requireNonNull(status)));

        assertThatThrownBy(() -> adapter.getIssues(DEFAULT_QUERY))
                .isInstanceOfSatisfying(
                        IntegrationException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(expectedErrorCode));
    }

    private void assertCreateStatusIsMappedToError(final HttpStatus status, final ErrorCode expectedErrorCode) {
        server.expect(requestTo(API_URL)).andRespond(withStatus(Objects.requireNonNull(status)));

        assertThatThrownBy(() -> adapter.createIssue(CREATE_INPUT))
                .isInstanceOfSatisfying(
                        IntegrationException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(expectedErrorCode));
    }

    private void assertUpdateStatusIsMappedToError(final HttpStatus status, final ErrorCode expectedErrorCode) {
        server.expect(requestTo(UPDATE_API_URL)).andRespond(withStatus(Objects.requireNonNull(status)));

        assertThatThrownBy(() -> adapter.updateIssue(UPDATE_INPUT))
                .isInstanceOfSatisfying(
                        IntegrationException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(expectedErrorCode));
    }

    private void assertDeleteStatusIsMappedToError(final HttpStatus status, final ErrorCode expectedErrorCode) {
        server.expect(requestTo(DELETE_API_URL)).andRespond(withStatus(Objects.requireNonNull(status)));

        assertThatThrownBy(() -> adapter.deleteIssue(12L))
                .isInstanceOfSatisfying(
                        IntegrationException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(expectedErrorCode));
    }

    @Test
    @DisplayName("getIssueDetail calls correct GitLab URI and returns mapped IssueDetail")
    void getIssueDetailCallsCorrectUriAndReturnsMappedIssueDetail() {
        server.expect(requestTo(GET_SINGLE_API_URL))
                .andExpect(method(Objects.requireNonNull(HttpMethod.GET)))
                .andRespond(
                        withStatus(HttpStatus.OK).contentType(APPLICATION_JSON).body("""
                                {
                                  "id": 500,
                                  "iid": 42,
                                  "title": "Fix login bug",
                                  "description": "SSO broken",
                                  "state": "opened",
                                  "labels": ["bug"],
                                  "assignees": [{"id": 10, "username": "john.doe", "name": "John Doe"}],
                                  "milestone": {"id": 5, "iid": 3, "title": "Sprint 12", "state": "active", "due_date": "2026-04-30"},
                                  "created_at": "2026-01-04T15:31:51.081Z",
                                  "updated_at": "2026-03-12T09:00:00.000Z",
                                  "closed_at": null
                                }
                                """));

        final IssueDetail result = adapter.getIssueDetail(42L);

        assertThat(result.issueId()).isEqualTo(42L);
        assertThat(result.title()).isEqualTo("Fix login bug");
        assertThat(result.state()).isEqualTo("opened");
        assertThat(result.assignees()).hasSize(1);
        assertThat(result.assignees().getFirst().username()).isEqualTo("john.doe");
        assertThat(result.milestone()).isNotNull();
        server.verify();
    }

    @Test
    @DisplayName("getIssueDetail null body from GitLab throws IntegrationException")
    void getIssueDetailNullBodyThrowsIntegrationException() {
        server.expect(requestTo(GET_SINGLE_API_URL))
                .andRespond(
                        withStatus(HttpStatus.OK).contentType(APPLICATION_JSON).body("null"));

        assertThatThrownBy(() -> adapter.getIssueDetail(42L))
                .isInstanceOfSatisfying(
                        IntegrationException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_FAILURE));
    }

    @Test
    @DisplayName("getLabelEvents calls correct URI and maps label events")
    void getLabelEventsCallsCorrectUriAndMapsLabelEvents() {
        server.expect(requestTo(GET_LABEL_EVENTS_API_URL))
                .andExpect(method(Objects.requireNonNull(HttpMethod.GET)))
                .andRespond(
                        withStatus(HttpStatus.OK).contentType(APPLICATION_JSON).body("""
                        [
                          {
                            "id": 1001,
                            "user": {"id": 1, "username": "root", "name": "Administrator"},
                            "created_at": "2026-01-15T09:30:00Z",
                            "label": {"id": 73, "name": "bug"},
                            "action": "add"
                          }
                        ]
                        """));

        final List<ChangeSet<?>> result = adapter.getLabelEvents(42L);

        assertThat(result).singleElement().isInstanceOfSatisfying(LabelChangeSet.class, changeSet -> {
            assertThat(changeSet.changeType()).isEqualTo("add");
            assertThat(changeSet.changedBy().id()).isEqualTo(1L);
            assertThat(changeSet.changedBy().username()).isEqualTo("root");
            assertThat(changeSet.changedBy().name()).isEqualTo("Administrator");
            assertThat(changeSet.change().id()).isEqualTo(73L);
            assertThat(changeSet.change().name()).isEqualTo("bug");
            assertThat(changeSet.change().field()).isEqualTo(ChangeField.LABEL);
            assertThat(changeSet.changedAt()).hasToString("2026-01-15T09:30Z");
        });
        server.verify();
    }

    @Test
    @DisplayName("getLabelEvents returns empty list when GitLab returns empty array")
    void getLabelEventsReturnsEmptyListWhenGitLabReturnsEmptyArray() {
        server.expect(requestTo(GET_LABEL_EVENTS_API_URL))
                .andExpect(method(Objects.requireNonNull(HttpMethod.GET)))
                .andRespond(
                        withStatus(HttpStatus.OK).contentType(APPLICATION_JSON).body("[]"));

        final List<ChangeSet<?>> result = adapter.getLabelEvents(42L);

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("getLabelEvents null body from GitLab throws IntegrationException")
    void getLabelEventsNullBodyThrowsIntegrationException() {
        server.expect(requestTo(GET_LABEL_EVENTS_API_URL))
                .andExpect(method(Objects.requireNonNull(HttpMethod.GET)))
                .andRespond(
                        withStatus(HttpStatus.OK).contentType(APPLICATION_JSON).body("null"));

        assertThatThrownBy(() -> adapter.getLabelEvents(42L))
                .isInstanceOfSatisfying(
                        IntegrationException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_FAILURE));
        server.verify();
    }
}
