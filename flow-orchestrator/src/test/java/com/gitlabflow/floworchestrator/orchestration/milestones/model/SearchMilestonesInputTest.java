package com.gitlabflow.floworchestrator.orchestration.milestones.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SearchMilestonesInputTest {

    @Test
    @DisplayName("defaults null state and milestoneIds")
    void defaultsNullStateAndMilestoneIds() {
        final SearchMilestonesInput input = new SearchMilestonesInput(null, null, null);

        assertThat(input.state()).isEqualTo(MilestoneState.ACTIVE);
        assertThat(input.titleSearch()).isNull();
        assertThat(input.milestoneIds()).isEmpty();
    }

    @Test
    @DisplayName("defensively copies milestoneIds and keeps null elements for validator")
    void defensivelyCopiesMilestoneIdsAndKeepsNullElementsForValidator() {
        final List<Long> milestoneIds = new ArrayList<>(Arrays.asList(1L, null, 2L));

        final SearchMilestonesInput input = new SearchMilestonesInput(MilestoneState.ALL, "release", milestoneIds);

        milestoneIds.clear();

        assertThat(input.state()).isEqualTo(MilestoneState.ALL);
        assertThat(input.titleSearch()).isEqualTo("release");
        assertThat(input.milestoneIds()).containsExactly(1L, null, 2L);
        assertThatThrownBy(() -> input.milestoneIds().add(3L)).isInstanceOf(UnsupportedOperationException.class);
    }
}
