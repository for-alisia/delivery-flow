package com.gitlabflow.floworchestrator.karate.issues;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.karatelabs.core.Runner;
import io.karatelabs.core.SuiteResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IssuesKarateTest {

    @Test
    @DisplayName("runs smoke-tagged Karate API scenarios")
    void runsSmokeTaggedKarateApiScenarios() {
        final SuiteResult result =
                Runner.path("classpath:issues").tags("@smoke").parallel(2);

        assertTrue(result.isPassed(), String.join("\n", result.getErrors()));
    }
}
