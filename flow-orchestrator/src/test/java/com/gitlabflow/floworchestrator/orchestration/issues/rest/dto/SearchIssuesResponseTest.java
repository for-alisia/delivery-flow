package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchIssuesResponseTest {

    @Test
    @DisplayName("normalizes null items to empty immutable list")
    void normalizesNullItemsToEmptyImmutableList() {
        final SearchIssuesResponse response = new SearchIssuesResponse(null, 0, 1);

        assertThat(response.items()).isEmpty();
        assertThatThrownBy(() -> response.items().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("defensively copies and detaches from mutable input list")
    void defensivelyCopiesAndDetachesFromMutableInputList() {
        final List<IssueDto> items = new ArrayList<>();
        items.add(new IssueDto(1L, "A", null, "opened", List.of("bug"), null, null, null));

        final SearchIssuesResponse response = new SearchIssuesResponse(items, 1, 1);
        items.clear();

        assertThat(response.items()).hasSize(1);
        assertThatThrownBy(() -> response.items().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
