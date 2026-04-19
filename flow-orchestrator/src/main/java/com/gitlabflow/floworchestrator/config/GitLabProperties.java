package com.gitlabflow.floworchestrator.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Builder
@Validated
@ConfigurationProperties(prefix = "app.gitlab")
public record GitLabProperties(
        @NotBlank String url,
        @NotBlank String token,
        @Positive int connectTimeoutSeconds,
        @Positive int readTimeoutSeconds) {}
