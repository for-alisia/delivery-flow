package com.gitlabflow.floworchestrator.orchestration.common.model;

import lombok.Builder;

@Builder
public record User(long id, String username, String name) {}
