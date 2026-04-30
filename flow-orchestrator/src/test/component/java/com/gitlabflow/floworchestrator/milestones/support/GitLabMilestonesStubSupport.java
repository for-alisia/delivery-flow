package com.gitlabflow.floworchestrator.milestones.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;

public final class GitLabMilestonesStubSupport {

    private static final String MILESTONES_PATH = "/api/v4/projects/group%2Fproject/milestones";
    private static final String CREATE_MILESTONES_PATH_REGEX = "/api/v4/projects/.*/milestones";
    private static final String CREATE_TITLE_ONLY_REQUEST = """
                        {
                            "title": "Release v1.0"
                        }
                        """;
    private static final String CREATE_FULL_REQUEST = """
                        {
                            "title": "Q2 2026 Delivery",
                            "description": "Second quarter release cycle",
                            "start_date": "2026-04-01",
                            "due_date": "2026-06-30"
                        }
                        """;
    private static final String DEFAULT_ACTIVE_URL = MILESTONES_PATH + "?page=1&per_page=100&state=active";
    private static final String FILTERED_ALL_URL =
            MILESTONES_PATH + "?page=1&per_page=100&search=release&iids%5B%5D=1&iids%5B%5D=2";
    private static final String SINGLE_FIXTURE = "stubs/milestones/gitlab-milestones-single.json";
    private static final String FILTERED_FIXTURE = "stubs/milestones/gitlab-milestones-filtered.json";
    private static final String CREATE_TITLE_ONLY_FIXTURE = "stubs/milestones/gitlab-create-milestone-title-only.json";
    private static final String CREATE_FULL_FIXTURE = "stubs/milestones/gitlab-create-milestone-full.json";

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

    public static void stubCreateMilestoneTitleOnlySuccess(final WireMockServer wireMockServer) {
        final String responseBody = loadFixture(CREATE_TITLE_ONLY_FIXTURE);
        wireMockServer.stubFor(
                post(urlPathMatching(CREATE_MILESTONES_PATH_REGEX)).willReturn(jsonOkResponse(responseBody)));
    }

    public static void verifyCreateMilestoneTitleOnlyRequest(final WireMockServer wireMockServer) {
        wireMockServer.verify(postRequestedFor(urlPathMatching(CREATE_MILESTONES_PATH_REGEX))
                .withRequestBody(equalToJson(CREATE_TITLE_ONLY_REQUEST, true, false)));
    }

    public static void stubCreateMilestoneFullSuccess(final WireMockServer wireMockServer) {
        final String responseBody = loadFixture(CREATE_FULL_FIXTURE);
        wireMockServer.stubFor(
                post(urlPathMatching(CREATE_MILESTONES_PATH_REGEX)).willReturn(jsonOkResponse(responseBody)));
    }

    public static void verifyCreateMilestoneFullRequest(final WireMockServer wireMockServer) {
        wireMockServer.verify(postRequestedFor(urlPathMatching(CREATE_MILESTONES_PATH_REGEX))
                .withRequestBody(equalToJson(CREATE_FULL_REQUEST, true, false)));
    }

    public static void stubCreateMilestoneUnauthorized(final WireMockServer wireMockServer) {
        wireMockServer.stubFor(post(urlPathMatching(CREATE_MILESTONES_PATH_REGEX))
                .willReturn(aResponse().withStatus(401)));
    }

    public static void verifyNoMilestonesApiRequest(final WireMockServer wireMockServer) {
        wireMockServer.verify(0, anyRequestedFor(urlPathMatching(CREATE_MILESTONES_PATH_REGEX)));
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

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder jsonOkResponse(final String body) {
        return aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withHeader("Content-Length", String.valueOf(body.getBytes(StandardCharsets.UTF_8).length))
                .withBody(body);
    }
}
