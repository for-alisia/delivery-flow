package com.gitlabflow.floworchestrator.integration.gitlab.issues.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.lang.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GitLabCreateIssueRequest(
        String title,
        @Nullable String description,
        @Nullable String labels) {}
