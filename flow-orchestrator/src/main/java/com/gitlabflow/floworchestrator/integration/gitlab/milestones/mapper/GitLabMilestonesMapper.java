package com.gitlabflow.floworchestrator.integration.gitlab.milestones.mapper;

import com.gitlabflow.floworchestrator.integration.gitlab.milestones.dto.GitLabMilestoneResponse;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.Milestone;
import org.springframework.stereotype.Component;

@Component
public class GitLabMilestonesMapper {

    public Milestone toMilestone(final GitLabMilestoneResponse response) {
        return Milestone.builder()
                .id(response.id())
                .milestoneId(response.iid())
                .title(response.title())
                .description(response.description())
                .startDate(response.startDate())
                .dueDate(response.dueDate())
                .state(response.state())
                .build();
    }
}
