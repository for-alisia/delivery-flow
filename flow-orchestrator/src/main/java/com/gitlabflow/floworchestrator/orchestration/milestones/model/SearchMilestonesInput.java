package com.gitlabflow.floworchestrator.orchestration.milestones.model;

import com.gitlabflow.floworchestrator.common.util.ImmutableListCopies;
import java.util.List;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record SearchMilestonesInput(
        MilestoneState state, @Nullable String titleSearch, List<Long> milestoneIds) {

    public SearchMilestonesInput {
        state = state == null ? MilestoneState.ACTIVE : state;
        milestoneIds = ImmutableListCopies.copyPreservingNullsOrEmpty(milestoneIds);
    }

    @Override
    public List<Long> milestoneIds() {
        return ImmutableListCopies.copyPreservingNullsOrEmpty(milestoneIds);
    }
}
