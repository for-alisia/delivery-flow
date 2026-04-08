package com.gitlabflow.floworchestrator.integration.gitlab.issues.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.Builder;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record GitLabLabelEventResponse(
        long id,
        GitLabUserDetail user,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        GitLabLabelDetail label,
        String action) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Builder
    public record GitLabUserDetail(long id, String username, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Builder
    public record GitLabLabelDetail(long id, String name) {}
}
