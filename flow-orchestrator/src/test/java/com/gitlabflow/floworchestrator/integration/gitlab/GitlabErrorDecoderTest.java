package com.gitlabflow.floworchestrator.integration.gitlab;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gitlabflow.floworchestrator.common.errors.ErrorCode;
import com.gitlabflow.floworchestrator.common.errors.IntegrationException;

import feign.Request;
import feign.RequestTemplate;
import feign.Response;

@DisplayName("GitlabErrorDecoder")
class GitlabErrorDecoderTest {

    private final GitlabErrorDecoder decoder = new GitlabErrorDecoder();

    @Test
    @DisplayName("given unauthorized response when decode then returns unauthorized integration exception")
    void givenUnauthorizedResponseWhenDecodeThenReturnsUnauthorizedIntegrationException() {
        IntegrationException result = (IntegrationException) decoder.decode("gitlab#listProjectIssues", response(401, Map.of()));

        assertThat(result.getCode()).isEqualTo(ErrorCode.INTEGRATION_UNAUTHORIZED);
        assertThat(result.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("given forbidden response when decode then returns forbidden integration exception")
    void givenForbiddenResponseWhenDecodeThenReturnsForbiddenIntegrationException() {
        IntegrationException result = (IntegrationException) decoder.decode("gitlab#listProjectIssues", response(403, Map.of()));

        assertThat(result.getCode()).isEqualTo(ErrorCode.INTEGRATION_FORBIDDEN);
        assertThat(result.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("given not found response when decode then returns not found integration exception")
    void givenNotFoundResponseWhenDecodeThenReturnsNotFoundIntegrationException() {
        IntegrationException result = (IntegrationException) decoder.decode("gitlab#listProjectIssues", response(404, Map.of()));

        assertThat(result.getCode()).isEqualTo(ErrorCode.INTEGRATION_NOT_FOUND);
        assertThat(result.getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("given rate limited response when decode then returns retry after in integration exception")
    void givenRateLimitedResponseWhenDecodeThenReturnsRetryAfterInIntegrationException() {
        IntegrationException result = (IntegrationException) decoder.decode(
                "gitlab#listProjectIssues",
                response(429, Map.of("Retry-After", java.util.List.of("60")))
        );

        assertThat(result.getCode()).isEqualTo(ErrorCode.INTEGRATION_RATE_LIMITED);
        assertThat(result.getRetryAfterSeconds()).hasValue(60L);
    }

    @Test
    @DisplayName("given server error response when decode then returns unavailable integration exception")
    void givenServerErrorResponseWhenDecodeThenReturnsUnavailableIntegrationException() {
        IntegrationException result = (IntegrationException) decoder.decode("gitlab#listProjectIssues", response(503, Map.of()));

        assertThat(result.getCode()).isEqualTo(ErrorCode.INTEGRATION_UNAVAILABLE);
        assertThat(result.getStatus()).isEqualTo(503);
    }

    private static Response response(int status, Map<String, java.util.Collection<String>> headers) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "https://gitlab.example.com/api/v4/projects/1/issues",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate()
        );

        return Response.builder()
                .request(request)
                .status(status)
                .reason("error")
                .headers(headers)
                .build();
    }
}
