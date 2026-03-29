package com.gitlabflow.floworchestrator.orchestration.controllers.models;

public record ProjectIssueResponseItem(
        Long id,
        Long iid,
        String title,
        String state,
        String webUrl
) {
}
