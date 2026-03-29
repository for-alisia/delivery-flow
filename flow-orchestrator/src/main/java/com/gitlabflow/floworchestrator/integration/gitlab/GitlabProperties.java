package com.gitlabflow.floworchestrator.integration.gitlab;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "app.gitlab")
public record GitlabProperties(
        @NotBlank String url,
        @NotBlank String token
) {

        /**
         * Accepts either a host-style GitLab URL or a project-style URL and normalizes it
         * to a Feign-safe API base host: scheme://host[:port].
         */
        public String apiBaseUrl() {
                final URI configuredUri;
                try {
                        configuredUri = URI.create(url);
                } catch (IllegalArgumentException ex) {
                        throw new IllegalArgumentException("app.gitlab.url must be a valid absolute URL", ex);
                }

                if (configuredUri.getScheme() == null || configuredUri.getHost() == null) {
                        throw new IllegalArgumentException("app.gitlab.url must be a valid absolute URL");
                }

                final StringBuilder normalized = new StringBuilder()
                                .append(configuredUri.getScheme())
                                .append("://")
                                .append(configuredUri.getHost());

                if (configuredUri.getPort() != -1) {
                        normalized.append(':').append(configuredUri.getPort());
                }

                return normalized.toString();
        }
}
