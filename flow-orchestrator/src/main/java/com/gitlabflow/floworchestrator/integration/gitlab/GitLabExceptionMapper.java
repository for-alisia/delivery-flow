package com.gitlabflow.floworchestrator.integration.gitlab;

import com.gitlabflow.floworchestrator.common.error.ErrorCode;
import com.gitlabflow.floworchestrator.common.error.IntegrationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GitLabExceptionMapper {

    public IntegrationException fromHttpFailure(
            final RestClientResponseException exception,
            final String source,
            final String resource
    ) {
        final String failureMessage = "GitLab " + resource + " operation failed";
        final HttpStatusCode statusCode = exception.getStatusCode();
        if (statusCode.value() == 401) {
            return new IntegrationException(
                    ErrorCode.INTEGRATION_AUTHENTICATION_FAILED,
                    failureMessage,
                    source
            );
        }
        if (statusCode.value() == 404) {
            return new IntegrationException(
                    ErrorCode.INTEGRATION_NOT_FOUND,
                    failureMessage,
                    source
            );
        }
        if (statusCode.value() == 429) {
            return new IntegrationException(
                    ErrorCode.INTEGRATION_RATE_LIMITED,
                    failureMessage,
                    source
            );
        }
        return new IntegrationException(
                ErrorCode.INTEGRATION_FAILURE,
                failureMessage,
                source
        );
    }

    public IntegrationException fromTransportFailure(final String source, final String resource) {
        return new IntegrationException(
                ErrorCode.INTEGRATION_FAILURE,
                "GitLab " + resource + " operation failed",
                source
        );
    }
}