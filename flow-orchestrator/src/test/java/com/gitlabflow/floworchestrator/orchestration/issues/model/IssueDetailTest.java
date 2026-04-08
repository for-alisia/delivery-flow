package com.gitlabflow.floworchestrator.orchestration.issues.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IssueDetailTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-01-04T15:31:51.081Z");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-03-12T09:00:00.000Z");

    @Test
    @DisplayName("normalizes null labels and assignees to immutable empty lists")
    void normalizesNullCollectionsToImmutableEmptyLists() {
        final IssueDetail detail = IssueDetail.builder()
                .issueId(42L)
                .title("Fix login bug")
                .description(null)
                .state("opened")
                .labels(null)
                .assignees(null)
                .milestone(null)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .closedAt(null)
                .build();
        final List<String> detailLabels = detail.labels();
        final List<IssueDetail.AssigneeDetail> detailAssignees = detail.assignees();

        assertThat(detailLabels).isEmpty();
        assertThat(detailAssignees).isEmpty();
        assertThatThrownBy(() -> detailLabels.add("bug")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> detailAssignees.add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("defensively copies labels and assignees")
    void defensivelyCopiesLabelsAndAssignees() {
        final List<String> labels = new ArrayList<>(List.of("bug", "high-priority"));
        final List<IssueDetail.AssigneeDetail> assignees = new ArrayList<>();
        assignees.add(IssueDetail.AssigneeDetail.builder()
                .id(10L)
                .username("john.doe")
                .name("John Doe")
                .build());

        final IssueDetail detail = IssueDetail.builder()
                .issueId(42L)
                .title("Fix login bug")
                .description("SSO broken")
                .state("opened")
                .labels(labels)
                .assignees(assignees)
                .milestone(null)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .closedAt(null)
                .build();

        labels.clear();
        assignees.clear();
        final List<String> detailLabels = detail.labels();
        final List<IssueDetail.AssigneeDetail> detailAssignees = detail.assignees();

        assertThat(detailLabels).containsExactly("bug", "high-priority");
        assertThat(detailAssignees).hasSize(1);
        assertThat(detailAssignees.getFirst().username()).isEqualTo("john.doe");
        assertThatThrownBy(() -> detailLabels.add("new-label")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> detailAssignees.add(null)).isInstanceOf(UnsupportedOperationException.class);
    }
}
