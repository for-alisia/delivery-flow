package com.gitlabflow.floworchestrator.orchestration.common.rest.dto;

import java.time.OffsetDateTime;

public interface ChangeSetDto {

    String changeType();

    UserDto changedBy();

    ChangeDto change();

    OffsetDateTime changedAt();
}
