package com.gitlabflow.floworchestrator.integration.gitlab.issues.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabSingleIssueResponse;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabSingleIssueResponse.GitLabAssigneeDetail;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabSingleIssueResponse.GitLabMilestoneDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueDetail;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GitLabIssueDetailMapperTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-01-04T15:31:51.081Z");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-03-12T09:00:00.000Z");
    private static final OffsetDateTime CLOSED_AT = OffsetDateTime.parse("2026-03-01T00:00:00.000Z");

    private final GitLabIssueDetailMapper mapper = new GitLabIssueDetailMapper();

    @Test
    @DisplayName("maps all fields from full response")
    void mapsAllFieldsFromFullResponse() {
        final var assignee = new GitLabAssigneeDetail(10L, "john.doe", "John Doe");
        final var milestone = new GitLabMilestoneDetail(5L, 3L, "Sprint 12", "active", "2026-04-30");
        final var response = new GitLabSingleIssueResponse(
                500L,
                42L,
                "Fix login bug",
                "SSO broken",
                "opened",
                List.of("bug", "high-priority"),
                List.of(assignee),
                milestone,
                CREATED_AT,
                UPDATED_AT,
                null);

        final IssueDetail result = mapper.toIssueDetail(response);

        assertThat(result.issueId()).isEqualTo(42L);
        assertThat(result.title()).isEqualTo("Fix login bug");
        assertThat(result.description()).isEqualTo("SSO broken");
        assertThat(result.state()).isEqualTo("opened");
        assertThat(result.labels()).containsExactly("bug", "high-priority");
        assertThat(result.assignees()).hasSize(1);
        assertThat(result.assignees().getFirst().id()).isEqualTo(10L);
        assertThat(result.assignees().getFirst().username()).isEqualTo("john.doe");
        assertThat(result.assignees().getFirst().name()).isEqualTo("John Doe");
        final var milestoneResult = Objects.requireNonNull(result.milestone());
        assertThat(milestoneResult.id()).isEqualTo(5L);
        assertThat(milestoneResult.milestoneId()).isEqualTo(3L);
        assertThat(milestoneResult.title()).isEqualTo("Sprint 12");
        assertThat(milestoneResult.state()).isEqualTo("active");
        assertThat(milestoneResult.dueDate()).isEqualTo("2026-04-30");
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
        assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
        assertThat(result.closedAt()).isNull();
    }

    @Test
    @DisplayName("maps null description to null")
    void mapsNullDescriptionToNull() {
        final var response = new GitLabSingleIssueResponse(
                1L, 7L, "No desc", null, "closed", List.of(), List.of(), null, CREATED_AT, UPDATED_AT, CLOSED_AT);

        final IssueDetail result = mapper.toIssueDetail(response);

        assertThat(result.description()).isNull();
    }

    @Test
    @DisplayName("maps null closedAt to null")
    void mapsNullClosedAtToNull() {
        final var response = new GitLabSingleIssueResponse(
                1L, 7L, "T", null, "opened", List.of(), List.of(), null, CREATED_AT, UPDATED_AT, null);

        final IssueDetail result = mapper.toIssueDetail(response);

        assertThat(result.closedAt()).isNull();
    }

    @Test
    @DisplayName("maps null labels to empty list via compact constructor")
    void mapsNullLabelsToEmptyList() {
        final var response = new GitLabSingleIssueResponse(
                1L, 7L, "T", null, "opened", null, List.of(), null, CREATED_AT, UPDATED_AT, null);

        final IssueDetail result = mapper.toIssueDetail(response);

        assertThat(result.labels()).isEmpty();
    }

    @Test
    @DisplayName("maps null assignees to empty list via compact constructor")
    void mapsNullAssigneesToEmptyList() {
        final var response = new GitLabSingleIssueResponse(
                1L, 7L, "T", null, "opened", List.of(), null, null, CREATED_AT, UPDATED_AT, null);

        final IssueDetail result = mapper.toIssueDetail(response);

        assertThat(result.assignees()).isEmpty();
    }

    @Test
    @DisplayName("maps null milestone to null")
    void mapsNullMilestoneToNull() {
        final var response = new GitLabSingleIssueResponse(
                1L, 7L, "T", null, "opened", List.of(), List.of(), null, CREATED_AT, UPDATED_AT, null);

        final IssueDetail result = mapper.toIssueDetail(response);

        assertThat(result.milestone()).isNull();
    }

    @Test
    @DisplayName("maps milestone with null dueDate cleanly")
    void mapsMilestoneWithNullDueDateCleanly() {
        final var milestone = new GitLabMilestoneDetail(5L, 3L, "Sprint 12", "active", null);
        final var response = new GitLabSingleIssueResponse(
                1L, 7L, "T", null, "opened", List.of(), List.of(), milestone, CREATED_AT, UPDATED_AT, null);

        final IssueDetail result = mapper.toIssueDetail(response);

        final var milestoneResult = Objects.requireNonNull(result.milestone());
        assertThat(milestoneResult.dueDate()).isNull();
    }
}
