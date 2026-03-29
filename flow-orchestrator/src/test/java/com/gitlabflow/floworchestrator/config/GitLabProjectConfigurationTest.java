package com.gitlabflow.floworchestrator.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class GitLabProjectConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class, ValidationAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class);

    @Test
    @DisplayName("creates parsed coordinates from configured project URL")
    void createsParsedCoordinatesFromConfiguredProjectUrl() {
        contextRunner
                .withPropertyValues(
                        "app.gitlab.url=https://gitlab.com/group-a/platform/project-a",
                        "app.gitlab.token=test-token",
                        "app.issues-api.default-page-size=40",
                        "app.issues-api.max-page-size=100"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    final var coordinates = context.getBean(GitLabProjectCoordinates.class);
                    assertThat(coordinates.apiBaseUrl()).isEqualTo("https://gitlab.com/api/v4");
                    assertThat(coordinates.projectPath()).isEqualTo("group-a/platform/project-a");
                });
    }

    @Test
    @DisplayName("fails fast when required gitlab URL is missing")
    void failsFastWhenRequiredGitLabUrlIsMissing() {
        contextRunner
                .withPropertyValues(
                        "app.gitlab.token=test-token",
                        "app.issues-api.default-page-size=40",
                        "app.issues-api.max-page-size=100"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("prefix=app.gitlab");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({GitLabProjectProperties.class, IssuesApiProperties.class})
    @Import(GitLabClientConfiguration.class)
    static class TestConfiguration {
    }
}