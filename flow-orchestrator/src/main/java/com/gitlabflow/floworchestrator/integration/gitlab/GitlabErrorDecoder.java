package com.gitlabflow.floworchestrator.integration.gitlab;

import java.util.Collection;
import java.util.Map;

import com.gitlabflow.floworchestrator.common.errors.ErrorCode;
import com.gitlabflow.floworchestrator.common.errors.IntegrationException;

import feign.Response;
import feign.codec.ErrorDecoder;

public class GitlabErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();

        if (status == 400) {
            return new IntegrationException(
                    ErrorCode.INTEGRATION_BAD_REQUEST,
                    "gitlab",
                    status,
                    "Issue retrieval request was rejected by the integration provider.",
                    null,
                    null
            );
        }
        if (status == 401) {
            return new IntegrationException(
                    ErrorCode.INTEGRATION_UNAUTHORIZED,
                    "gitlab",
                    status,
                    "Issue retrieval failed due to integration authentication.",
                    null,
                    null
            );
        }
        if (status == 403) {
            return new IntegrationException(
                    ErrorCode.INTEGRATION_FORBIDDEN,
                    "gitlab",
                    status,
                    "Issue retrieval is not permitted for the requested project.",
                    null,
                    null
            );
        }
        if (status == 404) {
            return new IntegrationException(
                    ErrorCode.INTEGRATION_NOT_FOUND,
                    "gitlab",
                    status,
                    "Requested project was not found or is not accessible.",
                    null,
                    null
            );
        }
        if (status == 429) {
            return new IntegrationException(
                    ErrorCode.INTEGRATION_RATE_LIMITED,
                    "gitlab",
                    status,
                    "Issue retrieval is temporarily rate-limited by the integration provider.",
                    parseRetryAfter(response.headers()),
                    null
            );
        }
        if (status >= 500) {
            return new IntegrationException(
                    ErrorCode.INTEGRATION_UNAVAILABLE,
                    "gitlab",
                    status,
                    "Issue retrieval is temporarily unavailable from the integration provider.",
                    null,
                    null
            );
        }

        return new IntegrationException(
                ErrorCode.INTEGRATION_UNKNOWN,
                "gitlab",
                status,
                "Issue retrieval failed due to an unexpected integration error.",
                null,
                null
        );
    }

    private Long parseRetryAfter(Map<String, Collection<String>> headers) {
        for (Map.Entry<String, Collection<String>> entry : headers.entrySet()) {
            if (!"retry-after".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            return entry.getValue().stream()
                    .findFirst()
                    .flatMap(this::parseLong)
                    .orElse(null);
        }
        return null;
    }

    private java.util.Optional<Long> parseLong(String value) {
        try {
            return java.util.Optional.of(Long.parseLong(value));
        } catch (NumberFormatException ex) {
            return java.util.Optional.empty();
        }
    }
}
