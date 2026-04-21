package com.gitlabflow.floworchestrator.integration.gitlab;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GitLabUriFactory {

    private final GitLabProjectLocator gitLabProjectLocator;

    public @NonNull String projectResourcePath(final String resource) {
        return "/projects/{projectPath}/" + resource;
    }

    @SuppressWarnings("null")
    public @NonNull String apiBaseUrl() {
        return gitLabProjectLocator.projectReference().apiBaseUrl();
    }

    @SuppressWarnings("null")
    public @NonNull String projectPath() {
        return gitLabProjectLocator.projectReference().projectPath();
    }
}
