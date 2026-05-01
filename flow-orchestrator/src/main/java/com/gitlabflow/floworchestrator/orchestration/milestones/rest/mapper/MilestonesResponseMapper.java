package com.gitlabflow.floworchestrator.orchestration.milestones.rest.mapper;

import com.gitlabflow.floworchestrator.orchestration.milestones.model.Milestone;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.CreateMilestoneResponse;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.MilestoneDto;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.SearchMilestonesResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MilestonesResponseMapper {

    public SearchMilestonesResponse toSearchMilestonesResponse(final List<Milestone> milestones) {
        final List<MilestoneDto> milestoneDtos = milestones == null
                ? List.of()
                : milestones.stream().map(this::toMilestoneDto).toList();

        return SearchMilestonesResponse.builder().milestones(milestoneDtos).build();
    }

    public CreateMilestoneResponse toCreateMilestoneResponse(final Milestone milestone) {
        return CreateMilestoneResponse.builder()
                .milestoneId(milestone.milestoneId())
                .title(milestone.title())
                .build();
    }

    private MilestoneDto toMilestoneDto(final Milestone milestone) {
        return MilestoneDto.builder()
                .id(milestone.id())
                .milestoneId(milestone.milestoneId())
                .title(milestone.title())
                .description(milestone.description())
                .startDate(milestone.startDate())
                .dueDate(milestone.dueDate())
                .state(milestone.state())
                .build();
    }
}
