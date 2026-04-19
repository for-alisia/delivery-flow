package com.gitlabflow.floworchestrator.integration.gitlab.issues.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import org.springframework.lang.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record GitLabUpdateIssueRequest(
        @Nullable String title,
        @Nullable String description,
        @JsonProperty("add_labels") @Nullable String addLabels,
        @JsonProperty("remove_labels") @Nullable String removeLabels) {}
