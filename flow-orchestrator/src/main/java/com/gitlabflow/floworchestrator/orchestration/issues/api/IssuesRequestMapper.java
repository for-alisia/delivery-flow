package com.gitlabflow.floworchestrator.orchestration.issues.api;

import static java.util.Optional.ofNullable;

import com.gitlabflow.floworchestrator.config.IssuesApiProperties;
import com.gitlabflow.floworchestrator.orchestration.issues.IssueQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.IssueState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class IssuesRequestMapper {

    private static final int DEFAULT_PAGE = 1;

    private final IssuesApiProperties issuesApiProperties;

    public IssueQuery toIssueQuery(final IssuesRequest request) {
        final PaginationRequest pagination = ofNullable(request)
                .map(IssuesRequest::pagination)
                .orElse(null);
        final IssueFiltersRequest filters = ofNullable(request)
                .map(IssuesRequest::filters)
                .orElse(null);

        final int page = ofNullable(pagination)
                .map(PaginationRequest::page)
                .orElse(DEFAULT_PAGE);
        final int perPage = ofNullable(pagination)
                .map(PaginationRequest::perPage)
                .orElse(issuesApiProperties.defaultPageSize());

        final IssueState state = ofNullable(filters)
                .map(IssueFiltersRequest::state)
                .map(IssueState::fromValue)
                .orElse(null);

        return new IssueQuery(
                page,
                perPage,
                state,
                extractSingleValue(ofNullable(filters).map(IssueFiltersRequest::labels).orElse(null)),
                extractSingleValue(ofNullable(filters).map(IssueFiltersRequest::assignee).orElse(null)),
                extractSingleValue(ofNullable(filters).map(IssueFiltersRequest::milestone).orElse(null))
        );
    }

    private String extractSingleValue(final List<String> values) {
        return ofNullable(values)
                .flatMap(list -> list.stream().filter(Objects::nonNull).findFirst())
                .orElse(null);
    }
}