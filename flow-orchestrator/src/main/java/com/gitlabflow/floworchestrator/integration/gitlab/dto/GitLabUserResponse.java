package com.gitlabflow.floworchestrator.integration.gitlab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitLabUserResponse(
        String username,
        String name,
        @JsonProperty("web_url") String webUrl
) {
}
