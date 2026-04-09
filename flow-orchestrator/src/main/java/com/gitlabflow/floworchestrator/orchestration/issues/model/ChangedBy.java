package com.gitlabflow.floworchestrator.orchestration.issues.model;

import lombok.Builder;

@Builder
public record ChangedBy(long id, String username, String name) {}
