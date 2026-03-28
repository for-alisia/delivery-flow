package com.gitlabflow.floworchestrator.integration.gitlab;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.gitlab")
public record GitlabProperties(String url, String token) {

    public GitlabProperties {
        if (isBlank(url)) {
            throw new IllegalArgumentException("app.gitlab.url must not be blank");
        }
        if (isBlank(token)) {
            throw new IllegalArgumentException("app.gitlab.token must not be blank");
        }
    }

    public String getBaseUrl() {
        URI uri = URI.create(url.trim());
        return uri.getScheme() + "://" + uri.getAuthority();
    }

    public String getEncodedProjectPath() {
        URI uri = URI.create(url.trim());
        String path = uri.getPath();
        if (isBlank(path) || "/".equals(path)) {
            throw new IllegalArgumentException("app.gitlab.url must include project path");
        }

        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        if (normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }

        if (isBlank(normalizedPath)) {
            throw new IllegalArgumentException("app.gitlab.url must include project path");
        }

        return URLEncoder.encode(normalizedPath, StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
