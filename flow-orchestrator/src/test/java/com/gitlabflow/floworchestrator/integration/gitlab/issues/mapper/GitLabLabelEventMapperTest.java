package com.gitlabflow.floworchestrator.integration.gitlab.issues.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabLabelEventResponse;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabLabelEventResponse.GitLabLabelDetail;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabLabelEventResponse.GitLabUserDetail;
import com.gitlabflow.floworchestrator.orchestration.common.model.ChangeField;
import com.gitlabflow.floworchestrator.orchestration.common.model.ChangeSet;
import com.gitlabflow.floworchestrator.orchestration.issues.model.LabelChangeSet;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GitLabLabelEventMapperTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-01-15T09:30:00Z");
    private static final GitLabUserDetail USER = new GitLabUserDetail(1L, "root", "Administrator");
    private static final GitLabLabelDetail LABEL = new GitLabLabelDetail(73L, "bug");

    private final GitLabLabelEventMapper mapper = new GitLabLabelEventMapper();

    @Test
    @DisplayName("maps action user label and createdAt from GitLab label event")
    void mapsActionUserLabelAndCreatedAtFromGitLabLabelEvent() {
        final var response = new GitLabLabelEventResponse(1001L, USER, CREATED_AT, LABEL, "add");

        final List<ChangeSet<?>> result = mapper.toLabelChangeSets(List.of(response));

        assertThat(result).singleElement().isInstanceOfSatisfying(LabelChangeSet.class, changeSet -> {
            assertThat(changeSet.changeType()).isEqualTo("add");
            assertThat(changeSet.changedBy().id()).isEqualTo(1L);
            assertThat(changeSet.changedBy().username()).isEqualTo("root");
            assertThat(changeSet.changedBy().name()).isEqualTo("Administrator");
            assertThat(changeSet.change().id()).isEqualTo(73L);
            assertThat(changeSet.change().name()).isEqualTo("bug");
            assertThat(changeSet.change().field()).isEqualTo(ChangeField.LABEL);
            assertThat(changeSet.changedAt()).isEqualTo(CREATED_AT);
        });
    }

    @Test
    @DisplayName("preserves ordering when mapping multiple label events")
    void preservesOrderingWhenMappingMultipleLabelEvents() {
        final var secondEvent = new GitLabLabelEventResponse(
                1002L,
                new GitLabUserDetail(2L, "jdoe", "Jane Doe"),
                CREATED_AT.plusDays(1),
                new GitLabLabelDetail(99L, "feature"),
                "remove");

        final List<ChangeSet<?>> result = mapper.toLabelChangeSets(
                List.of(new GitLabLabelEventResponse(1001L, USER, CREATED_AT, LABEL, "add"), secondEvent));

        assertThat(result).hasSize(2);
        assertThat(result.getFirst()).isInstanceOfSatisfying(LabelChangeSet.class, changeSet -> {
            assertThat(changeSet.changeType()).isEqualTo("add");
            assertThat(changeSet.change().id()).isEqualTo(73L);
        });
        assertThat(result.get(1)).isInstanceOfSatisfying(LabelChangeSet.class, changeSet -> {
            assertThat(changeSet.changeType()).isEqualTo("remove");
            assertThat(changeSet.change().id()).isEqualTo(99L);
        });
    }

    @Test
    @DisplayName("returns empty list when input label events list is empty")
    void returnsEmptyListWhenInputLabelEventsListIsEmpty() {
        final List<ChangeSet<?>> result = mapper.toLabelChangeSets(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("throws IllegalStateException when label is missing")
    void throwsIllegalStateExceptionWhenLabelIsMissing() {
        final var response = new GitLabLabelEventResponse(1001L, USER, CREATED_AT, null, "add");
        final List<GitLabLabelEventResponse> responses = List.of(response);

        assertThatThrownBy(() -> mapper.toLabelChangeSets(responses))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("label");
    }

    @Test
    @DisplayName("throws IllegalStateException when user is missing")
    void throwsIllegalStateExceptionWhenUserIsMissing() {
        final var response = new GitLabLabelEventResponse(1001L, null, CREATED_AT, LABEL, "add");
        final List<GitLabLabelEventResponse> responses = List.of(response);

        assertThatThrownBy(() -> mapper.toLabelChangeSets(responses))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("user");
    }

    @Test
    @DisplayName("throws IllegalStateException when action is missing")
    void throwsIllegalStateExceptionWhenActionIsMissing() {
        final var response = new GitLabLabelEventResponse(1001L, USER, CREATED_AT, LABEL, null);
        final List<GitLabLabelEventResponse> responses = List.of(response);

        assertThatThrownBy(() -> mapper.toLabelChangeSets(responses))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("action");
    }

    @Test
    @DisplayName("throws IllegalStateException when createdAt is missing")
    void throwsIllegalStateExceptionWhenCreatedAtIsMissing() {
        final var response = new GitLabLabelEventResponse(1001L, USER, null, LABEL, "add");
        final List<GitLabLabelEventResponse> responses = List.of(response);

        assertThatThrownBy(() -> mapper.toLabelChangeSets(responses))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("created_at");
    }
}
