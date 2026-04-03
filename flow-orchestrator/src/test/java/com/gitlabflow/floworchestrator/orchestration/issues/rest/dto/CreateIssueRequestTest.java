package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateIssueRequestTest {

    @Test
    @DisplayName("keeps null labels as null")
    void keepsNullLabelsAsNull() {
        final CreateIssueRequest request = new CreateIssueRequest("Deploy failure", "Step 3 failed", null);

        assertThat(request.labels()).isNull();
    }

    @Test
    @DisplayName("filters null elements from labels list")
    void filtersNullElementsFromLabelsList() {
        final List<String> labels = new ArrayList<>(List.of("bug", "deploy"));
        labels.add(null);

        final CreateIssueRequest request = new CreateIssueRequest("Deploy failure", "Step 3 failed", labels);
        labels.clear();

        assertThat(request.labels()).containsExactly("bug", "deploy");
        assertThatThrownBy(() -> request.labels().add("infra")).isInstanceOf(UnsupportedOperationException.class);
    }
}
