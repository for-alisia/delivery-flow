package com.gitlabflow.floworchestrator.orchestration.milestones.model;

import java.util.Locale;

public enum MilestoneState {
    ACTIVE("active"),
    CLOSED("closed"),
    ALL("all");

    private final String value;

    MilestoneState(final String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static MilestoneState fromValue(final String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "active" -> ACTIVE;
            case "closed" -> CLOSED;
            case "all" -> ALL;
            default -> throw new IllegalArgumentException("Unsupported milestone state: " + value);
        };
    }
}
