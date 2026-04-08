package com.gitlabflow.floworchestrator.orchestration.issues.model;

import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record LabelChangeSet(String changeType, ChangedBy changedBy, LabelChange change, OffsetDateTime changedAt)
        implements ChangeSet {}
