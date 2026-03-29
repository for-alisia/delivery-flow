package com.gitlabflow.floworchestrator.common.errors;

public class IntegrationException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String source;

    public IntegrationException(final ErrorCode errorCode, final String message, final String source) {
        super(message);
        this.errorCode = errorCode;
        this.source = source;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public String source() {
        return source;
    }
}
