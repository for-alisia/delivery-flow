package com.gitlabflow.floworchestrator.orchestration.milestones.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import com.gitlabflow.floworchestrator.common.error.ValidationException;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.MilestoneState;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.SearchMilestonesInput;
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
}
