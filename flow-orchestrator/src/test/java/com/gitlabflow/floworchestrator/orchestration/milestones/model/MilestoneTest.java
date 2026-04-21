package com.gitlabflow.floworchestrator.orchestration.milestones.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MilestoneTest {

    @Test
    @DisplayName("builder carries description startDate dueDate and state")
    void builderCarriesExpandedFields() {
        final Milestone milestone = Milestone.builder()
                .id(12L)
                .milestoneId(3L)
                .title("Release 1.0")
                .description("Version")
                .startDate("2026-05-01")
                .dueDate("2026-05-15")
                .state("active")
                .build();

        assertThat(milestone.id()).isEqualTo(12L);
        assertThat(milestone.milestoneId()).isEqualTo(3L);
        assertThat(milestone.title()).isEqualTo("Release 1.0");
        assertThat(milestone.description()).isEqualTo("Version");
        assertThat(milestone.startDate()).isEqualTo("2026-05-01");
        assertThat(milestone.dueDate()).isEqualTo("2026-05-15");
        assertThat(milestone.state()).isEqualTo("active");
    }

    @Test
    @DisplayName("keeps nullable description startDate and dueDate")
    void keepsNullableDescriptionStartDateAndDueDate() {
        final Milestone milestone = Milestone.builder()
                .id(13L)
                .milestoneId(4L)
                .title("Release 1.1")
                .description(null)
                .startDate(null)
                .dueDate(null)
                .state("closed")
                .build();

        assertThat(milestone.description()).isNull();
        assertThat(milestone.startDate()).isNull();
        assertThat(milestone.dueDate()).isNull();
        assertThat(Milestone.class.isRecord()).isTrue();
    }
}
