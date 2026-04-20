package com.gitlabflow.floworchestrator.integration.gitlab.issues.mapper;

import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabCreateIssueRequest;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabIssueResponse;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabUpdateIssueRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueSummary;
import com.gitlabflow.floworchestrator.orchestration.issues.model.UpdateIssueInput;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GitLabIssuesMapper {

    public IssueSummary toIssue(final GitLabIssueResponse issueResponse) {
        final String assignee = mapAssignee(issueResponse);
        final String milestone = Optional.ofNullable(issueResponse.milestone())
                .map(GitLabIssueResponse.GitLabMilestone::title)
                .orElse(null);
        final Long parent = Optional.ofNullable(issueResponse.epic())
                .map(GitLabIssueResponse.GitLabEpic::id)
                .orElse(null);

        log.debug(
                "Mapped GitLab issue id={} issueId={} state={} labels={} assigneePresent={} milestonePresent={} parentPresent={}",
                issueResponse.id(),
                issueResponse.iid(),
                issueResponse.state(),
                issueResponse.labels().size(),
                assignee != null,
                milestone != null,
                parent != null);

        return IssueSummary.builder()
                .id(issueResponse.id())
                .issueId(issueResponse.iid())
                .title(issueResponse.title())
                .description(issueResponse.description())
                .state(issueResponse.state())
                .labels(issueResponse.labels())
                .assignee(assignee)
                .milestone(milestone)
                .parent(parent)
                .build();
    }

    public GitLabCreateIssueRequest toCreateRequest(final CreateIssueInput input) {
        final String labels = toCommaSeparatedLabels(input.labels());

        log.debug(
                "Mapped create issue request descriptionPresent={} labelCount={}",
                input.description() != null,
                input.labels().size());

        return GitLabCreateIssueRequest.builder()
                .title(input.title())
                .description(input.description())
                .labels(labels)
                .build();
    }

    public GitLabUpdateIssueRequest toUpdateRequest(final UpdateIssueInput input) {
        final String addLabels = toCommaSeparatedLabels(input.addLabels());
        final String removeLabels = toCommaSeparatedLabels(input.removeLabels());

        log.debug(
                "Mapped update issue request issueId={} descriptionPresent={} addLabelCount={} removeLabelCount={}",
                input.issueId(),
                input.description() != null,
                input.addLabels().size(),
                input.removeLabels().size());

        return GitLabUpdateIssueRequest.builder()
                .title(input.title())
                .description(input.description())
                .addLabels(addLabels)
                .removeLabels(removeLabels)
                .build();
    }

    private String toCommaSeparatedLabels(final List<String> labels) {
        return labels.isEmpty() ? null : String.join(",", labels);
    }

    private String mapAssignee(final GitLabIssueResponse issueResponse) {
        final String firstAssignee = Optional.ofNullable(issueResponse.assignees()).stream()
                .flatMap(List::stream)
                .map(GitLabIssueResponse.GitLabAssignee::username)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (firstAssignee != null) {
            return firstAssignee;
        }

        return Optional.ofNullable(issueResponse.assignee())
                .map(GitLabIssueResponse.GitLabAssignee::username)
                .orElse(null);
    }
}
