package com.gitlabflow.floworchestrator.integration.gitlab;

import java.util.Collection;

import com.gitlabflow.floworchestrator.common.errors.ErrorCode;
import com.gitlabflow.floworchestrator.common.errors.IntegrationException;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GitlabErrorDecoder implements ErrorDecoder {

    private static final String SOURCE = "gitlab";

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        log.warn("GitLab API error: status={} method={}", status, methodKey);

        return switch (status) {
            case 400 -> integrationException(ErrorCode.INTEGRATION_BAD_REQUEST, status, "Invalid request sent to GitLab", null);
            case 401 -> integrationException(ErrorCode.INTEGRATION_UNAUTHORIZED, status, "GitLab authentication failed", null);
            case 403 -> integrationException(ErrorCode.INTEGRATION_FORBIDDEN, status, "GitLab access is forbidden", null);
            case 404 -> integrationException(ErrorCode.INTEGRATION_NOT_FOUND, status, "GitLab project not found or not accessible", null);
            case 429 -> integrationException(
                    ErrorCode.INTEGRATION_RATE_LIMITED,
                    status,
                    "GitLab rate limit exceeded",
                    parseRetryAfter(response.headers().get("Retry-After"))
            );
            default -> {
                if (status >= 500 && status <= 599) {
                    yield integrationException(ErrorCode.INTEGRATION_UNKNOWN, status, "GitLab server error", null);
                }
                yield integrationException(ErrorCode.INTEGRATION_UNKNOWN, status, "Unexpected GitLab integration error", null);
            }
        };
    }

    private IntegrationException integrationException(ErrorCode code, int status, String message, Long retryAfterSeconds) {
        return new IntegrationException(code, SOURCE, status, message, retryAfterSeconds, null);
    }

    private Long parseRetryAfter(Collection<String> retryAfterValues) {
        if (retryAfterValues == null || retryAfterValues.isEmpty()) {
            return null;
        }

        String rawValue = retryAfterValues.iterator().next();
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
