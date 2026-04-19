package com.gitlabflow.floworchestrator.orchestration.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum ChangeField {
    LABEL("label");

    private final String value;

    ChangeField(final String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ChangeField fromValue(final String value) {
        return Arrays.stream(values())
                .filter(field ->
                        field.value.equalsIgnoreCase(value) || field.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported ChangeField value: " + value));
    }
}
