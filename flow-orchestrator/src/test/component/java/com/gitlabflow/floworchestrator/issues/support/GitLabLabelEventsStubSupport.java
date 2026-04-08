package com.gitlabflow.floworchestrator.issues.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;

public final class GitLabLabelEventsStubSupport {

    private static final String ISSUES_PATH = "/api/v4/projects/group%2Fproject/issues";
    private static final String LABEL_EVENTS_SUFFIX = "/resource_label_events";
    private static final String EMPTY_LABEL_EVENTS_RESPONSE = "[]";

    private GitLabLabelEventsStubSupport() {}

    public static void stubGetLabelEvents(final WireMockServer wireMockServer, final long issueId) {
        wireMockServer.stubFor(get(urlPathEqualTo(labelEventsPath(issueId)))
                .willReturn(okJson(loadFixture("stubs/issues/gitlab-label-events-response.json"))));
    }

    public static void stubGetLabelEventsEmpty(final WireMockServer wireMockServer, final long issueId) {
        wireMockServer.stubFor(
                get(urlPathEqualTo(labelEventsPath(issueId))).willReturn(okJson(EMPTY_LABEL_EVENTS_RESPONSE)));
    }

    public static void stubGetLabelEventsServerError(final WireMockServer wireMockServer, final long issueId) {
        wireMockServer.stubFor(get(urlPathEqualTo(labelEventsPath(issueId)))
                .willReturn(aResponse().withStatus(500)));
    }

    public static void stubGetLabelEventsNotFound(final WireMockServer wireMockServer, final long issueId) {
        wireMockServer.stubFor(get(urlPathEqualTo(labelEventsPath(issueId))).willReturn(notFound()));
    }

    public static void verifyGetLabelEventsRequest(final WireMockServer wireMockServer, final long issueId) {
        wireMockServer.verify(getRequestedFor(urlPathEqualTo(labelEventsPath(issueId))));
    }

    public static void verifyGetLabelEventsNotCalled(final WireMockServer wireMockServer, final long issueId) {
        wireMockServer.verify(0, getRequestedFor(urlPathEqualTo(labelEventsPath(issueId))));
    }

    private static String labelEventsPath(final long issueId) {
        return ISSUES_PATH + "/" + issueId + LABEL_EVENTS_SUFFIX;
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
