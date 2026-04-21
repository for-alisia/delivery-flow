package com.gitlabflow.floworchestrator.integration.gitlab.milestones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.gitlabflow.floworchestrator.common.error.ErrorCode;
import com.gitlabflow.floworchestrator.common.error.IntegrationException;
import com.gitlabflow.floworchestrator.config.GitLabProperties;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabExceptionMapper;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabOffsetPaginationLoader;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabOperationExecutor;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabProjectLocator;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabUriFactory;
import com.gitlabflow.floworchestrator.integration.gitlab.milestones.mapper.GitLabMilestonesMapper;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.Milestone;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.MilestoneState;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.SearchMilestonesInput;
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
import org.springframework.web.client.RestClient;

class GitLabMilestonesAdapterTest {

    private static final String PROJECT_URL = "https://gitlab.example.com/group/project";
    private static final String API_URL = "https://gitlab.example.com/api/v4/projects/group%2Fproject/milestones";
    private static final @NonNull MediaType APPLICATION_JSON = Objects.requireNonNull(MediaType.APPLICATION_JSON);

    private MockRestServiceServer server;
    private GitLabMilestonesAdapter adapter;

    @BeforeEach
    void setUp() {
        final RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        final GitLabProjectLocator locator =
                new GitLabProjectLocator(new GitLabProperties(PROJECT_URL, "redacted", 5, 30));
        adapter = createAdapter(builder, locator);
    }

    @Test
    @DisplayName("forwards state search and repeated iids query parameters")
    void forwardsStateSearchAndRepeatedIidsQueryParameters() {
        server.expect(requestTo(API_URL + "?page=1&per_page=100&state=active&search=release&iids%5B%5D=3&iids%5B%5D=7"))
                .andExpect(method(Objects.requireNonNull(HttpMethod.GET)))
                .andExpect(queryParam("page", "1"))
                .andExpect(queryParam("per_page", "100"))
                .andExpect(queryParam("state", "active"))
                .andExpect(queryParam("search", "release"))
                .andRespond(
                        withStatus(HttpStatus.OK).contentType(APPLICATION_JSON).body("""
                                [
                                  {
                                    "id": 401,
                                    "iid": 3,
                                    "title": "Release 1.0",
                                    "description": "Scope",
                                    "start_date": "2026-05-01",
                                    "due_date": "2026-05-15",
                                    "state": "active"
                                  }
                                ]
                                """));

        final List<Milestone> milestones = adapter.searchMilestones(SearchMilestonesInput.builder()
                .state(MilestoneState.ACTIVE)
                .titleSearch("release")
                .milestoneIds(List.of(3L, 7L))
                .build());

        assertThat(milestones).singleElement().isInstanceOfSatisfying(Milestone.class, milestone -> {
            assertThat(milestone.id()).isEqualTo(401L);
            assertThat(milestone.milestoneId()).isEqualTo(3L);
            assertThat(milestone.title()).isEqualTo("Release 1.0");
            assertThat(milestone.state()).isEqualTo("active");
        });
        server.verify();
    }

    @Test
    @DisplayName("omits state search and iids query parameters for ALL without filters")
    void omitsStateSearchAndIidsQueryParametersForAllWithoutFilters() {
        server.expect(requestTo(API_URL + "?page=1&per_page=100"))
                .andExpect(method(Objects.requireNonNull(HttpMethod.GET)))
                .andExpect(queryParam("page", "1"))
                .andExpect(queryParam("per_page", "100"))
                .andRespond(
                        withStatus(HttpStatus.OK).contentType(APPLICATION_JSON).body("[]"));

        final List<Milestone> milestones = adapter.searchMilestones(SearchMilestonesInput.builder()
                .state(MilestoneState.ALL)
                .titleSearch(null)
                .milestoneIds(List.of())
                .build());

        assertThat(milestones).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("aggregates milestones across pages until page size drops below 100")
    void aggregatesMilestonesAcrossPagesUntilPageSizeDropsBelow100() {
        server.expect(requestTo(API_URL + "?page=1&per_page=100&state=closed"))
                .andExpect(method(Objects.requireNonNull(HttpMethod.GET)))
                .andExpect(queryParam("page", "1"))
                .andExpect(queryParam("per_page", "100"))
                .andExpect(queryParam("state", "closed"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(APPLICATION_JSON)
                        .body(Objects.requireNonNull(milestonesBody(1, 100, "closed"))));

        server.expect(requestTo(API_URL + "?page=2&per_page=100&state=closed"))
                .andExpect(method(Objects.requireNonNull(HttpMethod.GET)))
                .andExpect(queryParam("page", "2"))
                .andExpect(queryParam("per_page", "100"))
                .andExpect(queryParam("state", "closed"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(APPLICATION_JSON)
                        .body(Objects.requireNonNull(milestonesBody(101, 1, "closed"))));

        final List<Milestone> milestones = adapter.searchMilestones(SearchMilestonesInput.builder()
                .state(MilestoneState.CLOSED)
                .titleSearch(null)
                .milestoneIds(List.of())
                .build());

        assertThat(milestones).hasSize(101);
        assertThat(milestones.getFirst().milestoneId()).isEqualTo(1L);
        assertThat(milestones.getLast().milestoneId()).isEqualTo(101L);
        server.verify();
    }

    @Test
    @DisplayName("maps GitLab 429 response to integration rate limited error")
    void mapsGitLab429ResponseToIntegrationRateLimitedError() {
        server.expect(requestTo(API_URL + "?page=1&per_page=100&state=active"))
                .andExpect(method(Objects.requireNonNull(HttpMethod.GET)))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> adapter.searchMilestones(SearchMilestonesInput.builder()
                        .state(MilestoneState.ACTIVE)
                        .titleSearch(null)
                        .milestoneIds(List.of())
                        .build()))
                .isInstanceOfSatisfying(IntegrationException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_RATE_LIMITED);
                    assertThat(exception.getMessage()).isEqualTo("GitLab milestones operation failed");
                });
        server.verify();
    }

    private static GitLabMilestonesAdapter createAdapter(
            final RestClient.Builder builder, final GitLabProjectLocator locator) {
        final GitLabOperationExecutor operationExecutor = new GitLabOperationExecutor(new GitLabExceptionMapper());
        final GitLabOffsetPaginationLoader paginationLoader = new GitLabOffsetPaginationLoader(operationExecutor);
        return new GitLabMilestonesAdapter(
                builder.baseUrl(Objects.requireNonNull(
                                locator.projectReference().apiBaseUrl()))
                        .build(),
                locator,
                new GitLabUriFactory(locator),
                paginationLoader,
                new GitLabMilestonesMapper());
    }

    private String milestonesBody(final int startMilestoneId, final int count, final String state) {
        final StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < count; index++) {
            final long milestoneId = startMilestoneId + index;
            if (index > 0) {
                builder.append(',');
            }
            builder.append('{')
                    .append("\"id\":")
                    .append(1000 + milestoneId)
                    .append(',')
                    .append("\"iid\":")
                    .append(milestoneId)
                    .append(',')
                    .append("\"title\":\"Milestone ")
                    .append(milestoneId)
                    .append("\",")
                    .append("\"description\":\"Generated\",")
                    .append("\"start_date\":\"2026-05-01\",")
                    .append("\"due_date\":\"2026-05-31\",")
                    .append("\"state\":\"")
                    .append(state)
                    .append("\"}");
        }
        builder.append(']');
        return builder.toString();
    }
}
