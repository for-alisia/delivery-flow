package com.gitlabflow.floworchestrator.integration.gitlab;

import com.gitlabflow.floworchestrator.config.GitLabProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GitLabRestClientConfig {

    @Bean
    public RestClient gitLabRestClient(
            final RestClient.Builder restClientBuilder,
            final GitLabProperties gitLabProperties,
            final GitLabProjectLocator gitLabProjectLocator) {
        final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(gitLabProperties.connectTimeoutSeconds()))
                .build();
        final JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(gitLabProperties.readTimeoutSeconds()));

        return restClientBuilder
                .requestFactory(requestFactory)
                .baseUrl(gitLabProjectLocator.projectReference().apiBaseUrl())
                .defaultHeader("PRIVATE-TOKEN", gitLabProperties.token())
                .build();
    }
}
