package com.gitlabflow.floworchestrator.integration.gitlab;

import com.gitlabflow.floworchestrator.config.GitLabProperties;
import java.net.URI;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public final class GitLabProjectLocator {

    private final ProjectReference projectReference;

    // Constructor is intentionally manual because project URL resolution runs during bean initialization.
    public GitLabProjectLocator(final GitLabProperties gitLabProperties) {
        this.projectReference = resolve(gitLabProperties.url());
        log.info(
                "Resolved GitLab project reference apiBaseUrl={} projectPath={}",
                projectReference.apiBaseUrl(),
                projectReference.projectPath());
    }

    public ProjectReference projectReference() {
        return projectReference;
    }

    private ProjectReference resolve(final String projectUrl) {
        try {
            final URI uri = URI.create(projectUrl);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw invalidProjectUrl(projectUrl);
            }

            final String[] segments = Arrays.stream(uri.getPath().split("/"))
                    .filter(segment -> !segment.isBlank())
                    .toArray(String[]::new);

            if (segments.length < 2) {
                throw invalidProjectUrl(projectUrl);
            }

            final String projectPath = String.join("/", segments);

            final String apiBaseUrl = uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "") + "/api/v4";

            return new ProjectReference(apiBaseUrl, projectPath);
        } catch (final IllegalArgumentException exception) {
            throw invalidProjectUrl(projectUrl);
        }
    }

    private IllegalStateException invalidProjectUrl(final String projectUrl) {
        return new IllegalStateException("Invalid app.gitlab.url project URL: " + projectUrl);
    }

    public record ProjectReference(String apiBaseUrl, String projectPath) {}
}
