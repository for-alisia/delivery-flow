package com.gitlabflow.floworchestrator.orchestration.issues.rest.mapper;

import static java.util.Optional.ofNullable;

import com.gitlabflow.floworchestrator.config.IssuesApiProperties;
import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueState;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.CreateIssueRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueFiltersRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.PaginationRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class IssuesRequestMapper {

    private static final int DEFAULT_PAGE = 1;

    private final IssuesApiProperties issuesApiProperties;

    public IssueQuery toIssueQuery(final SearchIssuesRequest request) {
        final PaginationRequest pagination = ofNullable(request)
                .map(SearchIssuesRequest::pagination)
                .orElse(null);
        final IssueFiltersRequest filters = ofNullable(request)
                .map(SearchIssuesRequest::filters)
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

    public CreateIssueInput toCreateIssueInput(final CreateIssueRequest request) {
        final List<String> labels = request.labels() == null ? List.of() : request.labels();
        return new CreateIssueInput(request.title(), request.description(), labels);
    }

    private String extractSingleValue(final List<String> values) {
        return ofNullable(values)
                .flatMap(list -> list.stream().filter(Objects::nonNull).findFirst())
                .orElse(null);
    }
}