package com.gitlabflow.floworchestrator.integration.gitlab.milestones;

import com.gitlabflow.floworchestrator.common.util.ElapsedTime;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabOffsetPaginationLoader;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabProjectLocator;
import com.gitlabflow.floworchestrator.integration.gitlab.GitLabUriFactory;
import com.gitlabflow.floworchestrator.integration.gitlab.milestones.dto.GitLabMilestoneResponse;
import com.gitlabflow.floworchestrator.integration.gitlab.milestones.mapper.GitLabMilestonesMapper;
import com.gitlabflow.floworchestrator.orchestration.milestones.MilestonesPort;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.Milestone;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.MilestoneState;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.SearchMilestonesInput;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitLabMilestonesAdapter implements MilestonesPort {

    private static final String RESOURCE_MILESTONES = "milestones";

    private final RestClient gitLabRestClient;
    private final GitLabProjectLocator gitLabProjectLocator;
    private final GitLabUriFactory gitLabUriFactory;
    private final GitLabOffsetPaginationLoader gitLabOffsetPaginationLoader;
    private final GitLabMilestonesMapper gitLabMilestonesMapper;

    @Override
    public List<Milestone> searchMilestones(final SearchMilestonesInput input) {
        final long startedAt = System.nanoTime();
        log.info(
                "Fetching GitLab milestones state={} titleSearch={} milestoneIds={}",
                input.state().value(),
                input.titleSearch(),
                input.milestoneIds());

        final List<GitLabMilestoneResponse> milestoneResponses = gitLabOffsetPaginationLoader.loadAllPages(
                RESOURCE_MILESTONES, (page, perPage) -> fetchPage(input, page, perPage));

        final List<Milestone> milestones = milestoneResponses.stream()
                .map(gitLabMilestonesMapper::toMilestone)
                .toList();

        log.info(
                "GitLab milestones fetched resultCount={} durationMs={}",
                milestones.size(),
                ElapsedTime.toDurationMs(startedAt));
        return milestones;
    }

    private List<GitLabMilestoneResponse> fetchPage(
            final SearchMilestonesInput input, final int page, final int perPage) {
        return gitLabRestClient
                .get()
                .uri(buildMilestonesUri(input, page, perPage))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    private @NonNull URI buildMilestonesUri(final SearchMilestonesInput input, final int page, final int perPage) {
        final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(gitLabUriFactory.apiBaseUrl())
                .path(milestonesResourcePathTemplate())
                .queryParam("page", page)
                .queryParam("per_page", perPage);

        addStateParam(builder, input.state());
        addOptionalParam(builder, "search", input.titleSearch());
        addMilestoneIdsParam(builder, input.milestoneIds());

        return builder.encode()
                .buildAndExpand(gitLabProjectLocator.projectReference().projectPath())
                .toUri();
    }

    private @NonNull String milestonesResourcePathTemplate() {
        return "/projects/{projectPath}/" + RESOURCE_MILESTONES;
    }

    private void addStateParam(final UriComponentsBuilder uriBuilder, final MilestoneState state) {
        if (state != MilestoneState.ALL) {
            uriBuilder.queryParam("state", state.value());
        }
    }

    private void addOptionalParam(
            final UriComponentsBuilder uriBuilder, final @NonNull String name, final Object value) {
        if (value != null) {
            uriBuilder.queryParam(name, value);
        }
    }

    private void addMilestoneIdsParam(final UriComponentsBuilder uriBuilder, final List<Long> milestoneIds) {
        for (final Long milestoneId : milestoneIds) {
            if (milestoneId != null) {
                uriBuilder.queryParam("iids[]", milestoneId);
            }
        }
    }
}
