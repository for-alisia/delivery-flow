package com.gitlabflow.floworchestrator.orchestration.controllers.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListIssuesRequestBodyValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("rejects more than one label")
    void rejectsMoreThanOneLabel() {
        final var requestBody = new ListIssuesRequestBody(List.of("bug", "backend"), null, 1, 40);

        final var violations = validator.validate(requestBody);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("labels");
    }

    @Test
    @DisplayName("rejects non positive page")
    void rejectsNonPositivePage() {
        final var requestBody = new ListIssuesRequestBody(null, null, 0, 40);

        final var violations = validator.validate(requestBody);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("page");
    }

    @Test
    @DisplayName("rejects non positive page size")
    void rejectsNonPositivePageSize() {
        final var requestBody = new ListIssuesRequestBody(null, null, 1, 0);

        final var violations = validator.validate(requestBody);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("pageSize");
    }

    @Test
    @DisplayName("accepts optional fields")
    void acceptsOptionalFields() {
        final var requestBody = new ListIssuesRequestBody(null, null, null, null);

        final var violations = validator.validate(requestBody);

        assertThat(violations).isEmpty();
    }
}