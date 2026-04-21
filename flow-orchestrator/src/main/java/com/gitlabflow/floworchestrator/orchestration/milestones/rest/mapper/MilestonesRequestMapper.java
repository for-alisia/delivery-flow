package com.gitlabflow.floworchestrator.orchestration.milestones.rest.mapper;

import static java.util.Optional.ofNullable;

import com.gitlabflow.floworchestrator.orchestration.milestones.model.MilestoneState;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.SearchMilestonesInput;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.MilestoneFiltersRequest;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.SearchMilestonesRequest;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MilestonesRequestMapper {

    public SearchMilestonesInput toSearchMilestonesInput(final SearchMilestonesRequest request) {
        final MilestoneFiltersRequest filters =
                ofNullable(request).map(SearchMilestonesRequest::filters).orElse(null);

        final MilestoneState state = ofNullable(filters)
                .map(MilestoneFiltersRequest::state)
                .map(MilestoneState::fromValue)
                .orElse(MilestoneState.ACTIVE);

        final String titleSearch = ofNullable(filters)
                .map(MilestoneFiltersRequest::titleSearch)
                .map(String::trim)
                .orElse(null);

        final List<Long> milestoneIds =
                ofNullable(filters).map(MilestoneFiltersRequest::milestoneIds).orElse(List.of());

        return SearchMilestonesInput.builder()
                .state(state)
                .titleSearch(titleSearch)
                .milestoneIds(milestoneIds)
                .build();
    }
}
