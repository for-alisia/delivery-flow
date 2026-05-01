package com.gitlabflow.floworchestrator.integration.gitlab.milestones.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import org.springframework.lang.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record GitLabCreateMilestoneRequest(
        String title,
        @Nullable String description,
        @JsonProperty("start_date") @Nullable String startDate,
        @JsonProperty("due_date") @Nullable String dueDate) {}
