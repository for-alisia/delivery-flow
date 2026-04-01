package com.gitlabflow.floworchestrator.integration.gitlab;

import com.gitlabflow.floworchestrator.common.errors.ErrorCode;
import com.gitlabflow.floworchestrator.common.errors.IntegrationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;

class GitLabExceptionMapperTest {

    private static final String SOURCE = "gitlab";

    private final GitLabExceptionMapper mapper = new GitLabExceptionMapper();

    @Test
    @DisplayName("maps 401 to authentication failed")
    void mapsUnauthorizedToAuthenticationFailed() {
        final IntegrationException exception = mapper.fromHttpFailure(httpFailure(401), SOURCE);

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_AUTHENTICATION_FAILED);
    }

    @Test
    @DisplayName("maps 404 to not found")
    void mapsNotFoundToNotFound() {
        final IntegrationException exception = mapper.fromHttpFailure(httpFailure(404), SOURCE);

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_NOT_FOUND);
    }

    @Test
    @DisplayName("maps 429 to rate limited")
    void mapsRateLimitedToRateLimited() {
        final IntegrationException exception = mapper.fromHttpFailure(httpFailure(429), SOURCE);

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_RATE_LIMITED);
    }

    @Test
    @DisplayName("maps unsupported status to generic integration failure")
    void mapsUnsupportedStatusToGenericFailure() {
        final IntegrationException exception = mapper.fromHttpFailure(httpFailure(500), SOURCE);

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_FAILURE);
    }

    @Test
    @DisplayName("maps transport failure to generic integration failure")
    void mapsTransportFailureToGenericFailure() {
        final IntegrationException exception = mapper.fromTransportFailure(SOURCE);

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_FAILURE);
    }

    private RestClientResponseException httpFailure(final int statusCode) {
        return new RestClientResponseException("GitLab failure", statusCode, "error", null, null, null);
    }
}