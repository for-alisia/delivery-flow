package com.gitlabflow.floworchestrator.orchestration.common.model;

public interface Change {

    ChangeField field();

    long id();

    String name();
}
