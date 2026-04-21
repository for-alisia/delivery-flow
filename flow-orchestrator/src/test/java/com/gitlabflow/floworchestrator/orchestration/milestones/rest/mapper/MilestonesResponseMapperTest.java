package com.gitlabflow.floworchestrator.orchestration.milestones.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitlabflow.floworchestrator.orchestration.milestones.model.Milestone;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.SearchMilestonesResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MilestonesResponseMapperTest {

    private final MilestonesResponseMapper mapper = new MilestonesResponseMapper();

    @Test
    @DisplayName("maps empty milestone list to empty response")
    void mapsEmptyMilestoneListToEmptyResponse() {
        final SearchMilestonesResponse response = mapper.toSearchMilestonesResponse(List.of());

        assertThat(response.milestones()).isEmpty();
    }

    @Test
    @DisplayName("maps all milestone fields to shared dto")
    void mapsAllMilestoneFieldsToSharedDto() {
        final Milestone milestone =
                new Milestone(12L, 3L, "Release 1.0", "Version", "2026-05-01", "2026-05-15", "active");

        final SearchMilestonesResponse response = mapper.toSearchMilestonesResponse(List.of(milestone));

        assertThat(response.milestones()).hasSize(1);
        assertThat(response.milestones().getFirst().id()).isEqualTo(12L);
        assertThat(response.milestones().getFirst().milestoneId()).isEqualTo(3L);
        assertThat(response.milestones().getFirst().title()).isEqualTo("Release 1.0");
        assertThat(response.milestones().getFirst().description()).isEqualTo("Version");
        assertThat(response.milestones().getFirst().startDate()).isEqualTo("2026-05-01");
        assertThat(response.milestones().getFirst().dueDate()).isEqualTo("2026-05-15");
        assertThat(response.milestones().getFirst().state()).isEqualTo("active");
    }
}
