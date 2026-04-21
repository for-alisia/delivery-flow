package com.gitlabflow.floworchestrator.integration.gitlab.issues;

import com.gitlabflow.floworchestrator.integration.gitlab.GitLabOperationExecutor;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitLabIssuesAdapter implements IssuesPort {

    private static final String RESOURCE_ISSUES = "issues";
    private static final String RESOURCE_CREATE_ISSUE = "create issue";
    private static final String RESOURCE_UPDATE_ISSUE = "update issue";
    private static final String RESOURCE_DELETE_ISSUE = "delete issue";
    private static final String RESOURCE_GET_ISSUE_DETAIL = "get issue detail";
    private static final String RESOURCE_GET_LABEL_EVENTS = "get label events";
    private static final String EMPTY_BODY_MESSAGE = "GitLab returned empty response body";

    private final RestClient gitLabRestClient;
    private final GitLabUriFactory gitLabUriFactory;
    private final GitLabIssuesMapper gitLabIssuesMapper;
    private final GitLabIssueDetailMapper gitLabIssueDetailMapper;
    private final GitLabLabelEventMapper gitLabLabelEventMapper;
    private final GitLabOperationExecutor gitLabOperationExecutor;

    @Override
    public IssuePage getIssues(final IssueQuery query) {
        return gitLabOperationExecutor.execute(RESOURCE_ISSUES, () -> {
            final List<IssueSummary> issues = fetchIssues(query);
            return toIssuePage(query, issues);
        });
    }

    @Override
    public IssueSummary createIssue(final CreateIssueInput input) {
        log.info("Creating GitLab issue");

        return gitLabOperationExecutor.execute(RESOURCE_CREATE_ISSUE, () -> {
            final GitLabIssueResponse issueResponse = requestIssue(input);
            final IssueSummary issue = gitLabIssuesMapper.toIssue(issueResponse);
            log.info("GitLab issue created id={}", issue.id());
            return issue;
        });
    }

    @Override
    public IssueSummary updateIssue(final UpdateIssueInput input) {
        log.info("Updating GitLab issue issueId={}", input.issueId());

        return gitLabOperationExecutor.execute(RESOURCE_UPDATE_ISSUE, () -> {
            final GitLabIssueResponse issueResponse = requestIssueUpdate(input);
            final IssueSummary issue = gitLabIssuesMapper.toIssue(issueResponse);
            log.info("GitLab issue updated issueId={}", input.issueId());
            return issue;
        });
    }

    @Override
    public void deleteIssue(final long issueId) {
        log.info("Deleting GitLab issue issueId={}", issueId);

        gitLabOperationExecutor.execute(RESOURCE_DELETE_ISSUE, () -> {
            gitLabRestClient.delete().uri(issueByIdUri(issueId)).retrieve().toBodilessEntity();
            log.info("GitLab issue deleted issueId={}", issueId);
        });
    }

    @Override
    public IssueDetail getIssueDetail(final long issueId) {
        log.info("Fetching GitLab issue detail issueId={}", issueId);

        return gitLabOperationExecutor.execute(RESOURCE_GET_ISSUE_DETAIL, () -> {
            final GitLabSingleIssueResponse response =
                    gitLabRestClient.get().uri(issueByIdUri(issueId)).retrieve().body(GitLabSingleIssueResponse.class);

            if (response == null) {
                throw new IllegalStateException(EMPTY_BODY_MESSAGE);
            }

            final IssueDetail issueDetail = gitLabIssueDetailMapper.toIssueDetail(response);
            log.info("GitLab issue detail fetched issueId={}", issueId);
            return issueDetail;
        });
    }

    @Override
    public List<ChangeSet<?>> getLabelEvents(final long issueId) {
        log.info("Fetching GitLab label events issueId={}", issueId);

        return gitLabOperationExecutor.execute(RESOURCE_GET_LABEL_EVENTS, () -> {
            final List<GitLabLabelEventResponse> response = gitLabRestClient
                    .get()
                    .uri(issueLabelEventsUri(issueId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null) {
                throw new IllegalStateException(EMPTY_BODY_MESSAGE);
            }

            final List<ChangeSet<?>> changeSets = gitLabLabelEventMapper.toLabelChangeSets(response);
            log.info("GitLab label events fetched issueId={} count={}", issueId, changeSets.size());
            return changeSets;
        });
    }

    private List<IssueSummary> fetchIssues(final IssueQuery query) {
        final URI requestUri = buildUri(query);
        log.debug("Calling GitLab uri={}", requestUri);

        final List<GitLabIssueResponse> gitLabIssues =
                gitLabRestClient.get().uri(requestUri).retrieve().body(new ParameterizedTypeReference<>() {});

        return gitLabIssues == null
                ? List.of()
                : gitLabIssues.stream().map(gitLabIssuesMapper::toIssue).toList();
    }

    private @NonNull URI buildUri(final IssueQuery query) {
        final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(gitLabUriFactory.apiBaseUrl())
                .path(issuesResourcePathTemplate())
                .queryParam("page", query.page())
                .queryParam("per_page", query.perPage());

        final var state = query.state();
        addOptionalParam(builder, "state", state == null ? null : state.value());
        addOptionalParam(builder, "labels", query.label());
        addOptionalParam(builder, "assignee_username", query.assignee());
        addOptionalParam(builder, "milestone", query.milestone());

        return builder.encode().buildAndExpand(gitLabUriFactory.projectPath()).toUri();
    }

    private IssuePage toIssuePage(final IssueQuery query, final List<IssueSummary> issues) {
        log.info("GitLab resource={} returned count={}", RESOURCE_ISSUES, issues.size());
        return IssuePage.builder()
                .items(issues)
                .count(issues.size())
                .page(query.page())
                .build();
    }

    private GitLabIssueResponse requestIssue(final CreateIssueInput input) {
        final GitLabIssueResponse issueResponse = gitLabRestClient
                .post()
                .uri(issueCollectionUri())
                .contentType(jsonContentType())
                .body(createIssueBody(input))
                .retrieve()
                .body(GitLabIssueResponse.class);

        if (issueResponse == null) {
            throw new IllegalStateException(EMPTY_BODY_MESSAGE);
        }
        return issueResponse;
    }

    private GitLabIssueResponse requestIssueUpdate(final UpdateIssueInput input) {
        final GitLabIssueResponse issueResponse = gitLabRestClient
                .put()
                .uri(issueByIdUri(input.issueId()))
                .contentType(jsonContentType())
                .body(updateIssueBody(input))
                .retrieve()
                .body(GitLabIssueResponse.class);

        if (issueResponse == null) {
            throw new IllegalStateException(EMPTY_BODY_MESSAGE);
        }
        return issueResponse;
    }

    private @NonNull URI issueCollectionUri() {
        return UriComponentsBuilder.fromUriString(gitLabUriFactory.apiBaseUrl())
                .path(issuesResourcePathTemplate())
                .encode()
                .buildAndExpand(gitLabUriFactory.projectPath())
                .toUri();
    }

    private @NonNull URI issueByIdUri(final long issueId) {
        return UriComponentsBuilder.fromUriString(gitLabUriFactory.apiBaseUrl())
                .path(issueByIdPath())
                .encode()
                .buildAndExpand(gitLabUriFactory.projectPath(), issueId)
                .toUri();
    }

    private @NonNull URI issueLabelEventsUri(final long issueId) {
        final String resourcePath = issuesResourcePathTemplate() + "/{issueId}/resource_label_events";
        return UriComponentsBuilder.fromUriString(gitLabUriFactory.apiBaseUrl())
                .path(resourcePath)
                .encode()
                .buildAndExpand(gitLabUriFactory.projectPath(), issueId)
                .toUri();
    }

    private @NonNull String issueByIdPath() {
        return issuesResourcePathTemplate() + "/{issueId}";
    }

    private @NonNull String issuesResourcePathTemplate() {
        return "/projects/{projectPath}/" + RESOURCE_ISSUES;
    }

    @SuppressWarnings("null")
    private @NonNull Object createIssueBody(final CreateIssueInput input) {
        return gitLabIssuesMapper.toCreateRequest(input);
    }

    @SuppressWarnings("null")
    private @NonNull Object updateIssueBody(final UpdateIssueInput input) {
        return gitLabIssuesMapper.toUpdateRequest(input);
    }

    @SuppressWarnings("null")
    private @NonNull MediaType jsonContentType() {
        return MediaType.APPLICATION_JSON;
    }

    private void addOptionalParam(
            final UriComponentsBuilder uriBuilder, final @NonNull String name, final Object value) {
        if (value != null) {
            uriBuilder.queryParam(name, value);
        }
    }
}
