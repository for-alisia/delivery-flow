package com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record CreateMilestoneRequest(
        @NotBlank(message = "must not be blank") @Size(min = 4, max = 499, message = "length must be between 4 and 499")
        String title,

        @Nullable String description,
        @Nullable String dueDate,
        @Nullable String startDate) {}
