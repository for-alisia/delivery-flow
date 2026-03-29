package com.gitlabflow.floworchestrator.orchestration.controllers.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ListIssuesRequestBody(
        @Size(max = 1, message = "must contain at most one label")
        List<String> labels,
        String assignee,
        @Positive(message = "must be greater than 0")
        Integer page,
        @Positive(message = "must be greater than 0")
        Integer pageSize
) {
}
