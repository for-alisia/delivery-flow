package com.gitlabflow.floworchestrator.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IssuesApiPropertiesTest {

    @Test
    @DisplayName("accepts fixed contract values for default and max page size")
    void acceptsFixedContractValuesForDefaultAndMaxPageSize() {
        assertThatCode(() -> new IssuesApiProperties(20, 40)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects default page size drift from contract")
    void rejectsDefaultPageSizeDriftFromContract() {
        assertThatThrownBy(() -> new IssuesApiProperties(21, 40))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.issues-api.default-page-size must be 20");
    }

    @Test
    @DisplayName("rejects max page size drift from contract")
    void rejectsMaxPageSizeDriftFromContract() {
        assertThatThrownBy(() -> new IssuesApiProperties(20, 41))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("app.issues-api.max-page-size must be 40");
    }
}
