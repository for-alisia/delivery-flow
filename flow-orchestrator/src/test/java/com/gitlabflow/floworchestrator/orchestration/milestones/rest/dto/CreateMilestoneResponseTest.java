package com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateMilestoneResponseTest {

    @Test
    @DisplayName("builder carries milestoneId and title")
    void builderCarriesMilestoneIdAndTitle() {
        final CreateMilestoneResponse response = CreateMilestoneResponse.builder()
                .milestoneId(42L)
                .title("Q2 2026 Delivery")
                .build();

        assertThat(response.milestoneId()).isEqualTo(42L);
        assertThat(response.title()).isEqualTo("Q2 2026 Delivery");
        assertThat(CreateMilestoneResponse.class.isRecord()).isTrue();
    }
}
