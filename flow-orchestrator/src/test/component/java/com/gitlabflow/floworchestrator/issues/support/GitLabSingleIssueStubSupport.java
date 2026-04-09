package com.gitlabflow.floworchestrator.issues.support;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;

public final class GitLabSingleIssueStubSupport {

    private static final String ISSUES_PATH = "/api/v4/projects/group%2Fproject/issues";

    private GitLabSingleIssueStubSupport() {}

    public static void stubGetIssueDetail(final WireMockServer wireMockServer, final long issueId) {
        wireMockServer.stubFor(get(urlPathEqualTo(ISSUES_PATH + "/" + issueId))
                .willReturn(okJson(loadFixture("stubs/issues/gitlab-single-issue-detail.json"))));
    }

    public static void stubGetIssueDetailNotFound(final WireMockServer wireMockServer, final long issueId) {
        wireMockServer.stubFor(get(urlPathEqualTo(ISSUES_PATH + "/" + issueId)).willReturn(notFound()));
    }

    public static void verifyGetIssueDetailRequest(final WireMockServer wireMockServer, final long issueId) {
        wireMockServer.verify(getRequestedFor(urlPathEqualTo(ISSUES_PATH + "/" + issueId)));
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
