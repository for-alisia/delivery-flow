package com.gitlabflow.floworchestrator.integration.gitlab;

import com.gitlabflow.floworchestrator.config.GitLabProperties;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GitLabRestClientConfig {

    @Bean
    public RestClient gitLabRestClient(
            final RestClient.Builder restClientBuilder,
            final GitLabProperties gitLabProperties,
            final GitLabProjectLocator gitLabProjectLocator) {
        return restClientBuilder
                .baseUrl(Objects.requireNonNull(
                        gitLabProjectLocator.projectReference().apiBaseUrl()))
                .defaultHeader("PRIVATE-TOKEN", gitLabProperties.token())
                .build();
    }
}
