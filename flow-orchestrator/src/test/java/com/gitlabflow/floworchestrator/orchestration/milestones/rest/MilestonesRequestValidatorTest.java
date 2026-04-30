package com.gitlabflow.floworchestrator.orchestration.milestones.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import com.gitlabflow.floworchestrator.common.error.ValidationException;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.MilestoneState;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.SearchMilestonesInput;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.CreateMilestoneRequest;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MilestonesRequestValidatorTest {

    private final MilestonesRequestValidator validator = new MilestonesRequestValidator();

    @Test
    @DisplayName("accepts distinct positive milestone ids")
    void acceptsDistinctPositiveMilestoneIds() {
        final SearchMilestonesInput input =
                new SearchMilestonesInput(MilestoneState.ACTIVE, "release", List.of(1L, 2L, 3L));

        assertThatCode(() -> validator.validateSearchInput(input)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects duplicate milestone ids")
    void rejectsDuplicateMilestoneIds() {
        final SearchMilestonesInput input = new SearchMilestonesInput(MilestoneState.ACTIVE, null, List.of(2L, 2L));

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateSearchInput(input));

        assertThat(exception.details()).containsExactly("filters.milestoneIds must not contain duplicate values");
    }

    @Test
    @DisplayName("rejects zero and negative milestone ids")
    void rejectsZeroAndNegativeMilestoneIds() {
        final SearchMilestonesInput input = new SearchMilestonesInput(MilestoneState.ACTIVE, null, List.of(0L, -1L));

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateSearchInput(input));

        assertThat(exception.details())
                .containsExactly(
                        "filters.milestoneIds[0] must be a positive integer",
                        "filters.milestoneIds[1] must be a positive integer");
    }

    @Test
    @DisplayName("rejects null milestone id elements")
    void rejectsNullMilestoneIdElements() {
        final SearchMilestonesInput input =
                new SearchMilestonesInput(MilestoneState.ACTIVE, null, Arrays.asList(1L, null, 3L));

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateSearchInput(input));

        assertThat(exception.details()).containsExactly("filters.milestoneIds[1] must not be null");
    }

    @Test
    @DisplayName("accepts create request when title is absent and dates are valid")
    void acceptsCreateRequestWhenTitleIsAbsentAndDatesAreValid() {
        final CreateMilestoneRequest request = new CreateMilestoneRequest(null, "Scope", "2026-06-30", "2026-04-01");

        assertThatCode(() -> validator.validateCreateRequest(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects create request with invalid startDate format")
    void rejectsCreateRequestWithInvalidStartDateFormat() {
        final CreateMilestoneRequest request =
                new CreateMilestoneRequest("Release v1.0", null, "2026-06-30", "2026/04/01");

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateCreateRequest(request));

        assertThat(exception.details()).containsExactly("startDate must be a valid ISO date (YYYY-MM-DD)");
    }

    @Test
    @DisplayName("rejects create request with invalid dueDate format")
    void rejectsCreateRequestWithInvalidDueDateFormat() {
        final CreateMilestoneRequest request =
                new CreateMilestoneRequest("Release v1.0", null, "30-06-2026", "2026-04-01");

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateCreateRequest(request));

        assertThat(exception.details()).containsExactly("dueDate must be a valid ISO date (YYYY-MM-DD)");
    }

    @Test
    @DisplayName("rejects create request when dueDate equals startDate")
    void rejectsCreateRequestWhenDueDateEqualsStartDate() {
        final CreateMilestoneRequest request =
                new CreateMilestoneRequest("Release v1.0", null, "2026-06-30", "2026-06-30");

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateCreateRequest(request));

        assertThat(exception.details()).containsExactly("dueDate must be after startDate");
    }

    @Test
    @DisplayName("rejects create request when dueDate is before startDate")
    void rejectsCreateRequestWhenDueDateIsBeforeStartDate() {
        final CreateMilestoneRequest request =
                new CreateMilestoneRequest("Release v1.0", null, "2026-04-01", "2026-06-30");

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateCreateRequest(request));

        assertThat(exception.details()).containsExactly("dueDate must be after startDate");
    }
}
