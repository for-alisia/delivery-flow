package com.gitlabflow.floworchestrator.common.util;

public final class ElapsedTime {

    private static final long NANOS_PER_MILLISECOND = 1_000_000L;

    private ElapsedTime() {}

    public static long toDurationMs(final long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLISECOND;
    }
}
