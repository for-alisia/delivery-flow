package com.gitlabflow.floworchestrator.orchestration.milestones.model;

import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record CreateMilestoneInput(
        String title,
        @Nullable String description,
        @Nullable String startDate,
        @Nullable String dueDate) {}
