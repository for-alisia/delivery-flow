package com.gitlabflow.floworchestrator.integration.gitlab.issues;

import com.gitlabflow.floworchestrator.common.error.IntegrationException;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabExceptionMapper;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabProjectLocator;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabIssueResponse;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.mapper.GitLabIssuesMapper;
import com.gitlabflow.floworchestrator.orchestration.issues.IssuesPort;
import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.Issue;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery;
import org.springframework.core.ParameterizedTypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitLabIssuesAdapter implements IssuesPort {

    private static final String SOURCE_GITLAB = "gitlab";
    private static final String RESOURCE_ISSUES = "issues";
    private static final String RESOURCE_CREATE_ISSUE = "create issue";
    private static final String EMPTY_BODY_CATEGORY = "EmptyBody";

    private final RestClient gitLabRestClient;
    private final GitLabProjectLocator gitLabProjectLocator;
    private final GitLabIssuesMapper gitLabIssuesMapper;
    private final GitLabExceptionMapper gitLabExceptionMapper;

    @Override
    public IssuePage getIssues(final IssueQuery query) {
        try {
            final List<Issue> issues = fetchIssues(query);
            return toIssuePage(query, issues);
        } catch (final RestClientResponseException exception) {
            throw mapHttpFailure(exception, RESOURCE_ISSUES);
        } catch (final RuntimeException exception) {
            throw mapTransportFailure(exception.getClass().getSimpleName(), RESOURCE_ISSUES);
        }
    }

    @Override
    public Issue createIssue(final CreateIssueInput input) {
        log.info("Creating GitLab issue");

        try {
            final GitLabIssueResponse issueResponse = requestIssue(input);
            final Issue issue = gitLabIssuesMapper.toIssue(issueResponse);
            log.info("GitLab issue created id={}", issue.id());
            return issue;
        } catch (final RestClientResponseException exception) {
            throw mapHttpFailure(exception, RESOURCE_CREATE_ISSUE);
        } catch (final RuntimeException exception) {
            throw mapTransportFailure(exception.getClass().getSimpleName(), RESOURCE_CREATE_ISSUE);
        }
    }

    private List<Issue> fetchIssues(final IssueQuery query) {
        final List<GitLabIssueResponse> gitLabIssues = gitLabRestClient.get()
                .uri(uriBuilder -> {
                    final URI requestUri = buildUri(query, uriBuilder);
                    log.debug("Calling GitLab uri={}", requestUri);
                    return requestUri;
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        return gitLabIssues == null
                ? List.of()
            : gitLabIssues.stream().map(gitLabIssuesMapper::toIssue).toList();
    }

    private URI buildUri(final IssueQuery query, final org.springframework.web.util.UriBuilder uriBuilder) {
        final var builder = uriBuilder
                .path("/projects/{projectPath}/issues")
                .queryParam("page", query.page())
                .queryParam("per_page", query.perPage());

        final var state = query.state();
        if (state != null) {
            builder.queryParam("state", state.value());
        }

        final String label = query.label();
        if (label != null) {
            builder.queryParam("labels", label);
        }

        final String assignee = query.assignee();
        if (assignee != null) {
            builder.queryParam("assignee_username", assignee);
        }

        final String milestone = query.milestone();
        if (milestone != null) {
            builder.queryParam("milestone", milestone);
        }

        return builder.build(gitLabProjectLocator.projectReference().projectPath());
    }

    private IssuePage toIssuePage(final IssueQuery query, final List<Issue> issues) {
        log.info("GitLab resource={} returned count={}", RESOURCE_ISSUES, issues.size());
        return new IssuePage(issues, issues.size(), query.page());
    }

        private GitLabIssueResponse requestIssue(final CreateIssueInput input) {
        final GitLabIssueResponse issueResponse = gitLabRestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/projects/{projectPath}/issues")
                        .build(gitLabProjectLocator.projectReference().projectPath()))
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
            .body(Objects.requireNonNull(gitLabIssuesMapper.toCreateRequest(input)))
                .retrieve()
                .body(GitLabIssueResponse.class);

        if (issueResponse == null) {
            throw mapTransportFailure(EMPTY_BODY_CATEGORY, RESOURCE_CREATE_ISSUE);
        }
        return issueResponse;
    }

    private IntegrationException mapHttpFailure(
            final RestClientResponseException exception,
            final String resource
    ) {
        final IntegrationException mappedException = gitLabExceptionMapper.fromHttpFailure(
                exception,
                SOURCE_GITLAB,
                resource
        );
        log.warn(
                "GitLab request failed resource={} status={} category={}",
                resource,
                exception.getStatusCode().value(),
                mappedException.errorCode().name()
        );
        return mappedException;
    }

    private IntegrationException mapTransportFailure(final String category, final String resource) {
        log.error("GitLab transport failure resource={} category={}", resource, category);
        return gitLabExceptionMapper.fromTransportFailure(SOURCE_GITLAB, resource);
    }
}