package com.gitlabflow.floworchestrator.integration.gitlab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gitlabflow.floworchestrator.common.error.ErrorCode;
import com.gitlabflow.floworchestrator.common.error.IntegrationException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

class GitLabOperationExecutorTest {

    private static final String RESOURCE_MILESTONES = "milestones";

    private final GitLabOperationExecutor executor = new GitLabOperationExecutor(new GitLabExceptionMapper());

    @Test
    @DisplayName("returns supplier result when operation succeeds")
    void returnsSupplierResultWhenOperationSucceeds() {
        final String result = executor.execute(RESOURCE_MILESTONES, () -> "ok");

        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("maps HTTP failures through GitLab exception mapper")
    void mapsHttpFailuresThroughGitLabExceptionMapper() {
        assertThatThrownBy(() -> executor.execute(RESOURCE_MILESTONES, () -> {
                    throw httpFailure(401);
                }))
                .isInstanceOfSatisfying(IntegrationException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_AUTHENTICATION_FAILED);
                    assertThat(exception.getMessage()).isEqualTo("GitLab milestones operation failed");
                });
    }

    @Test
    @DisplayName("maps transport failures to generic integration failure")
    void mapsTransportFailuresToGenericIntegrationFailure() {
        assertThatThrownBy(() -> executor.execute(RESOURCE_MILESTONES, () -> {
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOfSatisfying(IntegrationException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.INTEGRATION_FAILURE);
                    assertThat(exception.getMessage()).isEqualTo("GitLab milestones operation failed");
                });
    }

    @Test
    @DisplayName("executes runnable operation")
    void executesRunnableOperation() {
        final AtomicInteger counter = new AtomicInteger();

        executor.execute(RESOURCE_MILESTONES, counter::incrementAndGet);

        assertThat(counter).hasValue(1);
    }

    private RestClientResponseException httpFailure(final int statusCode) {
        return new RestClientResponseException("GitLab failure", statusCode, "error", null, null, null);
    }
}
