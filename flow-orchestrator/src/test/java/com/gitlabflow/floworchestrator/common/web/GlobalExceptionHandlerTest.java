package com.gitlabflow.floworchestrator.common.web;

import com.gitlabflow.floworchestrator.common.error.ErrorCode;
import com.gitlabflow.floworchestrator.common.error.IntegrationException;
import com.gitlabflow.floworchestrator.common.error.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("returns validation error for business validation exception")
    void returnsValidationErrorForValidationException() {
        final ValidationException exception = new ValidationException(
                "Request validation failed",
                List.of("pagination.perPage must be less than or equal to 100")
        );

        final ResponseEntity<ErrorResponse> response = handler.handleValidationException(exception);
        final ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.message()).isEqualTo("Request validation failed");
        assertThat(body.details())
                .containsExactly("pagination.perPage must be less than or equal to 100");
    }

    @Test
    @DisplayName("returns validation error for malformed json")
    void returnsValidationErrorForMalformedJson() {
        final HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "bad json",
                new MockHttpInputMessage(new byte[0])
        );

        final ResponseEntity<ErrorResponse> response = handler.handleMalformedJson(exception);
        final ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.message()).isEqualTo("Request validation failed");
        assertThat(body.details()).containsExactly("Malformed JSON request body");
    }

    @Test
    @DisplayName("returns bad gateway for integration exception")
    void returnsBadGatewayForIntegrationException() {
        final IntegrationException exception = new IntegrationException(
                ErrorCode.INTEGRATION_FAILURE,
                "Unable to retrieve issues from GitLab",
                "gitlab"
        );

        final ResponseEntity<ErrorResponse> response = handler.handleIntegrationException(exception);
        final ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(body.code()).isEqualTo("INTEGRATION_FAILURE");
        assertThat(body.message()).isEqualTo("Unable to retrieve issues from GitLab");
        assertThat(body.details()).isEmpty();
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
