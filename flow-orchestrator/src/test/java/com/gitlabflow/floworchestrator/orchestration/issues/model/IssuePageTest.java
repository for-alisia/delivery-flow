package com.gitlabflow.floworchestrator.orchestration.issues.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IssuePageTest {

    @Test
    @DisplayName("normalizes null items to empty immutable list")
    void normalizesNullItemsToEmptyImmutableList() {
        final IssuePage page = new IssuePage(null, 0, 1);

        assertThat(page.items()).isEmpty();
        assertThatThrownBy(() -> page.items().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("defensively copies and detaches from mutable input list")
    void defensivelyCopiesAndDetachesFromMutableInputList() {
        final List<IssueSummary> items = new ArrayList<>();
        items.add(new IssueSummary(1L, 1L, "A", null, "opened", List.of("bug"), null, null, null, null));

        final IssuePage page = new IssuePage(items, 1, 1);
        items.clear();

        assertThat(page.items()).hasSize(1);
        assertThatThrownBy(() -> page.items().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }
}
