package com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto;

import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record MilestoneDto(
        long id,
        long milestoneId,
        String title,
        String state,
        @Nullable String dueDate) {}
