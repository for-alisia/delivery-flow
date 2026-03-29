package com.gitlabflow.floworchestrator.orchestration.issues;

import com.gitlabflow.floworchestrator.common.errors.ValidationException;
import com.gitlabflow.floworchestrator.config.IssuesApiProperties;
import com.gitlabflow.floworchestrator.orchestration.issues.model.ListIssuesQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.model.ListIssuesResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListIssuesUseCase {

    private final IssuesProvider issuesProvider;
    private final IssuesApiProperties issuesApiProperties;

    public ListIssuesResult listIssues(final List<String> labels,
                                       final String assignee,
                                       final Integer page,
                                       final Integer pageSize) {
        final int resolvedPage = page == null ? 1 : page;
        final int resolvedPageSize = pageSize == null ? issuesApiProperties.defaultPageSize() : pageSize;

        if (resolvedPage <= 0) {
            throw new ValidationException("Invalid payload", List.of("page must be greater than 0"));
        }
        if (resolvedPageSize <= 0) {
            throw new ValidationException("Invalid payload", List.of("pageSize must be greater than 0"));
        }
        if (resolvedPageSize > issuesApiProperties.maxPageSize()) {
            throw new ValidationException("Invalid payload", List.of("pageSize must be <= " + issuesApiProperties.maxPageSize()));
        }

        final String resolvedLabel = resolveLabel(labels);
        final String resolvedAssignee = normalizeOptional(assignee, "assignee");

        final var query = new ListIssuesQuery(resolvedLabel, resolvedAssignee, resolvedPage, resolvedPageSize);
        final var result = issuesProvider.listIssues(query);
        return new ListIssuesResult(result.issues(), resolvedPage, resolvedPageSize);
    }

    private String resolveLabel(final List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return null;
        }
        if (labels.size() > 1) {
            throw new ValidationException("Invalid payload", List.of("labels must contain at most one value"));
        }
        return normalizeOptional(labels.getFirst(), "labels[0]");
    }

    private String normalizeOptional(final String value, final String fieldName) {
        if (value == null) {
            return null;
        }
        final String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new ValidationException("Invalid payload", List.of(fieldName + " must not be blank"));
        }
        return normalized;
    }
}
