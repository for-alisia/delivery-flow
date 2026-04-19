package com.gitlabflow.floworchestrator.orchestration.common.rest.dto;

import lombok.Builder;

@Builder
public record UserDto(long id, String username, String name) {}
