package com.gitlabflow.floworchestrator.orchestration.milestones.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitlabflow.floworchestrator.orchestration.milestones.model.MilestoneState;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.SearchMilestonesInput;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.MilestoneFiltersRequest;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.SearchMilestonesRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MilestonesRequestMapperTest {

    private final MilestonesRequestMapper mapper = new MilestonesRequestMapper();

    @Test
    @DisplayName("maps null body to ACTIVE defaults")
    void mapsNullBodyToActiveDefaults() {
        final SearchMilestonesInput input = mapper.toSearchMilestonesInput(null);

        assertThat(input.state()).isEqualTo(MilestoneState.ACTIVE);
        assertThat(input.titleSearch()).isNull();
        assertThat(input.milestoneIds()).isEmpty();
    }

    @Test
    @DisplayName("maps missing filters to ACTIVE defaults")
    void mapsMissingFiltersToActiveDefaults() {
        final SearchMilestonesInput input = mapper.toSearchMilestonesInput(new SearchMilestonesRequest(null));

        assertThat(input.state()).isEqualTo(MilestoneState.ACTIVE);
        assertThat(input.titleSearch()).isNull();
        assertThat(input.milestoneIds()).isEmpty();
    }

    @Test
    @DisplayName("maps filters and strips titleSearch")
    void mapsFiltersAndStripsTitleSearch() {
        final SearchMilestonesRequest request =
                new SearchMilestonesRequest(new MilestoneFiltersRequest("all", "  release  ", List.of(1L, 2L, 3L)));

        final SearchMilestonesInput input = mapper.toSearchMilestonesInput(request);

        assertThat(input.state()).isEqualTo(MilestoneState.ALL);
        assertThat(input.titleSearch()).isEqualTo("release");
        assertThat(input.milestoneIds()).containsExactly(1L, 2L, 3L);
    }
}
