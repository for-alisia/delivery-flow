package com.gitlabflow.floworchestrator.common.errors;

import java.util.List;

public class ValidationException extends RuntimeException {

    private final List<String> details;

    public ValidationException(final String message, final List<String> details) {
        super(message);
        this.details = List.copyOf(details);
    }

    public List<String> details() {
        return details;
    }
}
