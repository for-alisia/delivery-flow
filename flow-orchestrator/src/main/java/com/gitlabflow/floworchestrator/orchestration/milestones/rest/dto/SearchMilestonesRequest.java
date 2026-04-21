package com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto;

import jakarta.validation.Valid;
import lombok.Builder;

@Builder
public record SearchMilestonesRequest(@Valid MilestoneFiltersRequest filters) {}
