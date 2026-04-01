package com.gitlabflow.floworchestrator.issues;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.gitlabflow.floworchestrator.issues.support.GitLabIssuesStubSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IssuesApiComponentTest {

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
        registry.add("app.issues-api.default-page-size", () -> 40);
        registry.add("app.issues-api.max-page-size", () -> 100);
    }

    @Test
    @DisplayName("returns mapped issues for request without body")
    void returnsMappedIssuesForRequestWithoutBody() throws Exception {
        wireMockServer.resetAll();
        GitLabIssuesStubSupport.stubDefaultIssues(wireMockServer);

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<Void> request = new HttpEntity<>(headers);

        final ResponseEntity<String> response = restTemplate.postForEntity("/api/issues", request, String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        final JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.path("count").asInt()).isEqualTo(1);
        assertThat(json.path("page").asInt()).isEqualTo(1);
        assertThat(json.path("items").get(0).path("id").asLong()).isEqualTo(123L);
        assertThat(json.path("items").get(0).path("assignee").asText()).isEqualTo("john.doe");
        assertThat(json.path("items").get(0).path("milestone").asText()).isEqualTo("M1");
        assertThat(json.path("items").get(0).path("parent").asLong()).isEqualTo(42L);

        GitLabIssuesStubSupport.verifyDefaultIssuesRequest(wireMockServer);
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

        final ResponseEntity<String> response = restTemplate.postForEntity("/api/issues", request, String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        GitLabIssuesStubSupport.verifyFilteredIssuesRequest(wireMockServer);
    }
}
