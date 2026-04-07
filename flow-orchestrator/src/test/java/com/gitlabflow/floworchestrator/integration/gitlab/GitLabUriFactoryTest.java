package com.gitlabflow.floworchestrator.integration.gitlab;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitlabflow.floworchestrator.config.GitLabProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GitLabUriFactoryTest {

    private static final String PROJECT_URL = "https://gitlab.example.com/group/subgroup/project";

    private final GitLabProjectLocator projectLocator =
            new GitLabProjectLocator(new GitLabProperties(PROJECT_URL, "redacted"));

    private final GitLabUriFactory gitLabUriFactory = new GitLabUriFactory(projectLocator);

    @Test
    @DisplayName("builds project-scoped resource path from resource name")
    void buildsProjectScopedResourcePathFromResourceName() {
        assertThat(gitLabUriFactory.projectResourcePath("issues")).isEqualTo("/projects/{projectPath}/issues");
    }

    @Test
    @DisplayName("returns resolved project path from locator")
    void returnsResolvedProjectPathFromLocator() {
        assertThat(gitLabUriFactory.projectPath()).isEqualTo("group/subgroup/project");
    }
}
