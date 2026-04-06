package com.gitlabflow.floworchestrator.integration.gitlab.issues.mapper;

import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabCreateIssueRequest;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabIssueResponse;
import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.Issue;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GitLabIssuesMapper {

    public Issue toIssue(final GitLabIssueResponse issueResponse) {
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

        return new Issue(
                issueResponse.id(),
                issueResponse.iid(),
                issueResponse.title(),
                issueResponse.description(),
                issueResponse.state(),
                issueResponse.labels(),
                assignee,
                milestone,
                parent);
    }

    public GitLabCreateIssueRequest toCreateRequest(final CreateIssueInput input) {
        final String labels = input.labels().isEmpty() ? null : String.join(",", input.labels());

        log.debug(
                "Mapped create issue request descriptionPresent={} labelCount={}",
                input.description() != null,
                input.labels().size());

        return new GitLabCreateIssueRequest(input.title(), input.description(), labels);
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
