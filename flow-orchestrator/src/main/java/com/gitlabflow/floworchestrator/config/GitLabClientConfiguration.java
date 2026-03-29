package com.gitlabflow.floworchestrator.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GitLabClientConfiguration {

    @Bean
    public GitLabProjectCoordinates gitLabProjectCoordinates(final GitLabProjectProperties properties) {
        return GitLabProjectCoordinates.fromProjectUrl(properties.url());
    }

    @Bean
    public RequestInterceptor gitLabAuthRequestInterceptor(final GitLabProjectProperties properties,
                                                           final GitLabProjectCoordinates coordinates) {
        return template -> {
            template.target(coordinates.apiBaseUrl());
            template.header("PRIVATE-TOKEN", properties.token());
        };
    }
}
