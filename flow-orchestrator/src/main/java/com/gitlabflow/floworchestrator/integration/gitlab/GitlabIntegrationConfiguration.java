package com.gitlabflow.floworchestrator.integration.gitlab;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;

@Configuration
@EnableConfigurationProperties(GitlabProperties.class)
public class GitlabIntegrationConfiguration {

    @Bean
    public String gitlabApiBaseUrl(GitlabProperties gitlabProperties) {
        return gitlabProperties.apiBaseUrl();
    }

    @Bean
    public RequestInterceptor gitlabTokenRequestInterceptor(GitlabProperties gitlabProperties) {
        return requestTemplate -> requestTemplate.header("PRIVATE-TOKEN", gitlabProperties.token());
    }

    @Bean
    public ErrorDecoder gitlabErrorDecoder() {
        return new GitlabErrorDecoder();
    }
}
