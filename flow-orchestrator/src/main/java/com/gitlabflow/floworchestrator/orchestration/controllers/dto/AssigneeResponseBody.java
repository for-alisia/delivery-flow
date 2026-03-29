package com.gitlabflow.floworchestrator.orchestration.controllers.dto;

public record AssigneeResponseBody(
        String username,
        String name,
        String webUrl
) {
}
