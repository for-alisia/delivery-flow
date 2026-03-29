package com.gitlabflow.floworchestrator.integration.gitlab;

import com.gitlabflow.floworchestrator.common.errors.ErrorCode;
import com.gitlabflow.floworchestrator.common.errors.IntegrationException;
import com.gitlabflow.floworchestrator.config.GitLabProjectCoordinates;
import com.gitlabflow.floworchestrator.integration.gitlab.dto.GitLabIssueResponse;
import com.gitlabflow.floworchestrator.integration.gitlab.dto.GitLabUserResponse;
import com.gitlabflow.floworchestrator.orchestration.issues.IssuesProvider;
import com.gitlabflow.floworchestrator.orchestration.issues.model.AssigneeSummary;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueSummary;
import com.gitlabflow.floworchestrator.orchestration.issues.model.ListIssuesQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.model.ListIssuesResult;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GitLabIssuesAdapter implements IssuesProvider {

    private static final String PROVIDER = "gitlab";

    private final GitLabIssuesClient gitLabIssuesClient;
    private final GitLabProjectCoordinates gitLabProjectCoordinates;

    @Override
    public ListIssuesResult listIssues(final ListIssuesQuery query) {
        try {
            // GitLab CE expects assignee_username as repeated values; MVP supports a single assignee only.
            final List<String> assigneeUsernames = query.assignee() == null ? null : List.of(query.assignee());
            final List<GitLabIssueResponse> issues = gitLabIssuesClient.listIssues(
                    gitLabProjectCoordinates.projectPath(),
                    query.label(),
                    assigneeUsernames,
                    query.page(),
                    query.pageSize()
            );

            final List<IssueSummary> mappedIssues = issues.stream()
                    .map(this::toIssueSummary)
                    .toList();

            return new ListIssuesResult(mappedIssues, query.page(), query.pageSize());
        } catch (final FeignException exception) {
            throw toIntegrationException(exception);
        } catch (final RuntimeException exception) {
            throw new IntegrationException(ErrorCode.INTEGRATION_FAILURE, "Issue provider request failed", PROVIDER);
        }
    }

    private IssueSummary toIssueSummary(final GitLabIssueResponse response) {
        final List<AssigneeSummary> assignees = response.assignees() == null
                ? List.of()
                : response.assignees().stream()
                .map(this::toAssigneeSummary)
                .toList();

        final List<String> labels = response.labels() == null ? List.of() : List.copyOf(response.labels());

        return new IssueSummary(
                response.issueNumber(),
                response.title(),
                response.state(),
                labels,
                assignees,
                response.webUrl(),
                response.createdAt(),
                response.updatedAt()
        );
    }

    private AssigneeSummary toAssigneeSummary(final GitLabUserResponse response) {
        return new AssigneeSummary(response.username(), response.name(), response.webUrl());
    }

    private IntegrationException toIntegrationException(final FeignException exception) {
        return switch (exception.status()) {
            case 401, 403 -> new IntegrationException(ErrorCode.INTEGRATION_AUTHENTICATION_FAILED, "Issue provider authentication failed", PROVIDER);
            case 404 -> new IntegrationException(ErrorCode.INTEGRATION_NOT_FOUND, "Configured project not found in provider", PROVIDER);
            case 429 -> new IntegrationException(ErrorCode.INTEGRATION_RATE_LIMITED, "Issue provider rate limit reached", PROVIDER);
            default -> new IntegrationException(ErrorCode.INTEGRATION_FAILURE, "Issue provider request failed", PROVIDER);
        };
    }
}
