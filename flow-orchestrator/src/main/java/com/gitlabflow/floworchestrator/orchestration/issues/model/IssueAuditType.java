package com.gitlabflow.floworchestrator.orchestration.issues.model;

import java.util.Locale;

public enum IssueAuditType {
    LABEL("label");

    private final String value;

    IssueAuditType(final String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static IssueAuditType fromValue(final String value) {
        if (value == null) {
            throw new IllegalArgumentException("Unsupported issue audit type: null");
        }

        return switch (value.toLowerCase(Locale.ROOT)) {
            case "label" -> LABEL;
            default -> throw new IllegalArgumentException("Unsupported issue audit type: " + value);
        };
    }
}
