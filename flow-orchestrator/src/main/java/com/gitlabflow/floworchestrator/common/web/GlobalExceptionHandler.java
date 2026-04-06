package com.gitlabflow.floworchestrator.common.web;

import com.gitlabflow.floworchestrator.common.error.ErrorCode;
import com.gitlabflow.floworchestrator.common.error.IntegrationException;
import com.gitlabflow.floworchestrator.common.error.ValidationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String REQUEST_VALIDATION_FAILED = "Request validation failed";

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(final ValidationException exception) {
        log.warn(
                "Validation failure message={} detailCount={}",
                exception.getMessage(),
                exception.details().size());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        ErrorCode.VALIDATION_ERROR.name(), exception.getMessage(), exception.details()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(final MethodArgumentNotValidException exception) {
        final List<String> details = exception.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof final FieldError fieldError) {
                        return fieldError.getField() + " " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .toList();

        log.warn("Validation failure message=Request validation failed detailCount={}", details.size());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ErrorCode.VALIDATION_ERROR.name(), REQUEST_VALIDATION_FAILED, details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(final HttpMessageNotReadableException exception) {
        log.warn(
                "Validation failure message=Malformed JSON request body category={}",
                exception.getClass().getSimpleName());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        ErrorCode.VALIDATION_ERROR.name(),
                        REQUEST_VALIDATION_FAILED,
                        List.of("Malformed JSON request body")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            final MethodArgumentTypeMismatchException exception) {
        final String parameterName = exception.getName();
        log.warn("Validation failure message=Request validation failed parameter={}", parameterName);
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        ErrorCode.VALIDATION_ERROR.name(),
                        REQUEST_VALIDATION_FAILED,
                        List.of(parameterName + " must be a positive number")));
    }

    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrationException(final IntegrationException exception) {
        log.warn(
                "Integration failure source={} code={}",
                exception.source(),
                exception.errorCode().name());
        return ResponseEntity.status(mapStatus(exception.errorCode()))
                .body(new ErrorResponse(exception.errorCode().name(), exception.getMessage(), List.of()));
    }

    private HttpStatus mapStatus(final ErrorCode errorCode) {
        return switch (errorCode) {
            case INTEGRATION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INTEGRATION_FORBIDDEN -> HttpStatus.FORBIDDEN;
            case INTEGRATION_AUTHENTICATION_FAILED -> HttpStatus.UNAUTHORIZED;
            case INTEGRATION_RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandledException(final Exception exception) {
        log.error("Unhandled exception category={}", exception.getClass().getSimpleName(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(ErrorCode.INTERNAL_ERROR.name(), "Unexpected error", List.of()));
    }
}
