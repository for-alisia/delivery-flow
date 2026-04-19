package com.gitlabflow.floworchestrator.orchestration.issues.model;

import com.gitlabflow.floworchestrator.orchestration.common.model.Change;
import com.gitlabflow.floworchestrator.orchestration.common.model.ChangeField;
import lombok.Builder;

@Builder
public record LabelChange(long id, String name) implements Change {

    private static final ChangeField FIELD = ChangeField.LABEL;

    @Override
    public ChangeField field() {
        return FIELD;
    }
}
