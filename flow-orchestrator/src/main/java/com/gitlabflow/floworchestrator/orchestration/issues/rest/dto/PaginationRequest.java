package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record PaginationRequest(
        @Positive Integer page, @Positive Integer perPage) {}
