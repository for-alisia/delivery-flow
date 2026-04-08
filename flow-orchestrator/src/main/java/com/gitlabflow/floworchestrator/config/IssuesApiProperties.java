package com.gitlabflow.floworchestrator.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Builder
@Validated
@ConfigurationProperties(prefix = "app.issues-api")
public record IssuesApiProperties(
        @Min(1) int defaultPageSize, @Min(1) @Max(100) int maxPageSize) {
    public IssuesApiProperties {
        if (defaultPageSize > maxPageSize) {
            throw new IllegalArgumentException(
                    "app.issues-api.default-page-size must be <= app.issues-api.max-page-size");
        }
    }
}
