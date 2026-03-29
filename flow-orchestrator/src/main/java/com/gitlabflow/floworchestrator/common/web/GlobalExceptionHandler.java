package com.gitlabflow.floworchestrator.common.web;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;

import com.gitlabflow.floworchestrator.common.errors.ErrorCode;
import com.gitlabflow.floworchestrator.common.errors.IntegrationException;
import com.gitlabflow.floworchestrator.common.errors.ValidationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for REST API errors.
 *
 * Catches exceptions across all controllers and returns standardized error responses
 * with appropriate HTTP status codes.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ValidationException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleValidation(
            RuntimeException ex,
            WebRequest request) {
        log.warn("Validation/config error: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION, "validation", ex.getMessage(), request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex,
            WebRequest request) {
        log.warn("Malformed request payload: {}", ex.getMessage());
        return buildError(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION,
                "validation",
                "Request body is malformed or invalid.",
                request,
                null
        );
    }

    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ErrorResponse> handleIntegration(
            IntegrationException ex,
            WebRequest request) {
        HttpStatus status = mapIntegrationStatus(ex.getCode());
        log.warn("Integration error from {} status={} code={}: {}", ex.getSource(), ex.getStatus(), ex.getCode(), ex.getMessage());
        return buildError(status, ex.getCode(), ex.getSource(), ex.getMessage(), request, ex.getRetryAfterSeconds().orElse(null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex,
            WebRequest request) {
        log.error("Unexpected error", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.UNKNOWN, "unknown",
                "An unexpected error occurred. Please try again later.", request, null);
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status,
                                                     ErrorCode code,
                                                     String source,
                                                     String message,
                                                     WebRequest request,
                                                     Long retryAfterSeconds) {
        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                status.value(),
                code,
                source,
                message,
                request.getDescription(false).replace("uri=", ""),
                null,
                retryAfterSeconds
        );
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (retryAfterSeconds != null) {
            builder.header("Retry-After", String.valueOf(retryAfterSeconds));
        }
        return builder.body(error);
    }

    private HttpStatus mapIntegrationStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case INTEGRATION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INTEGRATION_RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case INTEGRATION_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }
}
