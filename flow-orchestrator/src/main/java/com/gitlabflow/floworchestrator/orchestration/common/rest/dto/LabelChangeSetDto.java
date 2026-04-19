package com.gitlabflow.floworchestrator.orchestration.common.rest.dto;

import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record LabelChangeSetDto(String changeType, UserDto changedBy, LabelChangeDto change, OffsetDateTime changedAt)
        implements ChangeSetDto {}
