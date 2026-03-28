package com.gitlabflow.floworchestrator.integration.gitlab.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitLabUserDTO(
        @JsonProperty("id") Long id,
        @JsonProperty("username") String username
) {
}
