package com.gitlabflow.floworchestrator.integration.gitlab;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
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
    @DisplayName("given status 400 when decode then returns integration bad request")
    void givenStatus400WhenDecodeThenReturnsIntegrationBadRequest() {
        IntegrationException exception = (IntegrationException) decoder.decode("GitlabIssuesClient#getIssues", response(400, Map.of()));

        assertThat(exception.getCode()).isEqualTo(ErrorCode.INTEGRATION_BAD_REQUEST);
        assertThat(exception.getSource()).isEqualTo("gitlab");
    }

    @Test
    @DisplayName("given status 401 when decode then returns integration unauthorized")
    void givenStatus401WhenDecodeThenReturnsIntegrationUnauthorized() {
        IntegrationException exception = (IntegrationException) decoder.decode("GitlabIssuesClient#getIssues", response(401, Map.of()));

        assertThat(exception.getCode()).isEqualTo(ErrorCode.INTEGRATION_UNAUTHORIZED);
    }

    @Test
    @DisplayName("given status 403 when decode then returns integration forbidden")
    void givenStatus403WhenDecodeThenReturnsIntegrationForbidden() {
        IntegrationException exception = (IntegrationException) decoder.decode("GitlabIssuesClient#getIssues", response(403, Map.of()));

        assertThat(exception.getCode()).isEqualTo(ErrorCode.INTEGRATION_FORBIDDEN);
    }

    @Test
    @DisplayName("given status 404 when decode then returns integration not found")
    void givenStatus404WhenDecodeThenReturnsIntegrationNotFound() {
        IntegrationException exception = (IntegrationException) decoder.decode("GitlabIssuesClient#getIssues", response(404, Map.of()));

        assertThat(exception.getCode()).isEqualTo(ErrorCode.INTEGRATION_NOT_FOUND);
    }

    @Test
    @DisplayName("given status 429 with retry after when decode then returns rate limited with retry")
    void givenStatus429WithRetryAfterWhenDecodeThenReturnsRateLimitedWithRetry() {
        IntegrationException exception = (IntegrationException) decoder.decode(
                "GitlabIssuesClient#getIssues",
                response(429, Map.of("Retry-After", List.of("30")))
        );

        assertThat(exception.getCode()).isEqualTo(ErrorCode.INTEGRATION_RATE_LIMITED);
        assertThat(exception.getRetryAfterSeconds()).contains(30L);
    }

    @Test
    @DisplayName("given status 429 without retry after when decode then returns rate limited without retry")
    void givenStatus429WithoutRetryAfterWhenDecodeThenReturnsRateLimitedWithoutRetry() {
        IntegrationException exception = (IntegrationException) decoder.decode("GitlabIssuesClient#getIssues", response(429, Map.of()));

        assertThat(exception.getCode()).isEqualTo(ErrorCode.INTEGRATION_RATE_LIMITED);
        assertThat(exception.getRetryAfterSeconds()).isEmpty();
    }

    @Test
    @DisplayName("given status 500 when decode then returns integration unknown")
    void givenStatus500WhenDecodeThenReturnsIntegrationUnknown() {
        IntegrationException exception = (IntegrationException) decoder.decode("GitlabIssuesClient#getIssues", response(500, Map.of()));

        assertThat(exception.getCode()).isEqualTo(ErrorCode.INTEGRATION_UNKNOWN);
    }

    @Test
    @DisplayName("given status 503 when decode then returns integration unknown")
    void givenStatus503WhenDecodeThenReturnsIntegrationUnknown() {
        IntegrationException exception = (IntegrationException) decoder.decode("GitlabIssuesClient#getIssues", response(503, Map.of()));

        assertThat(exception.getCode()).isEqualTo(ErrorCode.INTEGRATION_UNKNOWN);
    }

    private Response response(int status, Map<String, Collection<String>> headers) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "https://gitlab.example.com/api/v4/projects/group%2Fproject/issues",
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
