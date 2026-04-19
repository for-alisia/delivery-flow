package com.gitlabflow.floworchestrator.integration.gitlab;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

@SpringBootTest(
        properties = {
            "app.gitlab.url=https://gitlab.example.com/group/project",
            "app.gitlab.token=redacted",
            "app.gitlab.connect-timeout-seconds=11",
            "app.gitlab.read-timeout-seconds=13"
        })
class GitLabRestClientConfigIT {

    @Autowired
    private RestClient gitLabRestClient;

    @Test
    @DisplayName("configures gitLabRestClient with JDK request factory and bound timeout values")
    void configuresGitLabRestClientWithJdkRequestFactoryAndBoundTimeoutValues() {
        final Object requestFactoryObject = ReflectionTestUtils.getField(gitLabRestClient, "clientRequestFactory");

        assertThat(requestFactoryObject).isInstanceOf(JdkClientHttpRequestFactory.class);

        final JdkClientHttpRequestFactory requestFactory = (JdkClientHttpRequestFactory) requestFactoryObject;
        final Duration readTimeout = (Duration) ReflectionTestUtils.getField(requestFactory, "readTimeout");
        final HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(requestFactory, "httpClient");

        assertThat(readTimeout).isEqualTo(Duration.ofSeconds(13));
        assertThat(httpClient.connectTimeout()).contains(Duration.ofSeconds(11));
    }
}
