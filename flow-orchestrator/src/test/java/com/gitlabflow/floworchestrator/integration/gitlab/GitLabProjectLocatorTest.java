package com.gitlabflow.floworchestrator.integration.gitlab;

import com.gitlabflow.floworchestrator.config.GitLabProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitLabProjectLocatorTest {

    @Test
    @DisplayName("extracts api base and encoded project path")
    void extractsApiBaseAndEncodedProjectPath() {
        final GitLabProjectLocator locator = new GitLabProjectLocator(
                new GitLabProperties("https://gitlab.com/group/subgroup/project", "redacted")
        );

        assertThat(locator.projectReference().apiBaseUrl()).isEqualTo("https://gitlab.com/api/v4");
        assertThat(locator.projectReference().projectPath()).isEqualTo("group/subgroup/project");
    }

    @Test
    @DisplayName("extracts api base with non-standard port")
    void extractsApiBaseWithNonStandardPort() {
        final GitLabProjectLocator locator = new GitLabProjectLocator(
                new GitLabProperties("https://gitlab.local:8443/group/project", "redacted")
        );

        assertThat(locator.projectReference().apiBaseUrl()).isEqualTo("https://gitlab.local:8443/api/v4");
        assertThat(locator.projectReference().projectPath()).isEqualTo("group/project");
    }

    @Test
    @DisplayName("accepts project url with trailing slash")
    void acceptsProjectUrlWithTrailingSlash() {
        final GitLabProjectLocator locator = new GitLabProjectLocator(
                new GitLabProperties("https://gitlab.com/group/project/", "redacted")
        );

        assertThat(locator.projectReference().apiBaseUrl()).isEqualTo("https://gitlab.com/api/v4");
        assertThat(locator.projectReference().projectPath()).isEqualTo("group/project");
    }

    @Test
    @DisplayName("fails for invalid gitlab project url")
    void failsForInvalidProjectUrl() {
        final GitLabProperties properties = new GitLabProperties("not-a-url", "redacted");

        assertThatThrownBy(() -> new GitLabProjectLocator(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid app.gitlab.url project URL");
    }

    @Test
    @DisplayName("fails when project url contains single path segment")
    void failsWhenProjectUrlContainsSinglePathSegment() {
        final GitLabProperties properties = new GitLabProperties("https://gitlab.com/project", "redacted");

        assertThatThrownBy(() -> new GitLabProjectLocator(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid app.gitlab.url project URL");
    }
}
