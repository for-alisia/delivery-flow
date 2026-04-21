package com.gitlabflow.floworchestrator.integration.gitlab.milestones.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import org.springframework.lang.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record GitLabMilestoneResponse(
        long id,
        long iid,
        String title,
        @Nullable String description,
        @JsonProperty("start_date") @Nullable String startDate,
        @JsonProperty("due_date") @Nullable String dueDate,
        String state) {}
