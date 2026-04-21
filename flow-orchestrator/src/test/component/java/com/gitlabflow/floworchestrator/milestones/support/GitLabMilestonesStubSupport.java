package com.gitlabflow.floworchestrator.milestones.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;

public final class GitLabMilestonesStubSupport {

    private static final String MILESTONES_PATH = "/api/v4/projects/group%2Fproject/milestones";
    private static final String DEFAULT_ACTIVE_URL = MILESTONES_PATH + "?page=1&per_page=100&state=active";
    private static final String FILTERED_ALL_URL =
            MILESTONES_PATH + "?page=1&per_page=100&search=release&iids%5B%5D=1&iids%5B%5D=2";
    private static final String SINGLE_FIXTURE = "stubs/milestones/gitlab-milestones-single.json";
    private static final String FILTERED_FIXTURE = "stubs/milestones/gitlab-milestones-filtered.json";

    private GitLabMilestonesStubSupport() {}

    public static void stubDefaultMilestones(final WireMockServer wireMockServer) {
        wireMockServer.stubFor(get(urlEqualTo(DEFAULT_ACTIVE_URL)).willReturn(okJson(loadFixture(SINGLE_FIXTURE))));
    }

    public static void verifyDefaultMilestonesRequest(final WireMockServer wireMockServer) {
        wireMockServer.verify(getRequestedFor(urlEqualTo(DEFAULT_ACTIVE_URL)));
    }

    public static void stubFilteredMilestones(final WireMockServer wireMockServer) {
        wireMockServer.stubFor(get(urlEqualTo(FILTERED_ALL_URL)).willReturn(okJson(loadFixture(FILTERED_FIXTURE))));
    }

    public static void verifyFilteredMilestonesRequest(final WireMockServer wireMockServer) {
        wireMockServer.verify(getRequestedFor(urlEqualTo(FILTERED_ALL_URL)));
    }

    public static void stubClosedMilestonesPagedResults(final WireMockServer wireMockServer) {
        wireMockServer.stubFor(
                get(urlEqualTo(closedMilestonesUrl(1))).willReturn(okJson(buildMilestonesPageBody(1, 100))));
        wireMockServer.stubFor(
                get(urlEqualTo(closedMilestonesUrl(2))).willReturn(okJson(buildMilestonesPageBody(101, 1))));
    }

    public static void verifyClosedMilestonesPageRequests(final WireMockServer wireMockServer) {
        wireMockServer.verify(getRequestedFor(urlEqualTo(closedMilestonesUrl(1))));
        wireMockServer.verify(getRequestedFor(urlEqualTo(closedMilestonesUrl(2))));
    }

    public static void stubRateLimitedDefaultMilestones(final WireMockServer wireMockServer) {
        wireMockServer.stubFor(
                get(urlEqualTo(DEFAULT_ACTIVE_URL)).willReturn(aResponse().withStatus(429)));
    }

    public static void verifyNoMilestonesRequest(final WireMockServer wireMockServer) {
        wireMockServer.verify(0, getRequestedFor(urlPathEqualTo(MILESTONES_PATH)));
    }

    private static String closedMilestonesUrl(final int page) {
        return MILESTONES_PATH + "?page=" + page + "&per_page=100&state=closed";
    }

    private static String buildMilestonesPageBody(final int startMilestoneId, final int count) {
        final StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < count; index++) {
            final long milestoneId = startMilestoneId + index;
            if (index > 0) {
                builder.append(',');
            }

            builder.append('{')
                    .append("\"id\":")
                    .append(2000 + milestoneId)
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
                    .append("\"state\":\"closed\"}");
        }
        builder.append(']');
        return builder.toString();
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
