package com.gitlabflow.floworchestrator.integration.gitlab.issues;

import com.gitlabflow.floworchestrator.common.errors.IntegrationException;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabExceptionMapper;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabProjectLocator;
import com.gitlabflow.floworchestrator.orchestration.issues.GetIssuesPort;
import com.gitlabflow.floworchestrator.orchestration.issues.Issue;
import com.gitlabflow.floworchestrator.orchestration.issues.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.IssueQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitLabIssuesAdapter implements GetIssuesPort {

    private static final String SOURCE_GITLAB = "gitlab";

    private final RestClient gitLabRestClient;
    private final GitLabProjectLocator gitLabProjectLocator;
    private final GitLabIssueMapper gitLabIssueMapper;
    private final GitLabExceptionMapper gitLabExceptionMapper;

    @Override
    public IssuePage getIssues(final IssueQuery query) {
        final String resource = "project issues";

        try {
            final List<Issue> issues = fetchIssues(query);
            return toIssuePage(query, resource, issues);
        } catch (final RestClientResponseException exception) {
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
            throw mappedException;
        } catch (final RestClientException exception) {
            log.error("GitLab transport failure resource={} category={}", resource, exception.getClass().getSimpleName());
            throw gitLabExceptionMapper.fromTransportFailure(SOURCE_GITLAB, resource);
        } catch (final RuntimeException exception) {
            log.error("GitLab unexpected failure resource={} category={}", resource, exception.getClass().getSimpleName());
            throw gitLabExceptionMapper.fromTransportFailure(SOURCE_GITLAB, resource);
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
                : gitLabIssues.stream().map(gitLabIssueMapper::toIssue).toList();
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

    private IssuePage toIssuePage(final IssueQuery query, final String resource, final List<Issue> issues) {
        log.info("GitLab resource={} returned count={}", resource, issues.size());
        return new IssuePage(issues, issues.size(), query.page());
    }
}