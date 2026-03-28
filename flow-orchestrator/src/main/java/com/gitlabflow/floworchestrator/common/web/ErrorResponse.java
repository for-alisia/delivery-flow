package com.gitlabflow.floworchestrator.common.web;

import java.time.Instant;

import com.gitlabflow.floworchestrator.common.errors.ErrorCode;

public record ErrorResponse(
        Instant timestamp,
        int status,
        ErrorCode code,
        String source,
        String message,
        String path,
        String correlationId,
        Long retryAfterSeconds
) {
}
