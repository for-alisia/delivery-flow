package com.gitlabflow.floworchestrator.milestones;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.gitlabflow.floworchestrator.milestones.support.GitLabMilestonesStubSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MilestonesApiComponentTest {

    private static final String CREATE_ENDPOINT = "/api/milestones";
    private static final String SEARCH_ENDPOINT = "/api/milestones/search";
    private static final int GITLAB_CONNECT_TIMEOUT_SECONDS = 2;
    private static final int GITLAB_READ_TIMEOUT_SECONDS = 1;

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
    }

    @Test
    @DisplayName("returns mapped milestones for default search without body")
    void returnsMappedMilestonesForDefaultSearchWithoutBody() throws Exception {
        wireMockServer.resetAll();
        GitLabMilestonesStubSupport.stubDefaultMilestones(wireMockServer);

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<Void> request = new HttpEntity<>(headers);

        final ResponseEntity<String> response = restTemplate.postForEntity(SEARCH_ENDPOINT, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("milestones").size()).isEqualTo(1);
        assertThat(json.path("milestones").get(0).path("id").asLong()).isEqualTo(301L);
        assertThat(json.path("milestones").get(0).path("milestoneId").asLong()).isEqualTo(1L);
        assertThat(json.path("milestones").get(0).path("title").asText()).isEqualTo("Release 1.0");
        assertThat(json.path("milestones").get(0).path("state").asText()).isEqualTo("active");

        GitLabMilestonesStubSupport.verifyDefaultMilestonesRequest(wireMockServer);
    }

    @Test
    @DisplayName("forwards all-state filtered search as search plus repeated iids parameters")
    void forwardsAllStateFilteredSearchAsSearchPlusRepeatedIidsParameters() throws Exception {
        wireMockServer.resetAll();
        GitLabMilestonesStubSupport.stubFilteredMilestones(wireMockServer);

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> request = new HttpEntity<>("""
                                {
                                  "filters": {
                                    "state": "all",
                                    "titleSearch": "  release  ",
                                    "milestoneIds": [1, 2]
                                  }
                                }
                                """, headers);

        final ResponseEntity<String> response = restTemplate.postForEntity(SEARCH_ENDPOINT, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("milestones").size()).isEqualTo(2);
        assertThat(json.path("milestones").get(0).path("milestoneId").asLong()).isEqualTo(1L);
        assertThat(json.path("milestones").get(1).path("milestoneId").asLong()).isEqualTo(2L);

        GitLabMilestonesStubSupport.verifyFilteredMilestonesRequest(wireMockServer);
    }

    @Test
    @DisplayName("rejects duplicate milestone ids before calling GitLab")
    void rejectsDuplicateMilestoneIdsBeforeCallingGitLab() throws Exception {
        wireMockServer.resetAll();

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> request = new HttpEntity<>("""
                                {
                                  "filters": {
                                    "milestoneIds": [3, 3]
                                  }
                                }
                                """, headers);

        final ResponseEntity<String> response = restTemplate.postForEntity(SEARCH_ENDPOINT, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("code").asText()).isEqualTo("VALIDATION_ERROR");

        GitLabMilestonesStubSupport.verifyNoMilestonesRequest(wireMockServer);
    }

    @Test
    @DisplayName("rejects null milestone id elements before calling GitLab")
    void rejectsNullMilestoneIdElementsBeforeCallingGitLab() throws Exception {
        wireMockServer.resetAll();

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> request = new HttpEntity<>("""
                                {
                                  "filters": {
                                    "milestoneIds": [1, null, 3]
                                  }
                                }
                                """, headers);

        final ResponseEntity<String> response = restTemplate.postForEntity(SEARCH_ENDPOINT, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(json.path("details").toString()).contains("filters.milestoneIds[1] must not be null");

        GitLabMilestonesStubSupport.verifyNoMilestonesRequest(wireMockServer);
    }

    @Test
    @DisplayName("aggregates milestones across multiple GitLab pages")
    void aggregatesMilestonesAcrossMultipleGitLabPages() throws Exception {
        wireMockServer.resetAll();
        GitLabMilestonesStubSupport.stubClosedMilestonesPagedResults(wireMockServer);

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> request = new HttpEntity<>("""
                                {
                                  "filters": {
                                    "state": "closed"
                                  }
                                }
                                """, headers);

        final ResponseEntity<String> response = restTemplate.postForEntity(SEARCH_ENDPOINT, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("milestones").size()).isEqualTo(101);
        assertThat(json.path("milestones").get(0).path("milestoneId").asLong()).isEqualTo(1L);
        assertThat(json.path("milestones").get(100).path("milestoneId").asLong())
                .isEqualTo(101L);

        GitLabMilestonesStubSupport.verifyClosedMilestonesPageRequests(wireMockServer);
    }

    @Test
    @DisplayName("maps GitLab rate limit failures to HTTP 429")
    void mapsGitLabRateLimitFailuresToHttp429() throws Exception {
        wireMockServer.resetAll();
        GitLabMilestonesStubSupport.stubRateLimitedDefaultMilestones(wireMockServer);

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<Void> request = new HttpEntity<>(headers);

        final ResponseEntity<String> response = restTemplate.postForEntity(SEARCH_ENDPOINT, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("code").asText()).isEqualTo("INTEGRATION_RATE_LIMITED");

        GitLabMilestonesStubSupport.verifyDefaultMilestonesRequest(wireMockServer);
    }

    @Test
    @DisplayName("creates milestone with title only and returns minimal response")
    void createsMilestoneWithTitleOnlyAndReturnsMinimalResponse() throws Exception {
        wireMockServer.resetAll();
        GitLabMilestonesStubSupport.stubCreateMilestoneTitleOnlySuccess(wireMockServer);

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> request = new HttpEntity<>("""
                                {
                                  "title": "Release v1.0"
                                }
                                """, headers);

        final ResponseEntity<String> response = restTemplate.postForEntity(CREATE_ENDPOINT, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("milestoneId").asLong()).isEqualTo(42L);
        assertThat(json.path("title").asText()).isEqualTo("Release v1.0");
        assertThat(json.has("id")).isFalse();
        assertThat(json.has("description")).isFalse();
        assertThat(json.has("startDate")).isFalse();
        assertThat(json.has("dueDate")).isFalse();

        GitLabMilestonesStubSupport.verifyCreateMilestoneTitleOnlyRequest(wireMockServer);
    }

    @Test
    @DisplayName("creates milestone with full payload and returns minimal response")
    void createsMilestoneWithFullPayloadAndReturnsMinimalResponse() throws Exception {
        wireMockServer.resetAll();
        GitLabMilestonesStubSupport.stubCreateMilestoneFullSuccess(wireMockServer);

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> request = new HttpEntity<>("""
                                {
                                  "title": "Q2 2026 Delivery",
                                  "description": "Second quarter release cycle",
                                  "startDate": "2026-04-01",
                                  "dueDate": "2026-06-30"
                                }
                                """, headers);

        final ResponseEntity<String> response = restTemplate.postForEntity(CREATE_ENDPOINT, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("milestoneId").asLong()).isEqualTo(43L);
        assertThat(json.path("title").asText()).isEqualTo("Q2 2026 Delivery");
        assertThat(json.size()).isEqualTo(2);

        GitLabMilestonesStubSupport.verifyCreateMilestoneFullRequest(wireMockServer);
    }

    @Test
    @DisplayName("rejects invalid create date range before calling GitLab")
    void rejectsInvalidCreateDateRangeBeforeCallingGitLab() throws Exception {
        wireMockServer.resetAll();

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> request = new HttpEntity<>("""
                                {
                                  "title": "Q2 2026 Delivery",
                                  "startDate": "2026-06-30",
                                  "dueDate": "2026-06-30"
                                }
                                """, headers);

        final ResponseEntity<String> response = restTemplate.postForEntity(CREATE_ENDPOINT, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(json.path("details").toString()).contains("dueDate must be after startDate");

        GitLabMilestonesStubSupport.verifyNoMilestonesApiRequest(wireMockServer);
    }

    @Test
    @DisplayName("maps create milestone GitLab authentication failure to unauthorized")
    void mapsCreateMilestoneGitLabAuthenticationFailureToUnauthorized() throws Exception {
        wireMockServer.resetAll();
        GitLabMilestonesStubSupport.stubCreateMilestoneUnauthorized(wireMockServer);

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<String> request = new HttpEntity<>("""
                                {
                                  "title": "Release v1.0"
                                }
                                """, headers);

        final ResponseEntity<String> response = restTemplate.postForEntity(CREATE_ENDPOINT, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("code").asText()).isEqualTo("INTEGRATION_AUTHENTICATION_FAILED");

        GitLabMilestonesStubSupport.verifyCreateMilestoneTitleOnlyRequest(wireMockServer);
    }
}
