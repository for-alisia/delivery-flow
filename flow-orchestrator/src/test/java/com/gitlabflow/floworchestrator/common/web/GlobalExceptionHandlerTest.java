package com.gitlabflow.floworchestrator.common.web;

import com.gitlabflow.floworchestrator.common.errors.ErrorCode;
import com.gitlabflow.floworchestrator.common.errors.IntegrationException;
import com.gitlabflow.floworchestrator.common.errors.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("maps validation exception to bad request with details")
    void mapsValidationExceptionToBadRequestWithDetails() {
        final var exception = new ValidationException("Invalid payload", List.of("labels must contain at most one value"));

        final var response = globalExceptionHandler.handleValidationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(ErrorCode.VALIDATION_ERROR.name(), "Invalid payload", List.of("labels must contain at most one value")));
    }

    @Test
    @DisplayName("maps integration exception to bad gateway without leaking internals")
    void mapsIntegrationExceptionToBadGatewayWithoutLeakingInternals() {
        final var exception = new IntegrationException(ErrorCode.INTEGRATION_FAILURE, "Provider call failed", "gitlab");

        final var response = globalExceptionHandler.handleIntegrationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(ErrorCode.INTEGRATION_FAILURE.name(), "Provider call failed", List.of()));
    }
}