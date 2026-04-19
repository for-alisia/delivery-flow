package com.gitlabflow.floworchestrator.orchestration.common.model;

import java.time.OffsetDateTime;

public interface ChangeSet<T extends Change> {

    String changeType();

    User changedBy();

    T change();

    OffsetDateTime changedAt();
}
