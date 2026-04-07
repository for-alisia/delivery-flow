package com.gitlabflow.floworchestrator.integration.gitlab;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GitLabUriFactory {

    private final GitLabProjectLocator gitLabProjectLocator;

    public String projectResourcePath(final String resource) {
        return "/projects/{projectPath}/" + resource;
    }

    public String projectPath() {
        return gitLabProjectLocator.projectReference().projectPath();
    }
}
