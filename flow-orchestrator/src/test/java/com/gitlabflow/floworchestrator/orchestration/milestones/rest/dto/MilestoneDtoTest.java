package com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MilestoneDtoTest {

    @Test
    @DisplayName("builder carries description startDate dueDate and state")
    void builderCarriesExpandedFields() {
        final MilestoneDto dto = MilestoneDto.builder()
                .id(12L)
                .milestoneId(3L)
                .title("Release 1.0")
                .description("Version")
                .startDate("2026-05-01")
                .dueDate("2026-05-15")
                .state("active")
                .build();

        assertThat(dto.id()).isEqualTo(12L);
        assertThat(dto.milestoneId()).isEqualTo(3L);
        assertThat(dto.title()).isEqualTo("Release 1.0");
        assertThat(dto.description()).isEqualTo("Version");
        assertThat(dto.startDate()).isEqualTo("2026-05-01");
        assertThat(dto.dueDate()).isEqualTo("2026-05-15");
        assertThat(dto.state()).isEqualTo("active");
    }

    @Test
    @DisplayName("preserves nullable description startDate and dueDate")
    void preservesNullableDescriptionStartDateAndDueDate() {
        final MilestoneDto dto = MilestoneDto.builder()
                .id(13L)
                .milestoneId(4L)
                .title("Release 1.1")
                .description(null)
                .startDate(null)
                .dueDate(null)
                .state("closed")
                .build();

        assertThat(dto.description()).isNull();
        assertThat(dto.startDate()).isNull();
        assertThat(dto.dueDate()).isNull();
        assertThat(MilestoneDto.class.isRecord()).isTrue();
    }
}
