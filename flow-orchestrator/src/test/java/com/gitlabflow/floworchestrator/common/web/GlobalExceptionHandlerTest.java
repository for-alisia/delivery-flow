package com.gitlabflow.floworchestrator.common.web;

import com.gitlabflow.floworchestrator.common.errors.ErrorCode;
import com.gitlabflow.floworchestrator.common.errors.IntegrationException;
import com.gitlabflow.floworchestrator.common.errors.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.List;

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

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Request validation failed");
        assertThat(response.getBody().details())
                .containsExactly("pagination.perPage must be less than or equal to 100");
    }

    @Test
    @DisplayName("returns validation error for malformed json")
    void returnsValidationErrorForMalformedJson() {
        final HttpMessageNotReadableException exception = new HttpMessageNotReadableException("bad json");

        final ResponseEntity<ErrorResponse> response = handler.handleMalformedJson(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Request validation failed");
        assertThat(response.getBody().details()).containsExactly("Malformed JSON request body");
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

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTEGRATION_FAILURE");
        assertThat(response.getBody().message()).isEqualTo("Unable to retrieve issues from GitLab");
        assertThat(response.getBody().details()).isEmpty();
    }

    @Test
    @DisplayName("returns internal error for unexpected exception")
    void returnsInternalErrorForUnexpectedException() {
        final ResponseEntity<ErrorResponse> response = handler.handleUnhandledException(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Unexpected error");
        assertThat(response.getBody().details()).isEmpty();
    }
}
