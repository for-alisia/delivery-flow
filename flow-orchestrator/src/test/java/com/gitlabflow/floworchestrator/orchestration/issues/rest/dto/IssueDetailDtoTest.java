package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IssueDetailDtoTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-01-04T15:31:51.081Z");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-03-12T09:00:00.000Z");

    @Test
    @DisplayName("normalizes null labels assignees and changeSets to immutable empty lists")
    void normalizesNullCollectionsToImmutableEmptyLists() {
        final IssueDetailDto dto = IssueDetailDto.builder()
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
                .changeSets(null)
                .build();
        final List<String> dtoLabels = dto.labels();
        final List<IssueDetailDto.AssigneeDto> dtoAssignees = dto.assignees();
        final List<Object> dtoChangeSets = dto.changeSets();

        assertThat(dtoLabels).isEmpty();
        assertThat(dtoAssignees).isEmpty();
        assertThat(dtoChangeSets).isEmpty();
        assertThatThrownBy(() -> dtoLabels.add("bug")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> dtoAssignees.add(null)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> dtoChangeSets.add("x")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("defensively copies labels assignees and changeSets")
    void defensivelyCopiesCollections() {
        final List<String> labels = new ArrayList<>(List.of("bug"));
        final List<IssueDetailDto.AssigneeDto> assignees = new ArrayList<>();
        assignees.add(IssueDetailDto.AssigneeDto.builder()
                .id(10L)
                .username("john.doe")
                .name("John Doe")
                .build());
        final List<Object> changeSets = new ArrayList<>(List.of("change-1"));

        final IssueDetailDto dto = IssueDetailDto.builder()
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
                .changeSets(changeSets)
                .build();

        labels.clear();
        assignees.clear();
        changeSets.clear();
        final List<String> dtoLabels = dto.labels();
        final List<IssueDetailDto.AssigneeDto> dtoAssignees = dto.assignees();
        final List<Object> dtoChangeSets = dto.changeSets();

        assertThat(dtoLabels).containsExactly("bug");
        assertThat(dtoAssignees).hasSize(1);
        assertThat(dtoAssignees.getFirst().username()).isEqualTo("john.doe");
        assertThat(dtoChangeSets).containsExactly("change-1");
        assertThatThrownBy(() -> dtoLabels.add("new")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> dtoAssignees.add(null)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> dtoChangeSets.add("new-change")).isInstanceOf(UnsupportedOperationException.class);
    }
}
