package com.gitlabflow.floworchestrator.integration.gitlab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GitLabOffsetPaginationLoaderTest {

    private static final String RESOURCE_MILESTONES = "milestones";

    @Mock
    private GitLabOperationExecutor gitLabOperationExecutor;

    private GitLabOffsetPaginationLoader loader;

    @BeforeEach
    void setUp() {
        loader = new GitLabOffsetPaginationLoader(gitLabOperationExecutor);
        when(gitLabOperationExecutor.execute(eq(RESOURCE_MILESTONES), anySupplier()))
                .thenAnswer(invocation -> supplier(invocation.getArgument(1)).get());
    }

    @Test
    @DisplayName("returns first page only when page size is below per-page threshold")
    void returnsFirstPageOnlyWhenPageSizeIsBelowPerPageThreshold() {
        final List<String> result = loader.loadAllPages(RESOURCE_MILESTONES, (page, perPage) -> {
            assertThat(page).isEqualTo(1);
            assertThat(perPage).isEqualTo(100);
            return List.of("one", "two");
        });

        assertThat(result).containsExactly("one", "two");
        verify(gitLabOperationExecutor, times(1)).execute(eq(RESOURCE_MILESTONES), anySupplier());
    }

    @Test
    @DisplayName("continues loading pages while page size equals per-page threshold")
    void continuesLoadingPagesWhilePageSizeEqualsPerPageThreshold() {
        final List<Integer> requestedPages = new ArrayList<>();
        final List<Integer> firstPageItems =
                IntStream.rangeClosed(1, 100).boxed().toList();

        final List<Integer> result = loader.loadAllPages(RESOURCE_MILESTONES, (page, perPage) -> {
            requestedPages.add(page);
            assertThat(perPage).isEqualTo(100);
            return page == 1 ? firstPageItems : List.of(101);
        });

        assertThat(result).hasSize(101);
        assertThat(requestedPages).containsExactly(1, 2);
        verify(gitLabOperationExecutor, times(2)).execute(eq(RESOURCE_MILESTONES), anySupplier());
    }

    @Test
    @DisplayName("normalizes null page result to empty immutable list")
    void normalizesNullPageResultToEmptyImmutableList() {
        final List<String> result = loader.loadAllPages(RESOURCE_MILESTONES, (page, perPage) -> null);

        assertThat(result).isEmpty();
        verify(gitLabOperationExecutor, times(1)).execute(eq(RESOURCE_MILESTONES), anySupplier());
    }

    @SuppressWarnings("unchecked")
    private <T> Supplier<T> supplier(final Object value) {
        return (Supplier<T>) value;
    }

    @SuppressWarnings("unchecked")
    private Supplier<Object> anySupplier() {
        return (Supplier<Object>) any(Supplier.class);
    }
}
