package com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record SearchMilestonesResponse(List<MilestoneDto> milestones) {

    public SearchMilestonesResponse {
        milestones = milestones == null ? List.of() : List.copyOf(milestones);
    }
}
