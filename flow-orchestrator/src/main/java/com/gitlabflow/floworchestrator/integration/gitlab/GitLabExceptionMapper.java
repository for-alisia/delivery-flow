package com.gitlabflow.floworchestrator.integration.gitlab;

import com.gitlabflow.floworchestrator.common.errors.ErrorCode;
import com.gitlabflow.floworchestrator.common.errors.IntegrationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GitLabExceptionMapper {

    public IntegrationException fromHttpFailure(
            final RestClientResponseException exception,
            final String source
    ) {
        return fromHttpFailure(exception, source, "issues");
    }

    public IntegrationException fromHttpFailure(
            final RestClientResponseException exception,
            final String source,
            final String resource
    ) {
        final String failureMessage = "Unable to retrieve " + resource + " from GitLab";
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

    public IntegrationException fromTransportFailure(final String source) {
        return fromTransportFailure(source, "issues");
    }

    public IntegrationException fromTransportFailure(final String source, final String resource) {
        return new IntegrationException(
                ErrorCode.INTEGRATION_FAILURE,
                "Unable to retrieve " + resource + " from GitLab",
                source
        );
    }
}