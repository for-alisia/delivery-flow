package com.gitlabflow.floworchestrator.integration.gitlab;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GitlabPropertiesValidation")
class GitlabPropertiesValidationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfig.class);

    @Test
    @DisplayName("given missing token when context starts then startup fails")
    void givenMissingTokenWhenContextStartsThenStartupFails() {
        contextRunner
                .withPropertyValues("app.gitlab.url=https://gitlab.example.com")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("field 'token'");
                });
    }

    @Test
    @DisplayName("given missing url when context starts then startup fails")
    void givenMissingUrlWhenContextStartsThenStartupFails() {
        contextRunner
                .withPropertyValues("app.gitlab.token=secret-token")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("field 'url'");
                });
    }

    @Test
    @DisplayName("given required properties when context starts then startup succeeds")
    void givenRequiredPropertiesWhenContextStartsThenStartupSucceeds() {
        contextRunner
                .withPropertyValues(
                        "app.gitlab.url=https://gitlab.example.com",
                        "app.gitlab.token=secret-token"
                )
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    @DisplayName("given project-style url when context starts then api base url is normalized")
    void givenProjectStyleUrlWhenContextStartsThenApiBaseUrlIsNormalized() {
        contextRunner
                .withPropertyValues(
                        "app.gitlab.url=https://gitlab.example.com/group/subgroup/project",
                        "app.gitlab.token=secret-token"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    GitlabProperties properties = context.getBean(GitlabProperties.class);
                    assertThat(properties.apiBaseUrl()).isEqualTo("https://gitlab.example.com");
                });
    }

    @Test
    @DisplayName("given invalid url when api base url is resolved then validation fails")
    void givenInvalidUrlWhenApiBaseUrlIsResolvedThenValidationFails() {
        contextRunner
                .withPropertyValues(
                        "app.gitlab.url=not-a-url",
                        "app.gitlab.token=secret-token"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    GitlabProperties properties = context.getBean(GitlabProperties.class);
                    assertThatThrownBy(properties::apiBaseUrl)
                        .isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("app.gitlab.url");
                });
    }

    @Configuration
    @EnableConfigurationProperties(GitlabProperties.class)
    static class PropertiesConfig {
    }
}
