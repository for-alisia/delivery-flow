package com.gitlabflow.floworchestrator.issues.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;

public final class GitLabUpdateIssueStubSupport {

    private static final String ISSUES_PATH = "/api/v4/projects/group%2Fproject/issues";
    private static final String UPDATE_REQUEST = """
            {
              "title": "Updated from component",
              "description": "Updated description",
              "add_labels": "backend",
              "remove_labels": "bug"
            }
            """;
    private static final String CLEAR_DESCRIPTION_REQUEST = """
            {
              "description": ""
            }
            """;

    private GitLabUpdateIssueStubSupport() {}

    public static void stubUpdateIssueSuccess(final WireMockServer wireMockServer, final long issueId) {
        wireMockServer.stubFor(put(urlPathEqualTo(updatePath(issueId)))
                .withRequestBody(equalToJson(UPDATE_REQUEST, true, false))
                .willReturn(okJson("""
                        {
                          "id": 700,
                          "iid": 12,
                          "title": "Updated from component",
                          "description": "Updated description",
                          "state": "opened",
                          "labels": ["backend"]
                        }
                        """)));
    }

    public static void stubClearDescriptionSuccess(final WireMockServer wireMockServer, final long issueId) {
        wireMockServer.stubFor(put(urlPathEqualTo(updatePath(issueId)))
                .withRequestBody(equalToJson(CLEAR_DESCRIPTION_REQUEST, true, false))
                .willReturn(okJson("""
                        {
                          "id": 701,
                          "iid": 12,
                          "title": "Updated from component",
                          "description": "",
                          "state": "opened",
                          "labels": ["backend"]
                        }
                        """)));
    }

    public static void stubUpdateIssueNotFound(final WireMockServer wireMockServer, final long issueId) {
        wireMockServer.stubFor(put(urlPathEqualTo(updatePath(issueId))).willReturn(notFound()));
    }

    public static void stubUpdateIssueServerError(final WireMockServer wireMockServer, final long issueId) {
        wireMockServer.stubFor(
                put(urlPathEqualTo(updatePath(issueId))).willReturn(aResponse().withStatus(500)));
    }

    public static void verifyUpdateIssueRequest(final WireMockServer wireMockServer, final long issueId) {
        wireMockServer.verify(putRequestedFor(urlPathEqualTo(updatePath(issueId)))
                .withRequestBody(equalToJson(UPDATE_REQUEST, true, false)));
    }

    public static void verifyClearDescriptionRequest(final WireMockServer wireMockServer, final long issueId) {
        wireMockServer.verify(putRequestedFor(urlPathEqualTo(updatePath(issueId)))
                .withRequestBody(equalToJson(CLEAR_DESCRIPTION_REQUEST, true, false)));
    }

    public static void verifyUpdateIssueNotCalled(final WireMockServer wireMockServer, final long issueId) {
        wireMockServer.verify(0, putRequestedFor(urlPathEqualTo(updatePath(issueId))));
    }

    private static String updatePath(final long issueId) {
        return ISSUES_PATH + "/" + issueId;
    }
}
