package com.gitlabflow.floworchestrator.issues.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public final class GitLabCreateIssueStubSupport {

    private static final String ISSUES_PATH = "/api/v4/projects/group%2Fproject/issues";
    private static final String FULL_CREATE_REQUEST = """
            {
              "title": "Deploy failure",
              "description": "Step 3 failed",
              "labels": "bug,deploy"
            }
            """;

    private GitLabCreateIssueStubSupport() {
    }

    public static void stubCreateIssueSuccess(final WireMockServer wireMockServer) {
        wireMockServer.stubFor(post(urlPathEqualTo(ISSUES_PATH))
                .withRequestBody(equalToJson(FULL_CREATE_REQUEST, true, false))
                .willReturn(created()
                        .withHeader("Content-Type", "application/json")
                        .withBody(loadFixture("stubs/issues/gitlab-create-issue-response.json"))));
    }

    public static void stubCreateIssueUnauthorized(final WireMockServer wireMockServer) {
        wireMockServer.stubFor(post(urlPathEqualTo(ISSUES_PATH))
                .willReturn(aResponse().withStatus(401)));
    }

    public static void verifyCreateIssueRequest(final WireMockServer wireMockServer) {
        wireMockServer.verify(postRequestedFor(urlPathEqualTo(ISSUES_PATH))
                .withRequestBody(equalToJson(FULL_CREATE_REQUEST, true, false)));
    }

    @SuppressWarnings("null")
    private static String loadFixture(final String path) {
        try {
            return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to load fixture " + path, exception);
        }
    }
}
