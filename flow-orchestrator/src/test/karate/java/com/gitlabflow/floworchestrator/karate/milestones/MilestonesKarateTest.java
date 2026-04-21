package com.gitlabflow.floworchestrator.karate.milestones;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.karatelabs.core.Runner;
import io.karatelabs.core.SuiteResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MilestonesKarateTest {

    @Test
    @DisplayName("runs smoke-tagged Karate API scenarios for milestones")
    void runsSmokeTaggedKarateApiScenariosForMilestones() {
        final SuiteResult result =
                Runner.path("classpath:milestones").tags("@smoke").parallel(2);

        assertTrue(result.isPassed(), String.join("\n", result.getErrors()));
    }
}
