package com.gitlabflow.floworchestrator.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Builder
@Validated
@ConfigurationProperties(prefix = "app.issues-api")
public record IssuesApiProperties(
        @Min(1) int defaultPageSize,
        @Min(1) @Max(40) int maxPageSize,
        @Valid @NotNull IssuesApiValidationProperties validation) {

    private static final int DEFAULT_PAGE_SIZE_CONTRACT = 20;
    private static final int MAX_PAGE_SIZE_CONTRACT = 40;

    public IssuesApiProperties {
        if (defaultPageSize != DEFAULT_PAGE_SIZE_CONTRACT) {
            throw new IllegalArgumentException("app.issues-api.default-page-size must be 20");
        }

        if (maxPageSize != MAX_PAGE_SIZE_CONTRACT) {
            throw new IllegalArgumentException("app.issues-api.max-page-size must be 40");
        }

        if (validation == null) {
            throw new IllegalArgumentException("app.issues-api.validation must be configured");
        }
    }
}
