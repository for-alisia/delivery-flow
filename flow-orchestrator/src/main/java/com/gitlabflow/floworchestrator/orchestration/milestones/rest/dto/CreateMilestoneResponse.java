package com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto;

import lombok.Builder;

@Builder
public record CreateMilestoneResponse(long milestoneId, String title) {}
