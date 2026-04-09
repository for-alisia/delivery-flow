package com.gitlabflow.floworchestrator.issues.support;

import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;

public final class GitLabDeleteIssueStubSupport {

    private static final String ISSUES_PATH = "/api/v4/projects/group%2Fproject/issues";

    private GitLabDeleteIssueStubSupport() {}

    public static void stubDeleteIssueSuccess(final WireMockServer wireMockServer, final long issueId) {
        wireMockServer.stubFor(
                delete(urlPathEqualTo(ISSUES_PATH + "/" + issueId)).willReturn(noContent()));
    }

    public static void verifyDeleteIssueRequest(final WireMockServer wireMockServer, final long issueId) {
        wireMockServer.verify(deleteRequestedFor(urlPathEqualTo(ISSUES_PATH + "/" + issueId)));
    }
}
