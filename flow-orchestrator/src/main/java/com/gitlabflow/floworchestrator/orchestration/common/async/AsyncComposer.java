package com.gitlabflow.floworchestrator.orchestration.common.async;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Shared fail-fast async composition helper for orchestration use cases.
 *
 * <p>Virtual threads are used through a shared executor so fan-out calls can scale for I/O-bound workloads
 * without tuning queue sizes or pool limits for each use case. A bounded pool was rejected because demand is
 * variable across capabilities, and {@code StructuredTaskScope} was rejected because preview APIs are not
 * enabled in this build.
 */
@Component
@RequiredArgsConstructor
public class AsyncComposer {

    private final ExecutorService asyncComposerExecutor;

    public <T> CompletableFuture<T> submit(final Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, asyncComposerExecutor);
    }

    public void joinFailFast(final List<? extends CompletableFuture<?>> futures) {
        if (futures.isEmpty()) {
            return;
        }

        final CompletableFuture<Throwable> firstFailure = new CompletableFuture<>();
        for (final CompletableFuture<?> future : futures) {
            future.whenComplete((ignored, throwable) -> {
                if (throwable != null) {
                    firstFailure.complete(throwable);
                }
            });
        }

        final CompletableFuture<Void> allCompleted = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));

        try {
            CompletableFuture.anyOf(allCompleted, firstFailure).join();
        } catch (final CompletionException exception) {
            cancelRemaining(futures);
            rethrowFailure(exception);
        }

        if (firstFailure.isDone()) {
            cancelRemaining(futures);
            rethrowFailure(firstFailure.join());
        }
    }

    private void cancelRemaining(final List<? extends CompletableFuture<?>> futures) {
        for (final CompletableFuture<?> future : futures) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
    }

    private void rethrowFailure(final Throwable throwable) {
        final Throwable rootCause = unwrapCompletionCause(throwable);
        if (rootCause == null) {
            throw new IllegalStateException("Async composition failed: missing root cause");
        }
        if (rootCause instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (rootCause instanceof Error error) {
            throw error;
        }
        throw new IllegalStateException("Async composition failed", rootCause);
    }

    private Throwable unwrapCompletionCause(final Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException completionException) {
            if (completionException.getCause() == null) {
                return null;
            }
            current = completionException.getCause();
        }
        return current;
    }
}
