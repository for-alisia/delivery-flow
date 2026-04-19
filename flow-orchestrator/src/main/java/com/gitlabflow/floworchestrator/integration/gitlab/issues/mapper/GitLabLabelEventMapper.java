package com.gitlabflow.floworchestrator.integration.gitlab.issues.mapper;

import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabLabelEventResponse;
import com.gitlabflow.floworchestrator.orchestration.common.model.ChangeSet;
import com.gitlabflow.floworchestrator.orchestration.common.model.User;
import com.gitlabflow.floworchestrator.orchestration.issues.model.LabelChange;
import com.gitlabflow.floworchestrator.orchestration.issues.model.LabelChangeSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GitLabLabelEventMapper {

    public List<ChangeSet<?>> toLabelChangeSets(final List<GitLabLabelEventResponse> responses) {
        return responses.stream().<ChangeSet<?>>map(this::toLabelChangeSet).toList();
    }

    private LabelChangeSet toLabelChangeSet(final GitLabLabelEventResponse response) {
        final var user = requiredField(response.user(), "user");
        final var label = requiredField(response.label(), "label");
        final String action = requiredField(response.action(), "action");

        log.debug(
                "Mapped GitLab label event id={} action={} userId={} labelId={}",
                response.id(),
                action,
                user.id(),
                label.id());

        return LabelChangeSet.builder()
                .changeType(action)
                .changedBy(User.builder()
                        .id(user.id())
                        .username(user.username())
                        .name(user.name())
                        .build())
                .change(LabelChange.builder().id(label.id()).name(label.name()).build())
                .changedAt(requiredField(response.createdAt(), "created_at"))
                .build();
    }

    private <T> T requiredField(final T value, final String fieldName) {
        if (value == null) {
            throw new IllegalStateException("Missing GitLab label event field: " + fieldName);
        }
        return value;
    }
}
