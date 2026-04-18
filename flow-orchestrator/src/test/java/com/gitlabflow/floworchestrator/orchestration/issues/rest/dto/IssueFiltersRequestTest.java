package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IssueFiltersRequestTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("normalizes null filter lists to empty immutable lists")
    void normalizesNullListsToEmptyImmutableLists() {
        final IssueFiltersRequest request = new IssueFiltersRequest("opened", null, null, null, null);

        assertThat(request.labels()).isEmpty();
        assertThat(request.assignee()).isEmpty();
        assertThat(request.milestone()).isEmpty();
        assertThat(request.audit()).isEmpty();
        final List<String> labels = request.labels();
        assertThatThrownBy(() -> labels.add("bug")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("filters out null values and detaches from mutable input lists")
    void filtersNullsAndDetachesFromMutableInputs() {
        final List<String> labels = new ArrayList<>(List.of("bug", "backend"));
        labels.add(null);
        final List<String> audit = new ArrayList<>(Arrays.asList("label", null));

        final IssueFiltersRequest request = new IssueFiltersRequest(
                "all", labels, new ArrayList<>(List.of("alice")), new ArrayList<>(List.of("M1")), audit);

        labels.clear();
        audit.clear();

        assertThat(request.labels()).containsExactly("bug", "backend");
        assertThat(request.assignee()).containsExactly("alice");
        assertThat(request.milestone()).containsExactly("M1");
        assertThat(request.audit()).containsExactly("label");
        assertThatThrownBy(() -> request.assignee().add("bob")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> request.audit().add("label")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("treats null-only audit values as empty")
    void treatsNullOnlyAuditValuesAsEmpty() {
        final IssueFiltersRequest request = new IssueFiltersRequest(
                "opened", List.of("bug"), List.of("alice"), List.of("M1"), new ArrayList<>(Arrays.asList((String)
                        null)));

        assertThat(request.audit()).isEmpty();
    }

    @Test
    @DisplayName("rejects unsupported audit values")
    void rejectsUnsupportedAuditValues() {
        final IssueFiltersRequest request = new IssueFiltersRequest(
                "opened", List.of("bug"), List.of("alice"), List.of("M1"), List.of("milestone"));

        final var violations = VALIDATOR.validate(request);

        assertThat(violations).hasSize(1);
        final var violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).contains("audit[0]");
        assertThat(violation.getMessage()).isEqualTo("must be one of [label]");
    }
}
