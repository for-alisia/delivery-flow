package com.gitlabflow.floworchestrator.orchestration.issues.model;

import lombok.Builder;

@Builder
public record LabelChange(long id, String name) implements Change {

    private static final ChangeField FIELD = ChangeField.LABEL;

    @Override
    public ChangeField field() {
        return FIELD;
    }
}
