package com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MilestoneFiltersRequestTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("normalizes null milestoneIds to immutable empty list")
    void normalizesNullMilestoneIdsToImmutableEmptyList() {
        final MilestoneFiltersRequest request = new MilestoneFiltersRequest("active", null, null);

        assertThat(request.milestoneIds()).isEmpty();
        assertThatThrownBy(() -> request.milestoneIds().add(1L)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("defensively copies milestoneIds and preserves null elements")
    void defensivelyCopiesMilestoneIdsAndPreservesNullElements() {
        final List<Long> milestoneIds = new ArrayList<>(Arrays.asList(1L, null, 2L));

        final MilestoneFiltersRequest request = new MilestoneFiltersRequest("all", " release ", milestoneIds);

        milestoneIds.clear();

        assertThat(request.milestoneIds()).containsExactly(1L, null, 2L);
        assertThatThrownBy(() -> request.milestoneIds().add(3L)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("rejects unsupported state values")
    void rejectsUnsupportedStateValues() {
        final MilestoneFiltersRequest request = new MilestoneFiltersRequest("opened", null, List.of());

        final var violations = VALIDATOR.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("must be one of: active, closed, all");
    }

    @Test
    @DisplayName("rejects blank titleSearch")
    void rejectsBlankTitleSearch() {
        final MilestoneFiltersRequest request = new MilestoneFiltersRequest("active", "   ", List.of());

        final var violations = VALIDATOR.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("must not be blank");
    }
}
