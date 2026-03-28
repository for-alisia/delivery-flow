package com.gitlabflow.floworchestrator.integration.gitlab;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import feign.Client;
import feign.Feign;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@Import(FeignClientsConfiguration.class)
@EnableConfigurationProperties(GitlabProperties.class)
public class GitlabIntegrationConfiguration {

    @Bean
    public Client feignClient() {
        return new Client.Default(null, null);
    }

    @Bean
    public GitlabIssuesClient gitlabIssuesClient(
            Client client,
            Encoder encoder,
            Decoder decoder,
            feign.Contract contract,
            GitlabProperties properties) {
        String projectBaseUrl = properties.getBaseUrl() + "/api/v4/projects/" + properties.getEncodedProjectPath();

        log.info("GitLab integration configured: base URL={}", properties.getBaseUrl());
        log.info("GitLab project path (encoded): {}", properties.getEncodedProjectPath());
        log.info("GitLab full target URL: {}", projectBaseUrl);

        return Feign.builder()
                .client(client)
                .encoder(encoder)
                .decoder(decoder)
                .contract(contract)
                .requestInterceptor(gitlabTokenInterceptor(properties))
                .errorDecoder(new GitlabErrorDecoder())
                .target(GitlabIssuesClient.class, projectBaseUrl);
    }

    private RequestInterceptor gitlabTokenInterceptor(GitlabProperties properties) {
        return template -> template.header("PRIVATE-TOKEN", properties.token());
    }
}
