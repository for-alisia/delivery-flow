package com.gitlabflow.floworchestrator.orchestration.milestones.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MilestoneStateTest {

    @Test
    @DisplayName("fromValue maps active closed and all")
    void fromValueMapsActiveClosedAndAll() {
        assertThat(MilestoneState.fromValue("active")).isEqualTo(MilestoneState.ACTIVE);
        assertThat(MilestoneState.fromValue("closed")).isEqualTo(MilestoneState.CLOSED);
        assertThat(MilestoneState.fromValue("all")).isEqualTo(MilestoneState.ALL);
    }

    @Test
    @DisplayName("fromValue rejects unsupported state")
    void fromValueRejectsUnsupportedState() {
        assertThatThrownBy(() -> MilestoneState.fromValue("opened"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported milestone state: opened");
    }
}
