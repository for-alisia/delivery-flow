package com.gitlabflow.floworchestrator.integration.gitlab;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitlabflow.floworchestrator.common.error.ErrorCode;
import com.gitlabflow.floworchestrator.common.error.IntegrationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

class GitLabExceptionMapperTest {

    private static final String SOURCE = "gitlab";
    private static final String RESOURCE_ISSUES = "issues";

    private final GitLabExceptionMapper mapper = new GitLabExceptionMapper();

    @Test
    @DisplayName("maps 401 to authentication failed")
    void mapsUnauthorizedToAuthenticationFailed() {
        final IntegrationException exception = mapper.fromHttpFailure(httpFailure(401), SOURCE, RESOURCE_ISSUES);

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_AUTHENTICATION_FAILED);
        assertThat(exception.getMessage()).isEqualTo("GitLab issues operation failed");
    }

    @Test
    @DisplayName("maps 403 to forbidden")
    void mapsForbiddenToForbidden() {
        final IntegrationException exception = mapper.fromHttpFailure(httpFailure(403), SOURCE, RESOURCE_ISSUES);

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_FORBIDDEN);
    }

    @Test
    @DisplayName("maps 404 to not found")
    void mapsNotFoundToNotFound() {
        final IntegrationException exception = mapper.fromHttpFailure(httpFailure(404), SOURCE, RESOURCE_ISSUES);

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_NOT_FOUND);
    }

    @Test
    @DisplayName("maps 429 to rate limited")
    void mapsRateLimitedToRateLimited() {
        final IntegrationException exception = mapper.fromHttpFailure(httpFailure(429), SOURCE, RESOURCE_ISSUES);

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_RATE_LIMITED);
    }

    @Test
    @DisplayName("maps unsupported status to generic integration failure")
    void mapsUnsupportedStatusToGenericFailure() {
        final IntegrationException exception = mapper.fromHttpFailure(httpFailure(500), SOURCE, RESOURCE_ISSUES);

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_FAILURE);
    }

    @Test
    @DisplayName("maps transport failure to generic integration failure")
    void mapsTransportFailureToGenericFailure() {
        final IntegrationException exception = mapper.fromTransportFailure(SOURCE, RESOURCE_ISSUES);

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_FAILURE);
        assertThat(exception.getMessage()).isEqualTo("GitLab issues operation failed");
    }

    @Test
    @DisplayName("uses resource-specific neutral operation message")
    void usesResourceSpecificNeutralOperationMessage() {
        final IntegrationException exception = mapper.fromHttpFailure(httpFailure(401), SOURCE, "create issue");

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_AUTHENTICATION_FAILED);
        assertThat(exception.getMessage()).isEqualTo("GitLab create issue operation failed");
    }

    private RestClientResponseException httpFailure(final int statusCode) {
        return new RestClientResponseException("GitLab failure", statusCode, "error", null, null, null);
    }
}
