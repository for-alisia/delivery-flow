package com.gitlabflow.floworchestrator.orchestration.issues.model;

public interface Change {

    ChangeField field();

    long id();

    String name();
}
