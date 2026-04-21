package com.gitlabflow.floworchestrator.integration.gitlab;

import com.gitlabflow.floworchestrator.config.GitLabProperties;
import java.net.URI;
import java.util.Arrays;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public final class GitLabProjectLocator {

    private final ProjectReference projectReference;

    // Constructor is intentionally manual because project URL resolution runs during bean initialization.
    public GitLabProjectLocator(final GitLabProperties gitLabProperties) {
        this.projectReference = resolve(gitLabProperties.url());
        log.info("Resolved GitLab project reference from configured app.gitlab.url");
    }

    public ProjectReference projectReference() {
        return projectReference;
    }

    private ProjectReference resolve(final String projectUrl) {
        try {
            final URI uri = URI.create(projectUrl);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw invalidProjectUrl();
            }

            final String[] segments = Arrays.stream(uri.getPath().split("/"))
                    .filter(segment -> !segment.isBlank())
                    .toArray(String[]::new);

            if (segments.length < 2) {
                throw invalidProjectUrl();
            }

            final String projectPath = String.join("/", segments);

            final String apiBaseUrl = uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "") + "/api/v4";

            return ProjectReference.builder()
                    .apiBaseUrl(apiBaseUrl)
                    .projectPath(projectPath)
                    .build();
        } catch (final IllegalArgumentException exception) {
            throw invalidProjectUrl();
        }
    }

    private IllegalStateException invalidProjectUrl() {
        return new IllegalStateException("Invalid app.gitlab.url project URL configuration."
                + " Expected an absolute URL with at least group/project path segments");
    }

    @Builder
    public record ProjectReference(String apiBaseUrl, String projectPath) {}
}
