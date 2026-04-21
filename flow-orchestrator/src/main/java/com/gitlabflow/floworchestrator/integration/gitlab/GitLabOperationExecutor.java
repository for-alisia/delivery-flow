package com.gitlabflow.floworchestrator.integration.gitlab;

import com.gitlabflow.floworchestrator.common.error.IntegrationException;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitLabOperationExecutor {

    private static final String SOURCE_GITLAB = "gitlab";

    private final GitLabExceptionMapper gitLabExceptionMapper;

    public <T> T execute(final String resource, final Supplier<T> operation) {
        try {
            return operation.get();
        } catch (final RestClientResponseException exception) {
            final IntegrationException mappedException =
                    gitLabExceptionMapper.fromHttpFailure(exception, SOURCE_GITLAB, resource);
            log.warn(
                    "GitLab request failed resource={} status={} category={}",
                    resource,
                    exception.getStatusCode().value(),
                    mappedException.errorCode().name());
            throw mappedException;
        } catch (final RuntimeException exception) {
            log.error(
                    "GitLab transport failure resource={} category={}",
                    resource,
                    exception.getClass().getSimpleName());
            throw gitLabExceptionMapper.fromTransportFailure(SOURCE_GITLAB, resource);
        }
    }

    public void execute(final String resource, final Runnable operation) {
        execute(resource, () -> {
            operation.run();
            return null;
        });
    }
}
