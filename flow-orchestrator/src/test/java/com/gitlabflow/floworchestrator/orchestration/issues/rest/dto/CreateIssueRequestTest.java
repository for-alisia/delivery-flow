package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
        final List<String> sanitizedLabels = Objects.requireNonNull(request.labels());
        labels.clear();

        assertThat(sanitizedLabels).containsExactly("bug", "deploy");
        assertThatThrownBy(() -> sanitizedLabels.add("infra")).isInstanceOf(UnsupportedOperationException.class);
    }
}
