package com.gitlabflow.floworchestrator.orchestration.milestones.model;

import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record Milestone(
        long id,
        long milestoneId,
        String title,
        String state,
        @Nullable String dueDate) {}
