package com.gitlabflow.floworchestrator.issues;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.gitlabflow.floworchestrator.issues.support.GitLabCreateIssueStubSupport;
import com.gitlabflow.floworchestrator.issues.support.GitLabDeleteIssueStubSupport;
import com.gitlabflow.floworchestrator.issues.support.GitLabIssuesStubSupport;
import com.gitlabflow.floworchestrator.issues.support.GitLabLabelEventsStubSupport;
import com.gitlabflow.floworchestrator.issues.support.GitLabSingleIssueStubSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IssuesApiComponentTest {

    private static final long ISSUE_ID = 42L;
    private static final long SEARCH_ISSUE_ID = 7L;
    private static final long SEARCH_ISSUE_ID_SECOND = 8L;
    private static final String ISSUE_DETAIL_PATH = "/api/issues/" + ISSUE_ID;
    private static final int GITLAB_CONNECT_TIMEOUT_SECONDS = 2;
    private static final int GITLAB_READ_TIMEOUT_SECONDS = 1;
    private static final int LABEL_EVENTS_DELAY_MILLIS = 3_000;

    private static WireMockServer wireMockServer;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void registerProperties(final DynamicPropertyRegistry registry) {
        registry.add("app.gitlab.url", () -> wireMockServer.baseUrl() + "/group/project");
        registry.add("app.gitlab.token", () -> "component-test-token");
        registry.add("app.gitlab.connect-timeout-seconds", () -> GITLAB_CONNECT_TIMEOUT_SECONDS);
        registry.add("app.gitlab.read-timeout-seconds", () -> GITLAB_READ_TIMEOUT_SECONDS);
        registry.add("app.issues-api.default-page-size", () -> 20);
        registry.add("app.issues-api.max-page-size", () -> 40);
    }

    @Test
    @DisplayName("returns mapped issues for request without body")
    void returnsMappedIssuesForRequestWithoutBody() throws Exception {
        wireMockServer.resetAll();
        GitLabIssuesStubSupport.stubDefaultIssues(wireMockServer);

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<Void> request = new HttpEntity<>(headers);

        final ResponseEntity<String> response = restTemplate.postForEntity("/api/issues/search", request, String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("count").asInt()).isEqualTo(1);
        assertThat(json.path("page").asInt()).isEqualTo(1);
        assertThat(json.path("items").get(0).path("id").asLong()).isEqualTo(123L);
        assertThat(json.path("items").get(0).path("issueId").asLong()).isEqualTo(7L);
        assertThat(json.path("items").get(0).path("assignee").asText()).isEqualTo("john.doe");
        assertThat(json.path("items").get(0).path("milestone").asText()).isEqualTo("M1");
        assertThat(json.path("items").get(0).path("parent").asLong()).isEqualTo(42L);
        assertThat(json.path("items").get(0).has("changeSets")).isFalse();

        GitLabIssuesStubSupport.verifyDefaultIssuesRequest(wireMockServer);
        GitLabLabelEventsStubSupport.verifyGetLabelEventsNotCalled(wireMockServer, SEARCH_ISSUE_ID);
    }

    @Test
    @DisplayName("returns search issues with label audit changeSets and lowercase field value")
    void returnsSearchIssuesWithLabelAuditChangeSetsAndLowercaseFieldValue() throws Exception {
        wireMockServer.resetAll();
        GitLabIssuesStubSupport.stubDefaultIssues(wireMockServer);
        GitLabLabelEventsStubSupport.stubGetLabelEvents(wireMockServer, SEARCH_ISSUE_ID);

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> request = new HttpEntity<>("""
                                {
                                    "pagination": {
                                        "page": 1,
                                        "perPage": 20
                                    },
                                    "filters": {
                                        "audit": ["label"]
                                    }
                                }
                                """, headers);

        final ResponseEntity<String> response = restTemplate.postForEntity("/api/issues/search", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("items").get(0).path("changeSets").isArray()).isTrue();
        assertThat(json.path("items")
                        .get(0)
                        .path("changeSets")
                        .get(0)
                        .path("change")
                        .path("field")
                        .asText())
                .isEqualTo("label");

        GitLabIssuesStubSupport.verifyDefaultIssuesRequest(wireMockServer);
        GitLabLabelEventsStubSupport.verifyGetLabelEventsRequest(wireMockServer, SEARCH_ISSUE_ID);
    }

    @Test
    @DisplayName("returns integration failure when one search label-events call fails")
    void returnsIntegrationFailureWhenOneSearchLabelEventsCallFails() throws Exception {
        wireMockServer.resetAll();
        GitLabIssuesStubSupport.stubDefaultIssuesMultiple(wireMockServer);
        GitLabLabelEventsStubSupport.stubGetLabelEvents(wireMockServer, SEARCH_ISSUE_ID);
        GitLabLabelEventsStubSupport.stubGetLabelEventsServerError(wireMockServer, SEARCH_ISSUE_ID_SECOND);
        useJdkRequestFactory();

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> request = new HttpEntity<>("""
                                {
                                    "filters": {
                                        "audit": ["label"]
                                    }
                                }
                                """, headers);

        final ResponseEntity<String> response = restTemplate.postForEntity("/api/issues/search", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("code").asText()).isEqualTo("INTEGRATION_FAILURE");
        assertThat(json.has("items")).isFalse();

        GitLabIssuesStubSupport.verifyDefaultIssuesRequest(wireMockServer);
        GitLabLabelEventsStubSupport.verifyGetLabelEventsRequest(wireMockServer, SEARCH_ISSUE_ID);
        GitLabLabelEventsStubSupport.verifyGetLabelEventsRequest(wireMockServer, SEARCH_ISSUE_ID_SECOND);
    }

    @Test
    @DisplayName("forwards supported filters to GitLab endpoint")
    void forwardsSupportedFiltersToGitLabEndpoint() {
        wireMockServer.resetAll();
        GitLabIssuesStubSupport.stubFilteredIssues(wireMockServer);

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> request = new HttpEntity<>("""
                {
                  "pagination": {
                    "page": 2,
                    "perPage": 20
                  },
                  "filters": {
                    "state": "opened",
                    "labels": ["bug"],
                    "assignee": ["john.doe"],
                    "milestone": ["M1"]
                  }
                }
                """, headers);

        final ResponseEntity<String> response = restTemplate.postForEntity("/api/issues/search", request, String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        GitLabIssuesStubSupport.verifyFilteredIssuesRequest(wireMockServer);
    }

    @Test
    @DisplayName("creates issue and returns mapped response")
    void createsIssueAndReturnsMappedResponse() throws Exception {
        wireMockServer.resetAll();
        GitLabCreateIssueStubSupport.stubCreateIssueSuccess(wireMockServer);

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> request = new HttpEntity<>("""
                {
                  "title": "Deploy failure",
                  "description": "Step 3 failed",
                  "labels": ["bug", "deploy"]
                }
                """, headers);

        final ResponseEntity<String> response = restTemplate.postForEntity("/api/issues", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("id").asLong()).isEqualTo(700L);
        assertThat(json.path("issueId").asLong()).isEqualTo(12L);
        assertThat(json.path("title").asText()).isEqualTo("Deploy failure");
        assertThat(json.path("description").asText()).isEqualTo("Step 3 failed");
        assertThat(json.path("state").asText()).isEqualTo("opened");
        assertThat(json.path("labels").get(0).asText()).isEqualTo("bug");
        assertThat(json.path("labels").get(1).asText()).isEqualTo("deploy");

        GitLabCreateIssueStubSupport.verifyCreateIssueRequest(wireMockServer);
    }

    @Test
    @DisplayName("maps gitlab authentication failure to unauthorized response")
    void mapsGitLabAuthenticationFailureToUnauthorizedResponse() throws Exception {
        wireMockServer.resetAll();
        GitLabCreateIssueStubSupport.stubCreateIssueUnauthorized(wireMockServer);
        useJdkRequestFactory();

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> request = new HttpEntity<>("""
                {
                  "title": "Deploy failure",
                  "description": "Step 3 failed",
                  "labels": ["bug", "deploy"]
                }
                """, headers);

        final ResponseEntity<String> response = restTemplate.postForEntity("/api/issues", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("code").asText()).isEqualTo("INTEGRATION_AUTHENTICATION_FAILED");
    }

    private void useJdkRequestFactory() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    @DisplayName("returns 200 with populated changeSets when GitLab label events exist")
    void returnsIssueDetailDtoWithPopulatedChangeSets() throws Exception {
        wireMockServer.resetAll();
        GitLabSingleIssueStubSupport.stubGetIssueDetail(wireMockServer, ISSUE_ID);
        GitLabLabelEventsStubSupport.stubGetLabelEvents(wireMockServer, ISSUE_ID);

        final ResponseEntity<String> response = restTemplate.getForEntity(ISSUE_DETAIL_PATH, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("issueId").asLong()).isEqualTo(ISSUE_ID);
        assertThat(json.path("title").asText()).isEqualTo("Fix login bug");
        assertThat(json.path("state").asText()).isEqualTo("opened");
        assertThat(json.path("assignees").get(0).path("username").asText()).isEqualTo("john.doe");
        assertThat(json.path("milestone").path("title").asText()).isEqualTo("Sprint 12");
        assertThat(json.path("milestone").path("milestoneId").asLong()).isEqualTo(3L);
        assertThat(json.path("changeSets").isArray()).isTrue();
        assertThat(json.path("changeSets").get(0).path("changeType").asText()).isEqualTo("add");
        assertThat(json.path("changeSets").get(0).path("changedBy").path("id").asLong())
                .isEqualTo(1L);
        assertThat(json.path("changeSets")
                        .get(0)
                        .path("changedBy")
                        .path("username")
                        .asText())
                .isEqualTo("root");
        assertThat(json.path("changeSets").get(0).path("change").path("field").asText())
                .isEqualTo("label");
        assertThat(json.path("changeSets").get(0).path("change").path("id").asLong())
                .isEqualTo(73L);
        assertThat(json.path("changeSets").get(0).path("change").path("name").asText())
                .isEqualTo("bug");

        GitLabSingleIssueStubSupport.verifyGetIssueDetailRequest(wireMockServer, ISSUE_ID);
        GitLabLabelEventsStubSupport.verifyGetLabelEventsRequest(wireMockServer, ISSUE_ID);
    }

    @Test
    @DisplayName("returns 200 with empty changeSets when GitLab label events are empty")
    void returnsIssueDetailDtoWithEmptyChangeSets() throws Exception {
        wireMockServer.resetAll();
        GitLabSingleIssueStubSupport.stubGetIssueDetail(wireMockServer, ISSUE_ID);
        GitLabLabelEventsStubSupport.stubGetLabelEventsEmpty(wireMockServer, ISSUE_ID);

        final ResponseEntity<String> response = restTemplate.getForEntity(ISSUE_DETAIL_PATH, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("changeSets").isArray()).isTrue();
        assertThat(json.path("changeSets").isEmpty()).isTrue();

        GitLabSingleIssueStubSupport.verifyGetIssueDetailRequest(wireMockServer, ISSUE_ID);
        GitLabLabelEventsStubSupport.verifyGetLabelEventsRequest(wireMockServer, ISSUE_ID);
    }

    @Test
    @DisplayName("returns integration failure when GitLab label events call fails")
    void returnsIntegrationFailureWhenGitLabLabelEventsCallFails() throws Exception {
        wireMockServer.resetAll();
        GitLabSingleIssueStubSupport.stubGetIssueDetail(wireMockServer, ISSUE_ID);
        GitLabLabelEventsStubSupport.stubGetLabelEventsServerError(wireMockServer, ISSUE_ID);
        useJdkRequestFactory();

        final ResponseEntity<String> response = restTemplate.getForEntity(ISSUE_DETAIL_PATH, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("code").asText()).isEqualTo("INTEGRATION_FAILURE");
        assertThat(json.has("issueId")).isFalse();

        GitLabSingleIssueStubSupport.verifyGetIssueDetailRequest(wireMockServer, ISSUE_ID);
        GitLabLabelEventsStubSupport.verifyGetLabelEventsRequest(wireMockServer, ISSUE_ID);
    }

    @Test
    @DisplayName("returns integration failure when GitLab label events call exceeds read timeout")
    void returnsIntegrationFailureWhenGitLabLabelEventsCallExceedsReadTimeout() throws Exception {
        wireMockServer.resetAll();
        GitLabSingleIssueStubSupport.stubGetIssueDetail(wireMockServer, ISSUE_ID);
        GitLabLabelEventsStubSupport.stubGetLabelEventsDelayed(wireMockServer, ISSUE_ID, LABEL_EVENTS_DELAY_MILLIS);

        final long startedAt = System.nanoTime();
        final ResponseEntity<String> response = restTemplate.getForEntity(ISSUE_DETAIL_PATH, String.class);
        final long durationMillis = (System.nanoTime() - startedAt) / 1_000_000L;

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("code").asText()).isEqualTo("INTEGRATION_FAILURE");
        assertThat(durationMillis).isLessThan(LABEL_EVENTS_DELAY_MILLIS);

        GitLabSingleIssueStubSupport.verifyGetIssueDetailRequest(wireMockServer, ISSUE_ID);
        GitLabLabelEventsStubSupport.verifyGetLabelEventsRequest(wireMockServer, ISSUE_ID);
    }

    @Test
    @DisplayName("returns 404 when GitLab returns 404 for issue during parallel detail fetch")
    void returns404WhenGitLabReturns404DuringParallelDetailFetch() throws Exception {
        wireMockServer.resetAll();
        GitLabSingleIssueStubSupport.stubGetIssueDetailNotFound(wireMockServer, ISSUE_ID);
        GitLabLabelEventsStubSupport.stubGetLabelEventsNotFound(wireMockServer, ISSUE_ID);
        useJdkRequestFactory();

        final ResponseEntity<String> response = restTemplate.getForEntity(ISSUE_DETAIL_PATH, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("code").asText()).isEqualTo("RESOURCE_NOT_FOUND");

        GitLabSingleIssueStubSupport.verifyGetIssueDetailRequest(wireMockServer, ISSUE_ID);
        GitLabLabelEventsStubSupport.verifyGetLabelEventsRequest(wireMockServer, ISSUE_ID);
    }

    @Test
    @DisplayName("deletes issue and returns no content")
    void deletesIssueAndReturnsNoContent() {
        wireMockServer.resetAll();
        GitLabDeleteIssueStubSupport.stubDeleteIssueSuccess(wireMockServer, 26L);

        final ResponseEntity<Void> response =
                restTemplate.exchange("/api/issues/26", HttpMethod.DELETE, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        GitLabDeleteIssueStubSupport.verifyDeleteIssueRequest(wireMockServer, 26L);
    }
}
