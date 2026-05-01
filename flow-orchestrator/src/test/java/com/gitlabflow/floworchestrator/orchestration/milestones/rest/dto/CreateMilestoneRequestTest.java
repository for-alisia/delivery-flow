package com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CreateMilestoneRequestTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("builder carries title description and optional dates")
    void builderCarriesTitleDescriptionAndOptionalDates() {
        final CreateMilestoneRequest request = CreateMilestoneRequest.builder()
                .title("Q2 2026 Delivery")
                .description("Second quarter release cycle")
                .startDate("2026-04-01")
                .dueDate("2026-06-30")
                .build();

        assertThat(request.title()).isEqualTo("Q2 2026 Delivery");
        assertThat(request.description()).isEqualTo("Second quarter release cycle");
        assertThat(request.startDate()).isEqualTo("2026-04-01");
        assertThat(request.dueDate()).isEqualTo("2026-06-30");
    }

    @Test
    @DisplayName("preserves nullable optional fields")
    void preservesNullableOptionalFields() {
        final CreateMilestoneRequest request = new CreateMilestoneRequest("Release v1.0", null, null, null);

        assertThat(request.title()).isEqualTo("Release v1.0");
        assertThat(request.description()).isNull();
        assertThat(request.startDate()).isNull();
        assertThat(request.dueDate()).isNull();
        assertThat(CreateMilestoneRequest.class.isRecord()).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTitleRequests")
    @DisplayName("rejects invalid title values")
    void rejectsInvalidTitleValues(
            final String scenario, final CreateMilestoneRequest request, final String expectedMessage) {
        final var violations = VALIDATOR.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("accepts title length boundary values")
    void acceptsTitleLengthBoundaryValues() {
        final CreateMilestoneRequest minTitleRequest = new CreateMilestoneRequest("abcd", null, null, null);
        final CreateMilestoneRequest maxTitleRequest = new CreateMilestoneRequest("x".repeat(499), null, null, null);

        final var minViolations = VALIDATOR.validate(minTitleRequest);
        final var maxViolations = VALIDATOR.validate(maxTitleRequest);

        assertThat(minViolations).isEmpty();
        assertThat(maxViolations).isEmpty();
    }

    private static Stream<Arguments> invalidTitleRequests() {
        return Stream.of(
                Arguments.of(
                        "missing title", new CreateMilestoneRequest(null, "Scope", null, null), "must not be blank"),
                Arguments.of("blank title", new CreateMilestoneRequest("    ", null, null, null), "must not be blank"),
                Arguments.of(
                        "title shorter than four characters",
                        new CreateMilestoneRequest("abc", null, null, null),
                        "length must be between 4 and 499"),
                Arguments.of(
                        "title longer than four hundred ninety nine characters",
                        new CreateMilestoneRequest("x".repeat(500), null, null, null),
                        "length must be between 4 and 499"));
    }
}
