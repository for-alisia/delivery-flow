package com.gitlabflow.floworchestrator.integration.gitlab;

import com.gitlabflow.floworchestrator.common.util.ElapsedTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitLabOffsetPaginationLoader {

    private static final int PER_PAGE = 100;

    private final GitLabOperationExecutor gitLabOperationExecutor;

    public <T> List<T> loadAllPages(final String resource, final BiFunction<Integer, Integer, List<T>> pageLoader) {
        final long startedAt = System.nanoTime();
        final List<T> allItems = new ArrayList<>();
        int currentPage = 1;
        int pagesFetched = 0;

        while (true) {
            final int page = currentPage;
            final List<T> pageItems = gitLabOperationExecutor.execute(resource, () -> {
                final List<T> loadedItems = pageLoader.apply(page, PER_PAGE);
                return loadedItems == null ? List.of() : List.copyOf(loadedItems);
            });

            pagesFetched++;
            allItems.addAll(pageItems);

            if (pageItems.size() < PER_PAGE) {
                break;
            }
            currentPage++;
        }

        log.info(
                "GitLab offset pagination completed resource={} pageCount={} itemCount={} durationMs={}",
                resource,
                pagesFetched,
                allItems.size(),
                ElapsedTime.toDurationMs(startedAt));

        return allItems.isEmpty() ? List.of() : List.copyOf(allItems);
    }
}
