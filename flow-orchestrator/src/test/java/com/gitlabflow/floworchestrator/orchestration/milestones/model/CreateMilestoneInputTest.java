package com.gitlabflow.floworchestrator.orchestration.milestones.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateMilestoneInputTest {

    @Test
    @DisplayName("builder preserves provided create milestone fields")
    void builderPreservesProvidedCreateMilestoneFields() {
        final CreateMilestoneInput input = CreateMilestoneInput.builder()
                .title("Q2 2026 Delivery")
                .description("Second quarter release cycle")
                .startDate("2026-04-01")
                .dueDate("2026-06-30")
                .build();

        assertThat(input.title()).isEqualTo("Q2 2026 Delivery");
        assertThat(input.description()).isEqualTo("Second quarter release cycle");
        assertThat(input.startDate()).isEqualTo("2026-04-01");
        assertThat(input.dueDate()).isEqualTo("2026-06-30");
    }

    @Test
    @DisplayName("keeps optional create milestone fields nullable")
    void keepsOptionalCreateMilestoneFieldsNullable() {
        final CreateMilestoneInput input = new CreateMilestoneInput("Release v1.0", null, null, null);

        assertThat(input.title()).isEqualTo("Release v1.0");
        assertThat(input.description()).isNull();
        assertThat(input.startDate()).isNull();
        assertThat(input.dueDate()).isNull();
        assertThat(CreateMilestoneInput.class.isRecord()).isTrue();
    }
}
