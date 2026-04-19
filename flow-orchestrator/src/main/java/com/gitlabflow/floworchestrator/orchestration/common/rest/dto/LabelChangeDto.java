package com.gitlabflow.floworchestrator.orchestration.common.rest.dto;

import com.gitlabflow.floworchestrator.orchestration.common.model.ChangeField;
import lombok.Builder;

@Builder
public record LabelChangeDto(ChangeField field, long id, String name) implements ChangeDto {}
