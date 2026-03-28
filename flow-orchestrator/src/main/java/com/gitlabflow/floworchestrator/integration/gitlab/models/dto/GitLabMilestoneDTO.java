package com.gitlabflow.floworchestrator.integration.gitlab.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitLabMilestoneDTO(
        @JsonProperty("id") Long id,
        @JsonProperty("title") String title
) {
}
