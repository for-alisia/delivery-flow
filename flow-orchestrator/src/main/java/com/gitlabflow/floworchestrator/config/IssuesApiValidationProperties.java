package com.gitlabflow.floworchestrator.config;

import jakarta.validation.constraints.Min;
import lombok.Builder;

@Builder
public record IssuesApiValidationProperties(
        @Min(1) int titleMinLength,
        @Min(1) int titleMaxLength,
        @Min(1) int descriptionMaxLength,
        @Min(1) int maxLabelsPerRequestField) {

    public IssuesApiValidationProperties {
        requirePositive("app.issues-api.validation.title-min-length", titleMinLength);
        requirePositive("app.issues-api.validation.title-max-length", titleMaxLength);
        requirePositive("app.issues-api.validation.description-max-length", descriptionMaxLength);
        requirePositive("app.issues-api.validation.max-labels-per-request-field", maxLabelsPerRequestField);

        if (titleMaxLength < titleMinLength) {
            throw new IllegalArgumentException(
                    "app.issues-api.validation.title-max-length must be greater than or equal to title-min-length");
        }
    }

    private static void requirePositive(final String propertyName, final int value) {
        if (value < 1) {
            throw new IllegalArgumentException(propertyName + " must be a positive number");
        }
    }
}
