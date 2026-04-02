package com.gitlabflow.floworchestrator.orchestration.issues.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssueFiltersRequestTest {

    @Test
    @DisplayName("normalizes null filter lists to empty immutable lists")
    void normalizesNullListsToEmptyImmutableLists() {
        final IssueFiltersRequest request = new IssueFiltersRequest("opened", null, null, null);

        assertThat(request.labels()).isEmpty();
        assertThat(request.assignee()).isEmpty();
        assertThat(request.milestone()).isEmpty();
        assertThatThrownBy(() -> request.labels().add("bug"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("filters out null values and detaches from mutable input lists")
    void filtersNullsAndDetachesFromMutableInputs() {
        final List<String> labels = new ArrayList<>(List.of("bug", "backend"));
        labels.add(null);

        final IssueFiltersRequest request = new IssueFiltersRequest(
                "all",
                labels,
                new ArrayList<>(List.of("alice")),
                new ArrayList<>(List.of("M1"))
        );

        labels.clear();

        assertThat(request.labels()).containsExactly("bug", "backend");
        assertThat(request.assignee()).containsExactly("alice");
        assertThat(request.milestone()).containsExactly("M1");
    }
}
