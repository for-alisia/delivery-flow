package com.gitlabflow.floworchestrator.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IssuesApiPropertiesTest {

    @Test
    @DisplayName("accepts fixed contract values for default and max page size")
    void acceptsFixedContractValuesForDefaultAndMaxPageSize() {
        assertThatCode(() -> new IssuesApiProperties(20, 40, validValidationProperties()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects default page size drift from contract")
    void rejectsDefaultPageSizeDriftFromContract() {
        final IssuesApiValidationProperties validationProperties = validValidationProperties();

        assertThatThrownBy(() -> new IssuesApiProperties(21, 40, validationProperties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.issues-api.default-page-size must be 20");
    }

    @Test
    @DisplayName("rejects max page size drift from contract")
    void rejectsMaxPageSizeDriftFromContract() {
        final IssuesApiValidationProperties validationProperties = validValidationProperties();

        assertThatThrownBy(() -> new IssuesApiProperties(20, 41, validationProperties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.issues-api.max-page-size must be 40");
    }

    @Test
    @DisplayName("rejects missing validation settings")
    void rejectsMissingValidationSettings() {
        assertThatThrownBy(() -> new IssuesApiProperties(20, 40, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.issues-api.validation must be configured");
    }

    @Test
    @DisplayName("rejects non positive nested validation values")
    void rejectsNonPositiveNestedValidationValues() {
        assertThatThrownBy(() -> new IssuesApiValidationProperties(0, 255, 1_000_000, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.issues-api.validation.title-min-length must be a positive number");
    }

    @Test
    @DisplayName("rejects title max length lower than title min length")
    void rejectsTitleMaxLengthLowerThanTitleMinLength() {
        assertThatThrownBy(() -> new IssuesApiValidationProperties(10, 9, 1_000_000, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "app.issues-api.validation.title-max-length must be greater than or equal to title-min-length");
    }

    private static IssuesApiValidationProperties validValidationProperties() {
        return new IssuesApiValidationProperties(3, 255, 1_000_000, 10);
    }
}
