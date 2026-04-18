package com.gitlabflow.floworchestrator.orchestration.issues.rest.mapper;

import com.gitlabflow.floworchestrator.orchestration.issues.model.ChangeField;
import com.gitlabflow.floworchestrator.orchestration.issues.model.ChangeSet;
import com.gitlabflow.floworchestrator.orchestration.issues.model.EnrichedIssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.Issue;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.model.LabelChangeSet;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDetailDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDetailDto.AssigneeDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDetailDto.ChangeSetDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDetailDto.ChangedByDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDetailDto.LabelChangeDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDetailDto.LabelChangeSetDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDetailDto.MilestoneDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssueDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesResponse;
import java.util.List;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class IssuesResponseMapper {

    public SearchIssuesResponse toSearchIssuesResponse(final IssuePage issuePage) {
        return SearchIssuesResponse.builder()
                .items(issuePage.items().stream().map(this::toSearchIssueDto).toList())
                .count(issuePage.count())
                .page(issuePage.page())
                .build();
    }

    public SearchIssueDto toSearchIssueDto(final Issue issue) {
        return SearchIssueDto.builder()
                .id(issue.id())
                .issueId(issue.issueId())
                .title(issue.title())
                .description(issue.description())
                .state(issue.state())
                .labels(issue.labels())
                .assignee(issue.assignee())
                .milestone(issue.milestone())
                .parent(issue.parent())
                .changeSets(toSearchChangeSetDtos(issue.changeSets()))
                .build();
    }

    public IssueDto toIssueDto(final Issue issue) {
        return IssueDto.builder()
                .id(issue.id())
                .issueId(issue.issueId())
                .title(issue.title())
                .description(issue.description())
                .state(issue.state())
                .labels(issue.labels())
                .assignee(issue.assignee())
                .milestone(issue.milestone())
                .parent(issue.parent())
                .build();
    }

    public IssueDetailDto toIssueDetailDto(final EnrichedIssueDetail enriched) {
        final IssueDetail issueDetail = enriched.issueDetail();
        final var assignees = issueDetail.assignees().stream()
                .map(a -> AssigneeDto.builder()
                        .id(a.id())
                        .username(a.username())
                        .name(a.name())
                        .build())
                .toList();

        final var rawMilestone = issueDetail.milestone();
        final MilestoneDto milestone = rawMilestone == null
                ? null
                : MilestoneDto.builder()
                        .id(rawMilestone.id())
                        .milestoneId(rawMilestone.milestoneId())
                        .title(rawMilestone.title())
                        .state(rawMilestone.state())
                        .dueDate(rawMilestone.dueDate())
                        .build();

        return IssueDetailDto.builder()
                .issueId(issueDetail.issueId())
                .title(issueDetail.title())
                .description(issueDetail.description())
                .state(issueDetail.state())
                .labels(issueDetail.labels())
                .assignees(assignees)
                .milestone(milestone)
                .createdAt(issueDetail.createdAt())
                .updatedAt(issueDetail.updatedAt())
                .closedAt(issueDetail.closedAt())
                .changeSets(toChangeSetDtos(enriched.changeSets()))
                .build();
    }

    private List<ChangeSetDto> toChangeSetDtos(final List<ChangeSet> changeSets) {
        return changeSets.stream().map(this::toChangeSetDto).toList();
    }

    private ChangeSetDto toChangeSetDto(final ChangeSet changeSet) {
        if (changeSet instanceof LabelChangeSet labelChangeSet) {
            return LabelChangeSetDto.builder()
                    .changeType(labelChangeSet.changeType())
                    .changedBy(toChangedByDto(labelChangeSet))
                    .change(toLabelChangeDto(labelChangeSet))
                    .changedAt(labelChangeSet.changedAt())
                    .build();
        }

        throw new IllegalArgumentException(
                "Unsupported ChangeSet type: " + changeSet.getClass().getName());
    }

    private ChangedByDto toChangedByDto(final LabelChangeSet labelChangeSet) {
        final var changedBy = labelChangeSet.changedBy();
        return ChangedByDto.builder()
                .id(changedBy.id())
                .username(changedBy.username())
                .name(changedBy.name())
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

    private @Nullable List<SearchIssueDto.ChangeSetDto> toSearchChangeSetDtos(final List<ChangeSet> changeSets) {
        if (changeSets == null) {
            return null;
        }
        return changeSets.stream().map(this::toSearchChangeSetDto).toList();
    }

    private SearchIssueDto.ChangeSetDto toSearchChangeSetDto(final ChangeSet changeSet) {
        if (changeSet instanceof LabelChangeSet labelChangeSet) {
            return SearchIssueDto.ChangeSetDto.builder()
                    .changeType(labelChangeSet.changeType())
                    .changedBy(toSearchChangedByDto(labelChangeSet))
                    .change(toSearchLabelChangeDto(labelChangeSet))
                    .changedAt(labelChangeSet.changedAt())
                    .build();
        }

        throw new IllegalArgumentException(
                "Unsupported ChangeSet type: " + changeSet.getClass().getName());
    }

    private SearchIssueDto.ChangedByDto toSearchChangedByDto(final LabelChangeSet labelChangeSet) {
        final var changedBy = labelChangeSet.changedBy();
        return SearchIssueDto.ChangedByDto.builder()
                .id(changedBy.id())
                .username(changedBy.username())
                .name(changedBy.name())
                .build();
    }

    private SearchIssueDto.LabelChangeDto toSearchLabelChangeDto(final LabelChangeSet labelChangeSet) {
        final var change = labelChangeSet.change();
        return SearchIssueDto.LabelChangeDto.builder()
                .field(toSearchChangeField(change.field()))
                .id(change.id())
                .name(change.name())
                .build();
    }

    private String toSearchChangeField(final ChangeField changeField) {
        return switch (changeField) {
            case LABEL -> "label";
        };
    }
}
