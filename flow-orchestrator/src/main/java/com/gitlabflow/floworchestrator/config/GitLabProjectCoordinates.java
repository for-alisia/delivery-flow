package com.gitlabflow.floworchestrator.config;

import java.net.URI;

public record GitLabProjectCoordinates(
        String apiBaseUrl,
    String projectPath
) {

    public static GitLabProjectCoordinates fromProjectUrl(final String projectUrl) {
        final URI uri;
        try {
            uri = URI.create(projectUrl);
        } catch (final IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid GitLab project URL format", exception);
        }

        final String host = uri.getScheme() + "://" + uri.getHost();
        final String path = uri.getPath();
        if (path == null || path.isBlank() || !path.contains("/")) {
            throw new IllegalStateException("GitLab project URL must contain a namespace and project segment");
        }

        final String normalizedProjectPath = path.startsWith("/") ? path.substring(1) : path;

        return new GitLabProjectCoordinates(host + "/api/v4", normalizedProjectPath);
    }
}
