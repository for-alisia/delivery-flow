package com.gitlabflow.floworchestrator.orchestration.common.rest.dto;

import com.gitlabflow.floworchestrator.orchestration.common.model.ChangeField;

public interface ChangeDto {

    ChangeField field();

    long id();

    String name();
}
