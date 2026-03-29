package com.gitlabflow.floworchestrator.orchestration.issues;

import org.springframework.stereotype.Component;

import com.gitlabflow.floworchestrator.common.errors.ValidationException;
import com.gitlabflow.floworchestrator.orchestration.issues.models.ListProjectIssuesQuery;

@Component
public class ListProjectIssuesRequestValidator {

    private static final int MAX_PAGE_SIZE = 100;

    public void validate(ListProjectIssuesQuery query) {
        if (query.projectId() == null || query.projectId().isBlank()) {
            throw new ValidationException("projectId is required");
        }
        if (query.page() <= 0) {
            throw new ValidationException("page must be positive");
        }
        if (query.pageSize() <= 0) {
            throw new ValidationException("pageSize must be positive");
        }
        if (query.pageSize() > MAX_PAGE_SIZE) {
            throw new ValidationException("pageSize must be less than or equal to 100");
        }
    }
}
