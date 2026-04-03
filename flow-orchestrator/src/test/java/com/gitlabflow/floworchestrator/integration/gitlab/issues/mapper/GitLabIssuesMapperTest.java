package com.gitlabflow.floworchestrator.integration.gitlab.issues.mapper;

import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabCreateIssueRequest;
import com.gitlabflow.floworchestrator.integration.gitlab.issues.dto.GitLabIssueResponse;
import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.Issue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitLabIssuesMapperTest {

    private final GitLabIssuesMapper mapper = new GitLabIssuesMapper();

    @Test
    @DisplayName("joins labels as comma separated string")
    void joinsLabelsAsCommaSeparatedString() {
        final CreateIssueInput input = new CreateIssueInput(
                "Deploy failure",
                "Step 3 failed",
                List.of("bug", "deploy")
        );

        final GitLabCreateIssueRequest request = mapper.toCreateRequest(input);

        assertThat(request.title()).isEqualTo("Deploy failure");
        assertThat(request.description()).isEqualTo("Step 3 failed");
        assertThat(request.labels()).isEqualTo("bug,deploy");
    }

    @Test
    @DisplayName("maps empty labels to null labels field")
    void mapsEmptyLabelsToNullLabelsField() {
        final CreateIssueInput input = new CreateIssueInput("Reporting bug", "Description", List.of());

        final GitLabCreateIssueRequest request = mapper.toCreateRequest(input);

        assertThat(request.description()).isEqualTo("Description");
        assertThat(request.labels()).isNull();
    }

    @Test
    @DisplayName("maps labels milestone epic and assignee list")
    void mapsLabelsMilestoneEpicAndAssigneeList() {
        final GitLabIssueResponse response = new GitLabIssueResponse(
                100L,
                "Title",
                "Desc",
                "opened",
                List.of("bug"),
                List.of(new GitLabIssueResponse.GitLabAssignee("john")),
                null,
                new GitLabIssueResponse.GitLabMilestone("M1"),
                new GitLabIssueResponse.GitLabEpic(42L)
        );

        final Issue issue = mapper.toIssue(response);

        assertThat(issue.id()).isEqualTo(100L);
        assertThat(issue.assignee()).isEqualTo("john");
        assertThat(issue.milestone()).isEqualTo("M1");
        assertThat(issue.parent()).isEqualTo(42L);
    }

    @Test
    @DisplayName("falls back to deprecated single assignee and nullables")
    void fallsBackToSingleAssigneeAndNullables() {
        final GitLabIssueResponse response = new GitLabIssueResponse(
                101L,
                "Title",
                null,
                "closed",
                null,
                List.of(),
                new GitLabIssueResponse.GitLabAssignee("legacy"),
                null,
                null
        );

        final Issue issue = mapper.toIssue(response);

        assertThat(issue.labels()).isEmpty();
        assertThat(issue.assignee()).isEqualTo("legacy");
        assertThat(issue.milestone()).isNull();
        assertThat(issue.parent()).isNull();
    }

    @Test
    @DisplayName("returns null assignee when both assignee sources are missing")
    void returnsNullAssigneeWhenBothSourcesAreMissing() {
        final GitLabIssueResponse response = new GitLabIssueResponse(
                102L,
                "No Assignee",
                "Desc",
                "opened",
                List.of("backend"),
                null,
                null,
                null,
                null
        );

        final Issue issue = mapper.toIssue(response);

        assertThat(issue.assignee()).isNull();
    }

    @Test
    @DisplayName("falls back to single assignee when assignee list has null usernames")
    void fallsBackToSingleAssigneeWhenAssigneeListHasNullUsernames() {
        final GitLabIssueResponse response = new GitLabIssueResponse(
                103L,
                "Fallback Assignee",
                "Desc",
                "opened",
                List.of(),
                List.of(new GitLabIssueResponse.GitLabAssignee(null)),
                new GitLabIssueResponse.GitLabAssignee("fallback-user"),
                null,
                null
        );

        final Issue issue = mapper.toIssue(response);

        assertThat(issue.assignee()).isEqualTo("fallback-user");
    }
}
