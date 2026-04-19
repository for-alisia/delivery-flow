package com.gitlabflow.floworchestrator.integration.gitlab.issues;

import com.gitlabflow.floworchestrator.common.error.IntegrationException;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabExceptionMapper;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabUriFactory;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabIssueResponse;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabLabelEventResponse;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabSingleIssueResponse;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.mapper.GitLabIssueDetailMapper;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.mapper.GitLabIssuesMapper;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.mapper.GitLabLabelEventMapper;
import com.gitlabflow.floworchestrator.orchestration.common.model.ChangeSet;
import com.gitlabflow.floworchestrator.orchestration.issues.IssuesPort;
import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueSummary;
import com.gitlabflow.floworchestrator.orchestration.issues.model.UpdateIssueInput;
import java.net.URI;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitLabIssuesAdapter implements IssuesPort {

    private static final String SOURCE_GITLAB = "gitlab";
    private static final String RESOURCE_ISSUES = "issues";
    private static final String RESOURCE_CREATE_ISSUE = "create issue";
    private static final String RESOURCE_UPDATE_ISSUE = "update issue";
    private static final String RESOURCE_DELETE_ISSUE = "delete issue";
    private static final String RESOURCE_GET_ISSUE_DETAIL = "get issue detail";
    private static final String RESOURCE_GET_LABEL_EVENTS = "get label events";
    private static final String EMPTY_BODY_CATEGORY = "EmptyBody";

    private final RestClient gitLabRestClient;
    private final GitLabUriFactory gitLabUriFactory;
    private final GitLabIssuesMapper gitLabIssuesMapper;
    private final GitLabIssueDetailMapper gitLabIssueDetailMapper;
    private final GitLabLabelEventMapper gitLabLabelEventMapper;
    private final GitLabExceptionMapper gitLabExceptionMapper;

    @Override
    public IssuePage getIssues(final IssueQuery query) {
        return executeGitLabOperation(RESOURCE_ISSUES, () -> {
            final List<IssueSummary> issues = fetchIssues(query);
            return toIssuePage(query, issues);
        });
    }

    @Override
    public IssueSummary createIssue(final CreateIssueInput input) {
        log.info("Creating GitLab issue");

        return executeGitLabOperation(RESOURCE_CREATE_ISSUE, () -> {
            final GitLabIssueResponse issueResponse = requestIssue(input);
            final IssueSummary issue = gitLabIssuesMapper.toIssue(issueResponse);
            log.info("GitLab issue created id={}", issue.id());
            return issue;
        });
    }

    @Override
    public IssueSummary updateIssue(final UpdateIssueInput input) {
        log.info("Updating GitLab issue issueId={}", input.issueId());

        return executeGitLabOperation(RESOURCE_UPDATE_ISSUE, () -> {
            final GitLabIssueResponse issueResponse = requestIssueUpdate(input);
            final IssueSummary issue = gitLabIssuesMapper.toIssue(issueResponse);
            log.info("GitLab issue updated issueId={}", input.issueId());
            return issue;
        });
    }

    @Override
    public void deleteIssue(final long issueId) {
        log.info("Deleting GitLab issue issueId={}", issueId);

        executeGitLabOperation(RESOURCE_DELETE_ISSUE, () -> {
            gitLabRestClient
                    .delete()
                    .uri(uriBuilder -> uriBuilder.path(issueByIdPath()).build(gitLabUriFactory.projectPath(), issueId))
                    .retrieve()
                    .toBodilessEntity();
            log.info("GitLab issue deleted issueId={}", issueId);
        });
    }

    @Override
    public IssueDetail getIssueDetail(final long issueId) {
        log.info("Fetching GitLab issue detail issueId={}", issueId);

        return executeGitLabOperation(RESOURCE_GET_ISSUE_DETAIL, () -> {
            final GitLabSingleIssueResponse response = gitLabRestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder.path(issueByIdPath()).build(gitLabUriFactory.projectPath(), issueId))
                    .retrieve()
                    .body(GitLabSingleIssueResponse.class);

            if (response == null) {
                throw mapTransportFailure(EMPTY_BODY_CATEGORY, RESOURCE_GET_ISSUE_DETAIL);
            }

            final IssueDetail issueDetail = gitLabIssueDetailMapper.toIssueDetail(response);
            log.info("GitLab issue detail fetched issueId={}", issueId);
            return issueDetail;
        });
    }

    @Override
    public List<ChangeSet<?>> getLabelEvents(final long issueId) {
        log.info("Fetching GitLab label events issueId={}", issueId);

        return executeGitLabOperation(RESOURCE_GET_LABEL_EVENTS, () -> {
            final List<GitLabLabelEventResponse> response = gitLabRestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(gitLabUriFactory.projectResourcePath(RESOURCE_ISSUES)
                                    + "/{issueId}/resource_label_events")
                            .build(gitLabUriFactory.projectPath(), issueId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null) {
                throw mapTransportFailure(EMPTY_BODY_CATEGORY, RESOURCE_GET_LABEL_EVENTS);
            }

            final List<ChangeSet<?>> changeSets = gitLabLabelEventMapper.toLabelChangeSets(response);
            log.info("GitLab label events fetched issueId={} count={}", issueId, changeSets.size());
            return changeSets;
        });
    }

    private List<IssueSummary> fetchIssues(final IssueQuery query) {
        final List<GitLabIssueResponse> gitLabIssues = gitLabRestClient
                .get()
                .uri(uriBuilder -> {
                    final URI requestUri = buildUri(query, uriBuilder);
                    log.debug("Calling GitLab uri={}", requestUri);
                    return requestUri;
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return gitLabIssues == null
                ? List.of()
                : gitLabIssues.stream().map(gitLabIssuesMapper::toIssue).toList();
    }

    @SuppressWarnings("null")
    private URI buildUri(final IssueQuery query, final UriBuilder uriBuilder) {
        final var builder = uriBuilder
                .path(gitLabUriFactory.projectResourcePath(RESOURCE_ISSUES))
                .queryParam("page", query.page())
                .queryParam("per_page", query.perPage());

        final var state = query.state();
        addOptionalParam(builder, "state", state == null ? null : state.value());
        addOptionalParam(builder, "labels", query.label());
        addOptionalParam(builder, "assignee_username", query.assignee());
        addOptionalParam(builder, "milestone", query.milestone());

        return builder.build(gitLabUriFactory.projectPath());
    }

    private IssuePage toIssuePage(final IssueQuery query, final List<IssueSummary> issues) {
        log.info("GitLab resource={} returned count={}", RESOURCE_ISSUES, issues.size());
        return IssuePage.builder()
                .items(issues)
                .count(issues.size())
                .page(query.page())
                .build();
    }

    @SuppressWarnings("null")
    private GitLabIssueResponse requestIssue(final CreateIssueInput input) {
        final GitLabIssueResponse issueResponse = gitLabRestClient
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path(gitLabUriFactory.projectResourcePath(RESOURCE_ISSUES))
                        .build(gitLabUriFactory.projectPath()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(gitLabIssuesMapper.toCreateRequest(input))
                .retrieve()
                .body(GitLabIssueResponse.class);

        if (issueResponse == null) {
            throw mapTransportFailure(EMPTY_BODY_CATEGORY, RESOURCE_CREATE_ISSUE);
        }
        return issueResponse;
    }

    @SuppressWarnings("null")
    private GitLabIssueResponse requestIssueUpdate(final UpdateIssueInput input) {
        final GitLabIssueResponse issueResponse = gitLabRestClient
                .put()
                .uri(uriBuilder ->
                        uriBuilder.path(issueByIdPath()).build(gitLabUriFactory.projectPath(), input.issueId()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(gitLabIssuesMapper.toUpdateRequest(input))
                .retrieve()
                .body(GitLabIssueResponse.class);

        if (issueResponse == null) {
            throw mapTransportFailure(EMPTY_BODY_CATEGORY, RESOURCE_UPDATE_ISSUE);
        }
        return issueResponse;
    }

    private @NonNull String issueByIdPath() {
        return gitLabUriFactory.projectResourcePath(RESOURCE_ISSUES) + "/{issueId}";
    }

    private IntegrationException mapHttpFailure(final RestClientResponseException exception, final String resource) {
        final IntegrationException mappedException =
                gitLabExceptionMapper.fromHttpFailure(exception, SOURCE_GITLAB, resource);
        log.warn(
                "GitLab request failed resource={} status={} category={}",
                resource,
                exception.getStatusCode().value(),
                mappedException.errorCode().name());
        return mappedException;
    }

    private IntegrationException mapTransportFailure(final String category, final String resource) {
        log.error("GitLab transport failure resource={} category={}", resource, category);
        return gitLabExceptionMapper.fromTransportFailure(SOURCE_GITLAB, resource);
    }

    private <T> T executeGitLabOperation(final String resource, final Supplier<T> operation) {
        try {
            return operation.get();
        } catch (final RestClientResponseException exception) {
            throw mapHttpFailure(exception, resource);
        } catch (final RuntimeException exception) {
            throw mapTransportFailure(exception.getClass().getSimpleName(), resource);
        }
    }

    private void executeGitLabOperation(final String resource, final Runnable operation) {
        executeGitLabOperation(resource, () -> {
            operation.run();
            return null;
        });
    }

    @SuppressWarnings("null")
    private void addOptionalParam(final UriBuilder uriBuilder, final String name, final Object value) {
        if (value != null) {
            uriBuilder.queryParam(name, value);
        }
    }
}
