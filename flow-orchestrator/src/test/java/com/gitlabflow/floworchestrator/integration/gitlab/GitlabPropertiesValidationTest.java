package com.gitlabflow.floworchestrator.integration.gitlab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GitlabProperties")
class GitlabPropertiesValidationTest {

    @Test
    @DisplayName("given blank url when create then throws illegal argument")
    void givenBlankUrlWhenCreateThenThrowsIllegalArgument() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new GitlabProperties(" ", "token"));
    }

    @Test
    @DisplayName("given null url when create then throws illegal argument")
    void givenNullUrlWhenCreateThenThrowsIllegalArgument() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new GitlabProperties(null, "token"));
    }

    @Test
    @DisplayName("given blank token when create then throws illegal argument")
    void givenBlankTokenWhenCreateThenThrowsIllegalArgument() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new GitlabProperties("https://gitlab.example.com/group/project", " "));
    }

    @Test
    @DisplayName("given null token when create then throws illegal argument")
    void givenNullTokenWhenCreateThenThrowsIllegalArgument() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new GitlabProperties("https://gitlab.example.com/group/project", null));
    }

    @Test
    @DisplayName("given valid url and token when create then succeeds")
    void givenValidUrlAndTokenWhenCreateThenSucceeds() {
        GitlabProperties properties = new GitlabProperties("https://gitlab.example.com/group/project", "token");

        assertThat(properties.url()).isEqualTo("https://gitlab.example.com/group/project");
        assertThat(properties.token()).isEqualTo("token");
    }

    @Test
    @DisplayName("given project url when get base url then returns host root")
    void givenProjectUrlWhenGetBaseUrlThenReturnsHostRoot() {
        GitlabProperties properties = new GitlabProperties("https://gitlab.example.com/group/project", "token");

        assertThat(properties.getBaseUrl()).isEqualTo("https://gitlab.example.com");
    }

    @Test
    @DisplayName("given project url when get encoded project path then encodes slash")
    void givenProjectUrlWhenGetEncodedProjectPathThenEncodesSlash() {
        GitlabProperties properties = new GitlabProperties("https://gitlab.example.com/group/project", "token");

        assertThat(properties.getEncodedProjectPath()).isEqualTo("group%2Fproject");
    }

    @Test
    @DisplayName("given nested project url when get encoded project path then encodes full path")
    void givenNestedProjectUrlWhenGetEncodedProjectPathThenEncodesFullPath() {
        GitlabProperties properties = new GitlabProperties("https://gitlab.example.com/group/subgroup/project", "token");

        assertThat(properties.getEncodedProjectPath()).isEqualTo("group%2Fsubgroup%2Fproject");
    }
}
