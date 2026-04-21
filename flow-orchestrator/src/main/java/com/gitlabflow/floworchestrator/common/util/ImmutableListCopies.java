package com.gitlabflow.floworchestrator.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.lang.Nullable;

public final class ImmutableListCopies {

    private ImmutableListCopies() {}

    // List.copyOf rejects null elements, but boundary validators rely on indexed null entries.
    public static <T> List<T> copyPreservingNullsOrEmpty(@Nullable final List<T> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    @Nullable
    public static <T> List<T> copyPreservingNullsOrNull(@Nullable final List<T> source) {
        if (source == null) {
            return null;
        }
        return source.isEmpty() ? List.of() : Collections.unmodifiableList(new ArrayList<>(source));
    }
}
