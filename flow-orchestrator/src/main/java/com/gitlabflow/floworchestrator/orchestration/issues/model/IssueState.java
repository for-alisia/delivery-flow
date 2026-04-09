package com.gitlabflow.floworchestrator.orchestration.issues.model;

import java.util.Locale;

public enum IssueState {
    OPENED("opened"),
    CLOSED("closed"),
    ALL("all");

    private final String value;

    IssueState(final String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static IssueState fromValue(final String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "opened" -> OPENED;
            case "closed" -> CLOSED;
            case "all" -> ALL;
            default -> throw new IllegalArgumentException("Unsupported issue state: " + value);
        };
    }
}
