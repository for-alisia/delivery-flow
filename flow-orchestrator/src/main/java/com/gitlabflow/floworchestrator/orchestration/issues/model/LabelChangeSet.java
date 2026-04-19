package com.gitlabflow.floworchestrator.orchestration.issues.model;

import com.gitlabflow.floworchestrator.orchestration.common.model.ChangeSet;
import com.gitlabflow.floworchestrator.orchestration.common.model.User;
import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record LabelChangeSet(String changeType, User changedBy, LabelChange change, OffsetDateTime changedAt)
        implements ChangeSet<LabelChange> {}
