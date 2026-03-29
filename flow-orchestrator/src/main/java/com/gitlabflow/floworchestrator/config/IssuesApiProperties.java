package com.gitlabflow.floworchestrator.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.issues-api")
public record IssuesApiProperties(
        @Min(1) int defaultPageSize,
        @Min(1) int maxPageSize
) {
}
