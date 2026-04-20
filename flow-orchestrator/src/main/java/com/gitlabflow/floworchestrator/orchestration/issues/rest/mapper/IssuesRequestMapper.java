package com.gitlabflow.floworchestrator.orchestration.issues.rest.mapper;

import static java.util.Optional.ofNullable;

import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueAuditType;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueState;
import com.gitlabflow.floworchestrator.orchestration.issues.model.UpdateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.CreateIssueRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueFiltersRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.PaginationRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.UpdateIssueRequest;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class IssuesRequestMapper {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PER_PAGE = 20;

    public IssueQuery toIssueQuery(final SearchIssuesRequest request) {
        final PaginationRequest pagination =
                ofNullable(request).map(SearchIssuesRequest::pagination).orElse(null);
        final IssueFiltersRequest filters =
                ofNullable(request).map(SearchIssuesRequest::filters).orElse(null);

        final int page = ofNullable(pagination).map(PaginationRequest::page).orElse(DEFAULT_PAGE);
        final int perPage =
                ofNullable(pagination).map(PaginationRequest::perPage).orElse(DEFAULT_PER_PAGE);

        final IssueState state = ofNullable(filters)
                .map(IssueFiltersRequest::state)
                .map(IssueState::fromValue)
                .orElse(null);

        return IssueQuery.builder()
                .page(page)
                .perPage(perPage)
                .state(state)
                .label(extractSingleValue(
                        ofNullable(filters).map(IssueFiltersRequest::labels).orElse(null)))
                .assignee(extractSingleValue(
                        ofNullable(filters).map(IssueFiltersRequest::assignee).orElse(null)))
                .milestone(extractSingleValue(
                        ofNullable(filters).map(IssueFiltersRequest::milestone).orElse(null)))
                .auditTypes(mapAuditTypes(filters))
                .build();
    }

    public CreateIssueInput toCreateIssueInput(final CreateIssueRequest request) {
        final List<String> labels = deduplicateLabels(request.labels());
        return CreateIssueInput.builder()
                .title(request.title())
                .description(request.description())
                .labels(labels)
                .build();
    }

    public UpdateIssueInput toUpdateIssueInput(final long issueId, final UpdateIssueRequest request) {
        final List<String> addLabels = deduplicateLabels(request.addLabels());
        final List<String> removeLabels = deduplicateLabels(request.removeLabels());

        return UpdateIssueInput.builder()
                .issueId(issueId)
                .title(request.title())
                .description(request.description())
                .addLabels(addLabels)
                .removeLabels(removeLabels)
                .build();
    }

    private List<String> deduplicateLabels(final List<String> labels) {
        return labels == null ? List.of() : labels.stream().distinct().toList();
    }

    private String extractSingleValue(final List<String> values) {
        return ofNullable(values)
                .flatMap(list -> list.stream().filter(Objects::nonNull).findFirst())
                .orElse(null);
    }

    private List<IssueAuditType> mapAuditTypes(final IssueFiltersRequest filters) {
        return ofNullable(filters).map(IssueFiltersRequest::audit).orElse(List.of()).stream()
                .map(IssueAuditType::fromValue)
                .distinct()
                .toList();
    }
}
