package com.gitlabflow.floworchestrator.orchestration.common.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import com.gitlabflow.floworchestrator.common.error.ErrorCode;
import com.gitlabflow.floworchestrator.common.error.IntegrationException;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AsyncComposerTest {

    private static final long CANCELLATION_WAIT_MILLIS = 1_000L;

    private ExecutorService asyncExecutor;
    private AsyncComposer asyncComposer;

    @BeforeEach
    void setUp() {
        asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
        asyncComposer = new AsyncComposer(asyncExecutor);
    }

    @AfterEach
    void tearDown() {
        asyncExecutor.close();
    }

    @Test
    @DisplayName("joinFailFast returns after all successful tasks complete and keeps call-site order")
    void joinFailFastReturnsAfterAllSuccessfulTasksCompleteAndKeepsCallSiteOrder() {
        final CompletableFuture<String> detailFuture = asyncComposer.submit(() -> {
            LockSupport.parkNanos(Duration.ofMillis(50).toNanos());
            return "detail";
        });
        final CompletableFuture<String> eventsFuture = asyncComposer.submit(() -> "events");

        asyncComposer.joinFailFast(List.of(detailFuture, eventsFuture));

        assertThat(List.of(detailFuture.join(), eventsFuture.join())).containsExactly("detail", "events");
    }

    @Test
    @DisplayName("joinFailFast cancels slow sibling and rethrows original runtime exception")
    void joinFailFastCancelsSlowSiblingAndRethrowsOriginalRuntimeException() {
        final CountDownLatch slowTaskStarted = new CountDownLatch(1);
        final AtomicReference<CompletableFuture<Void>> slowFutureReference = new AtomicReference<>();
        final IntegrationException failure =
                new IntegrationException(ErrorCode.INTEGRATION_FAILURE, "GitLab detail enrichment failed", "gitlab");

        final CompletableFuture<Void> slowFuture = asyncComposer.submit(() -> {
            slowTaskStarted.countDown();
            while (true) {
                final CompletableFuture<Void> currentFuture = slowFutureReference.get();
                if (Thread.currentThread().isInterrupted() || (currentFuture != null && currentFuture.isCancelled())) {
                    return null;
                }
                LockSupport.parkNanos(Duration.ofMillis(1).toNanos());
            }
        });
        slowFutureReference.set(slowFuture);

        final CompletableFuture<Void> failingFuture = asyncComposer.submit(() -> {
            awaitLatch(slowTaskStarted);
            throw failure;
        });

        final long startedAt = System.nanoTime();
        final IntegrationException thrown = assertThrowsExactly(
                IntegrationException.class, () -> asyncComposer.joinFailFast(List.of(failingFuture, slowFuture)));
        final long durationMillis = (System.nanoTime() - startedAt) / 1_000_000L;

        assertThat(thrown).isSameAs(failure);
        assertThat(durationMillis).isLessThan(CANCELLATION_WAIT_MILLIS);
        assertThat(slowFuture.isDone()).isTrue();
        assertThat(slowFuture.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("joinFailFast unwraps CompletionException and rethrows the original IntegrationException")
    void joinFailFastUnwrapsCompletionExceptionAndRethrowsOriginalIntegrationException() {
        final IntegrationException failure = new IntegrationException(
                ErrorCode.INTEGRATION_FAILURE, "GitLab label events operation failed", "gitlab");
        final CompletableFuture<Void> failedFuture = CompletableFuture.failedFuture(new CompletionException(failure));
        final CompletableFuture<Void> siblingFuture = asyncComposer.submit(() -> null);

        final IntegrationException thrown = assertThrowsExactly(
                IntegrationException.class, () -> asyncComposer.joinFailFast(List.of(failedFuture, siblingFuture)));

        assertThat(thrown).isSameAs(failure);
    }

    @Test
    @DisplayName("joinFailFast wraps checked causes in IllegalStateException")
    void joinFailFastWrapsCheckedCausesInIllegalStateException() {
        final IOException checkedCause = new IOException("timed out");
        final CompletableFuture<Void> failedFuture =
                CompletableFuture.failedFuture(new CompletionException(checkedCause));

        final IllegalStateException thrown = assertThrowsExactly(
                IllegalStateException.class, () -> asyncComposer.joinFailFast(List.of(failedFuture)));

        assertThat(thrown).hasCause(checkedCause);
    }

    @Test
    @DisplayName("joinFailFast rethrows Error causes")
    void joinFailFastRethrowsErrorCauses() {
        final AssertionError failure = new AssertionError("fatal");
        final CompletableFuture<Void> failedFuture = CompletableFuture.failedFuture(new CompletionException(failure));

        final AssertionError thrown =
                assertThrowsExactly(AssertionError.class, () -> asyncComposer.joinFailFast(List.of(failedFuture)));

        assertThat(thrown).isSameAs(failure);
    }

    @Test
    @DisplayName("joinFailFast returns immediately for empty input")
    void joinFailFastReturnsImmediatelyForEmptyInput() {
        assertDoesNotThrow(() -> asyncComposer.joinFailFast(List.of()));
    }

    private static void awaitLatch(final CountDownLatch latch) {
        try {
            final boolean released = latch.await(CANCELLATION_WAIT_MILLIS, TimeUnit.MILLISECONDS);
            if (!released) {
                throw new IllegalStateException("Timed out waiting for async task coordination");
            }
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while coordinating async tasks", exception);
        }
    }
}
