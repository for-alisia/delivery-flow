package com.gitlabflow.floworchestrator.orchestration.milestones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gitlabflow.floworchestrator.orchestration.milestones.model.Milestone;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.MilestoneState;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.SearchMilestonesInput;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MilestonesServiceTest {

    @Mock
    private MilestonesPort milestonesPort;

    @Test
    @DisplayName("delegates milestone search and returns results")
    void delegatesMilestoneSearchAndReturnsResults() {
        final MilestonesService service = new MilestonesService(milestonesPort);
        final SearchMilestonesInput input = new SearchMilestonesInput(MilestoneState.ALL, "release", List.of(1L, 2L));
        final List<Milestone> expected =
                List.of(new Milestone(12L, 3L, "Release 1.0", "Version", "2026-05-01", "2026-05-15", "active"));
        when(milestonesPort.searchMilestones(input)).thenReturn(expected);

        final List<Milestone> actual = service.searchMilestones(input);

        assertThat(actual).isEqualTo(expected);
        verify(milestonesPort).searchMilestones(input);
    }

    @Test
    @DisplayName("returns empty list when adapter returns no milestones")
    void returnsEmptyListWhenAdapterReturnsNoMilestones() {
        final MilestonesService service = new MilestonesService(milestonesPort);
        final SearchMilestonesInput input = new SearchMilestonesInput(MilestoneState.ACTIVE, null, List.of());
        when(milestonesPort.searchMilestones(input)).thenReturn(List.of());

        final List<Milestone> actual = service.searchMilestones(input);

        assertThat(actual).isEmpty();
        verify(milestonesPort).searchMilestones(input);
    }
}
