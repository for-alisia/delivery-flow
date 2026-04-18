package com.gitlabflow.floworchestrator.issues.support;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;

public final class GitLabIssuesStubSupport {

    private static final String ISSUES_PATH = "/api/v4/projects/group%2Fproject/issues";
    private static final String SINGLE_ISSUE_FIXTURE = "stubs/issues/gitlab-issues-single.json";
    private static final String MULTI_ISSUE_FIXTURE = "stubs/issues/gitlab-issues-multi.json";

    private GitLabIssuesStubSupport() {}

    public static void stubDefaultIssues(final WireMockServer wireMockServer) {
        wireMockServer.stubFor(get(urlPathEqualTo(ISSUES_PATH))
                .withQueryParam("page", equalTo("1"))
                .withQueryParam("per_page", equalTo("20"))
                .willReturn(okJson(loadFixture(SINGLE_ISSUE_FIXTURE))));
    }

    public static void stubDefaultIssuesMultiple(final WireMockServer wireMockServer) {
        wireMockServer.stubFor(get(urlPathEqualTo(ISSUES_PATH))
                .withQueryParam("page", equalTo("1"))
                .withQueryParam("per_page", equalTo("20"))
                .willReturn(okJson(loadFixture(MULTI_ISSUE_FIXTURE))));
    }

    public static void stubFilteredIssues(final WireMockServer wireMockServer) {
        wireMockServer.stubFor(get(urlPathEqualTo(ISSUES_PATH))
                .withQueryParam("page", equalTo("2"))
                .withQueryParam("per_page", equalTo("20"))
                .withQueryParam("state", equalTo("opened"))
                .withQueryParam("labels", equalTo("bug"))
                .withQueryParam("assignee_username", equalTo("john.doe"))
                .withQueryParam("milestone", equalTo("M1"))
                .willReturn(okJson(loadFixture("stubs/issues/gitlab-issues-single.json"))));
    }

    public static void verifyDefaultIssuesRequest(final WireMockServer wireMockServer) {
        wireMockServer.verify(getRequestedFor(urlPathEqualTo(ISSUES_PATH))
                .withQueryParam("page", equalTo("1"))
                .withQueryParam("per_page", equalTo("20")));
    }

    public static void verifyFilteredIssuesRequest(final WireMockServer wireMockServer) {
        wireMockServer.verify(getRequestedFor(urlPathEqualTo(ISSUES_PATH))
                .withQueryParam("page", equalTo("2"))
                .withQueryParam("per_page", equalTo("20"))
                .withQueryParam("state", equalTo("opened"))
                .withQueryParam("labels", equalTo("bug"))
                .withQueryParam("assignee_username", equalTo("john.doe"))
                .withQueryParam("milestone", equalTo("M1")));
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
