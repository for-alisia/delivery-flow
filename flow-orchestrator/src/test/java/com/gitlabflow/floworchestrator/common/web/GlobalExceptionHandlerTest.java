package com.gitlabflow.floworchestrator.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitlabflow.floworchestrator.common.error.ErrorCode;
import com.gitlabflow.floworchestrator.common.error.IntegrationException;
import com.gitlabflow.floworchestrator.common.error.ValidationException;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("returns validation error for business validation exception")
    void returnsValidationErrorForValidationException() {
        final ValidationException exception = new ValidationException(
                "Request validation failed", List.of("pagination.perPage must be less than or equal to 100"));

        final ResponseEntity<ErrorResponse> response = handler.handleValidationException(exception);
        final ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.message()).isEqualTo("Request validation failed");
        assertThat(body.details()).containsExactly("pagination.perPage must be less than or equal to 100");
    }

    @Test
    @DisplayName("returns validation error for malformed json")
    void returnsValidationErrorForMalformedJson() {
        final HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException("bad json", new MockHttpInputMessage(new byte[0]));

        final ResponseEntity<ErrorResponse> response = handler.handleMalformedJson(exception);
        final ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.message()).isEqualTo("Request validation failed");
        assertThat(body.details()).containsExactly("Malformed JSON request body");
    }

    @Test
    @DisplayName("returns bad gateway for generic integration failure")
    void returnsBadGatewayForGenericIntegrationFailure() {
        final IntegrationException exception = new IntegrationException(
                ErrorCode.INTEGRATION_FAILURE, "Unable to retrieve issues from GitLab", "gitlab");

        final ResponseEntity<ErrorResponse> response = handler.handleIntegrationException(exception);
        final ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(body.code()).isEqualTo("INTEGRATION_FAILURE");
        assertThat(body.message()).isEqualTo("Unable to retrieve issues from GitLab");
        assertThat(body.details()).isEmpty();
    }

    @Test
    @DisplayName("returns not found for integration not found")
    void returnsNotFoundForIntegrationNotFound() {
        final IntegrationException exception =
                new IntegrationException(ErrorCode.INTEGRATION_NOT_FOUND, "GitLab issues operation failed", "gitlab");

        final ResponseEntity<ErrorResponse> response = handler.handleIntegrationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("returns forbidden for integration forbidden")
    void returnsForbiddenForIntegrationForbidden() {
        final IntegrationException exception =
                new IntegrationException(ErrorCode.INTEGRATION_FORBIDDEN, "GitLab issues operation failed", "gitlab");

        final ResponseEntity<ErrorResponse> response = handler.handleIntegrationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("returns unauthorized for integration authentication failure")
    void returnsUnauthorizedForIntegrationAuthenticationFailure() {
        final IntegrationException exception = new IntegrationException(
                ErrorCode.INTEGRATION_AUTHENTICATION_FAILED, "GitLab issues operation failed", "gitlab");

        final ResponseEntity<ErrorResponse> response = handler.handleIntegrationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("returns too many requests for integration rate limited")
    void returnsTooManyRequestsForIntegrationRateLimited() {
        final IntegrationException exception = new IntegrationException(
                ErrorCode.INTEGRATION_RATE_LIMITED, "GitLab issues operation failed", "gitlab");

        final ResponseEntity<ErrorResponse> response = handler.handleIntegrationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("returns validation error for path variable type mismatch")
    void returnsValidationErrorForPathVariableTypeMismatch() {
        final MethodArgumentTypeMismatchException exception =
                new MethodArgumentTypeMismatchException("abc", Long.class, "issueId", null, null);

        final ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentTypeMismatch(exception);
        final ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.message()).isEqualTo("Request validation failed");
        assertThat(body.details()).containsExactly("issueId must be a positive number");
    }

    @Test
    @DisplayName("returns internal error for unexpected exception")
    void returnsInternalErrorForUnexpectedException() {
        final ResponseEntity<ErrorResponse> response = handler.handleUnhandledException(new RuntimeException("boom"));
        final ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.message()).isEqualTo("Unexpected error");
        assertThat(body.details()).isEmpty();
    }
}
