package com.gitlabflow.floworchestrator.integration.gitlab.issues.mapper;

import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabSingleIssueResponse;
import com.gitlabflow.floworchestrator.orchestration.common.model.User;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueDetail;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.Milestone;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GitLabIssueDetailMapper {

    public IssueDetail toIssueDetail(final GitLabSingleIssueResponse response) {
        final List<User> assignees = response.assignees().stream()
                .map(a -> User.builder()
                        .id(a.id())
                        .username(a.username())
                        .name(a.name())
                        .build())
                .toList();

        final var rawMilestone = response.milestone();
        final Milestone milestone = rawMilestone == null
                ? null
                : Milestone.builder()
                        .id(rawMilestone.id())
                        .milestoneId(rawMilestone.iid())
                        .title(rawMilestone.title())
                        .description(rawMilestone.description())
                        .startDate(rawMilestone.startDate())
                        .dueDate(rawMilestone.dueDate())
                        .state(rawMilestone.state())
                        .build();

        log.debug(
                "Mapped GitLab single issue id={} issueId={} state={} assigneeCount={} milestonePresent={}",
                response.id(),
                response.iid(),
                response.state(),
                assignees.size(),
                milestone != null);

        return IssueDetail.builder()
                .issueId(response.iid())
                .title(response.title())
                .description(response.description())
                .state(response.state())
                .labels(response.labels())
                .assignees(assignees)
                .milestone(milestone)
                .createdAt(response.createdAt())
                .updatedAt(response.updatedAt())
                .closedAt(response.closedAt())
                .build();
    }
}
