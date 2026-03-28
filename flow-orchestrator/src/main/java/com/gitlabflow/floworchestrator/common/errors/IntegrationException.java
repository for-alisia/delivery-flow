package com.gitlabflow.floworchestrator.common.errors;

import java.util.Optional;

import lombok.Getter;

@Getter
public class IntegrationException extends RuntimeException {

    private final ErrorCode code;
    private final String source;
    private final Integer status;
    private final Long retryAfterSeconds;

    public IntegrationException(ErrorCode code, String source, Integer status, String message, Long retryAfterSeconds, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.source = source;
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Optional<Long> getRetryAfterSeconds() {
        return Optional.ofNullable(retryAfterSeconds);
    }
}
