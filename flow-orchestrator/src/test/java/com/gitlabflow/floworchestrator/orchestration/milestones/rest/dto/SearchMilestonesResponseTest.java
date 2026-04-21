package com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SearchMilestonesResponseTest {

    @Test
    @DisplayName("normalizes null milestones to immutable empty list")
    void normalizesNullMilestonesToImmutableEmptyList() {
        final SearchMilestonesResponse response = new SearchMilestonesResponse(null);

        assertThat(response.milestones()).isEmpty();
        assertThatThrownBy(() -> response.milestones().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("defensively copies milestones")
    void defensivelyCopiesMilestones() {
        final List<MilestoneDto> milestones = new ArrayList<>(
                List.of(new MilestoneDto(12L, 3L, "Release 1.0", "Version", "2026-05-01", "2026-05-15", "active")));

        final SearchMilestonesResponse response = new SearchMilestonesResponse(milestones);

        milestones.clear();

        assertThat(response.milestones()).hasSize(1);
        assertThat(response.milestones().getFirst().title()).isEqualTo("Release 1.0");
        assertThatThrownBy(() -> response.milestones().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }
}
