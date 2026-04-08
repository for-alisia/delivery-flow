package com.gitlabflow.floworchestrator.orchestration.issues.model;

import java.time.OffsetDateTime;

public interface ChangeSet {

    String changeType();

    ChangedBy changedBy();

    Change change();

    OffsetDateTime changedAt();
}
