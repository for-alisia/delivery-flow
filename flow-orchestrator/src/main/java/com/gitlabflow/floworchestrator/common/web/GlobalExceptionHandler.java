package com.gitlabflow.floworchestrator.common.web;

import com.gitlabflow.floworchestrator.common.error.ErrorCode;
import com.gitlabflow.floworchestrator.common.error.IntegrationException;
import com.gitlabflow.floworchestrator.common.error.ValidationException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Objects;
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
                .body(ErrorResponse.builder()
                        .code(ErrorCode.VALIDATION_ERROR.name())
                        .message(exception.getMessage())
                        .details(exception.details())
                        .build());
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
                .body(ErrorResponse.builder()
                        .code(ErrorCode.VALIDATION_ERROR.name())
                        .message(REQUEST_VALIDATION_FAILED)
                        .details(details)
                        .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            final ConstraintViolationException exception) {
        final List<String> details = exception.getConstraintViolations().stream()
                .map(jakarta.validation.ConstraintViolation::getMessage)
                .toList();

        log.warn("Validation failure message=Request validation failed detailCount={}", details.size());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                        .code(ErrorCode.VALIDATION_ERROR.name())
                        .message(REQUEST_VALIDATION_FAILED)
                        .details(details)
                        .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(final HttpMessageNotReadableException exception) {
        log.warn(
                "Validation failure message=Malformed JSON request body category={}",
                exception.getClass().getSimpleName());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                        .code(ErrorCode.VALIDATION_ERROR.name())
                        .message(REQUEST_VALIDATION_FAILED)
                        .details(List.of("Malformed JSON request body"))
                        .build());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            final MethodArgumentTypeMismatchException exception) {
        final String parameterName = exception.getName();
        log.warn("Validation failure message=Request validation failed parameter={}", parameterName);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                        .code(ErrorCode.VALIDATION_ERROR.name())
                        .message(REQUEST_VALIDATION_FAILED)
                        .details(List.of(parameterName + " must be a positive number"))
                        .build());
    }

    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrationException(final IntegrationException exception) {
        log.warn(
                "Integration failure source={} code={}",
                exception.source(),
                exception.errorCode().name());
        return ResponseEntity.status(Objects.requireNonNull(mapStatus(exception.errorCode())))
                .body(ErrorResponse.builder()
                        .code(exception.errorCode().name())
                        .message(exception.getMessage())
                        .details(List.of())
                        .build());
    }

    private HttpStatus mapStatus(final ErrorCode errorCode) {
        return switch (errorCode) {
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
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
                .body(ErrorResponse.builder()
                        .code(ErrorCode.INTERNAL_ERROR.name())
                        .message("Unexpected error")
                        .details(List.of())
                        .build());
    }
}
