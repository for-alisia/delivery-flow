package com.gitlabflow.floworchestrator.orchestration.issues;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.gitlabflow.floworchestrator.common.errors.ValidationException;
import com.gitlabflow.floworchestrator.orchestration.issues.models.GetIssuesRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GetIssuesRequestValidator {

    private static final Set<String> ALLOWED_STATES = Set.of("opened", "closed", "all");
    private static final Set<String> ALLOWED_ORDER_BY = Set.of("created_at", "updated_at");
    private static final Set<String> ALLOWED_SORT = Set.of("asc", "desc");

    public void validate(GetIssuesRequest request) {
        validateAllowed("state", request.state(), ALLOWED_STATES);
        validateAllowed("orderBy", request.orderBy(), ALLOWED_ORDER_BY);
        validateAllowed("sort", request.sort(), ALLOWED_SORT);

        if (request.page() != null && request.page() < 1) {
            throwValidation("page", "must be greater than or equal to 1");
        }

        if (request.perPage() != null && (request.perPage() < 1 || request.perPage() > 100)) {
            throwValidation("perPage", "must be between 1 and 100");
        }
    }

    private void validateAllowed(String field, String value, Set<String> allowedValues) {
        if (value == null) {
            return;
        }
        if (!allowedValues.contains(value)) {
            throwValidation(field, "unsupported value: " + value);
        }
    }

    private void throwValidation(String field, String message) {
        log.debug("Validation failed for parameter '{}': {}", field, message);
        throw new ValidationException("Invalid " + field + ": " + message);
    }
}
