package com.gitlabflow.floworchestrator.orchestration.issues.rest.mapper;

import com.gitlabflow.floworchestrator.orchestration.common.model.ChangeSet;
import com.gitlabflow.floworchestrator.orchestration.common.model.User;
import com.gitlabflow.floworchestrator.orchestration.common.rest.dto.ChangeSetDto;
import com.gitlabflow.floworchestrator.orchestration.common.rest.dto.LabelChangeDto;
import com.gitlabflow.floworchestrator.orchestration.common.rest.dto.LabelChangeSetDto;
import com.gitlabflow.floworchestrator.orchestration.common.rest.dto.UserDto;
import com.gitlabflow.floworchestrator.orchestration.issues.model.EnrichedIssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueSummary;
import com.gitlabflow.floworchestrator.orchestration.issues.model.LabelChangeSet;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDetailDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueSummaryDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesResponse;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.Milestone;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.MilestoneDto;
import java.util.List;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class IssuesResponseMapper {

    public SearchIssuesResponse toSearchIssuesResponse(final IssuePage issuePage) {
        return SearchIssuesResponse.builder()
                .items(issuePage.items().stream().map(this::toIssueSummaryDto).toList())
                .count(issuePage.count())
                .page(issuePage.page())
                .build();
    }

    public IssueSummaryDto toIssueSummaryDto(final IssueSummary issue) {
        return IssueSummaryDto.builder()
                .id(issue.id())
                .issueId(issue.issueId())
                .title(issue.title())
                .description(issue.description())
                .state(issue.state())
                .labels(issue.labels())
                .assignee(issue.assignee())
                .milestone(issue.milestone())
                .parent(issue.parent())
                .changeSets(toNullableChangeSetDtos(issue.changeSets()))
                .build();
    }

    public IssueDetailDto toIssueDetailDto(final EnrichedIssueDetail enriched) {
        final IssueDetail issueDetail = enriched.issueDetail();

        return IssueDetailDto.builder()
                .issueId(issueDetail.issueId())
                .title(issueDetail.title())
                .description(issueDetail.description())
                .state(issueDetail.state())
                .labels(issueDetail.labels())
                .assignees(issueDetail.assignees().stream().map(this::toUserDto).toList())
                .milestone(toMilestoneDto(issueDetail.milestone()))
                .createdAt(issueDetail.createdAt())
                .updatedAt(issueDetail.updatedAt())
                .closedAt(issueDetail.closedAt())
                .changeSets(toChangeSetDtos(enriched.changeSets()))
                .build();
    }

    private @Nullable List<ChangeSetDto> toNullableChangeSetDtos(final @Nullable List<ChangeSet<?>> changeSets) {
        if (changeSets == null) {
            return null;
        }
        return toChangeSetDtos(changeSets);
    }

    private List<ChangeSetDto> toChangeSetDtos(final List<ChangeSet<?>> changeSets) {
        return changeSets.stream().map(this::toChangeSetDto).toList();
    }

    private ChangeSetDto toChangeSetDto(final ChangeSet<?> changeSet) {
        if (changeSet instanceof LabelChangeSet labelChangeSet) {
            return LabelChangeSetDto.builder()
                    .changeType(labelChangeSet.changeType())
                    .changedBy(toUserDto(labelChangeSet.changedBy()))
                    .change(toLabelChangeDto(labelChangeSet))
                    .changedAt(labelChangeSet.changedAt())
                    .build();
        }

        throw new IllegalArgumentException(
                "Unsupported ChangeSet type: " + changeSet.getClass().getName());
    }

    private UserDto toUserDto(final User user) {
        return UserDto.builder()
                .id(user.id())
                .username(user.username())
                .name(user.name())
                .build();
    }

    private @Nullable MilestoneDto toMilestoneDto(final @Nullable Milestone milestone) {
        if (milestone == null) {
            return null;
        }

        return MilestoneDto.builder()
                .id(milestone.id())
                .milestoneId(milestone.milestoneId())
                .title(milestone.title())
                .state(milestone.state())
                .dueDate(milestone.dueDate())
                .build();
    }

    private LabelChangeDto toLabelChangeDto(final LabelChangeSet labelChangeSet) {
        final var change = labelChangeSet.change();
        return LabelChangeDto.builder()
                .field(change.field())
                .id(change.id())
                .name(change.name())
                .build();
    }
}
