package com.gitlabflow.floworchestrator.integration.gitlab.milestones.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitlabflow.floworchestrator.integration.gitlab.milestones.dto.GitLabCreateMilestoneRequest;
import com.gitlabflow.floworchestrator.integration.gitlab.milestones.dto.GitLabMilestoneResponse;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.CreateMilestoneInput;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.Milestone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GitLabMilestonesMapperTest {

    private final GitLabMilestonesMapper mapper = new GitLabMilestonesMapper();

    @Test
    @DisplayName("maps create milestone title-only input to GitLab request")
    void mapsCreateMilestoneTitleOnlyInputToGitLabRequest() {
        final CreateMilestoneInput input = new CreateMilestoneInput("Release v1.0", null, null, null);

        final GitLabCreateMilestoneRequest request = mapper.toCreateRequest(input);

        assertThat(request.title()).isEqualTo("Release v1.0");
        assertThat(request.description()).isNull();
        assertThat(request.startDate()).isNull();
        assertThat(request.dueDate()).isNull();
    }

    @Test
    @DisplayName("maps create milestone full input to GitLab request")
    void mapsCreateMilestoneFullInputToGitLabRequest() {
        final CreateMilestoneInput input =
                new CreateMilestoneInput("Q2 2026 Delivery", "Scope", "2026-04-01", "2026-06-30");

        final GitLabCreateMilestoneRequest request = mapper.toCreateRequest(input);

        assertThat(request.title()).isEqualTo("Q2 2026 Delivery");
        assertThat(request.description()).isEqualTo("Scope");
        assertThat(request.startDate()).isEqualTo("2026-04-01");
        assertThat(request.dueDate()).isEqualTo("2026-06-30");
    }

    @Test
    @DisplayName("maps GitLab milestone DTO fields to shared milestone model")
    void mapsGitLabMilestoneDtoFieldsToSharedMilestoneModel() {
        final GitLabMilestoneResponse response = GitLabMilestoneResponse.builder()
                .id(300L)
                .iid(12L)
                .title("Release 1.0")
                .description("Scope")
                .startDate("2026-05-01")
                .dueDate("2026-05-15")
                .state("active")
                .build();

        final Milestone milestone = mapper.toMilestone(response);

        assertThat(milestone.id()).isEqualTo(300L);
        assertThat(milestone.milestoneId()).isEqualTo(12L);
        assertThat(milestone.title()).isEqualTo("Release 1.0");
        assertThat(milestone.description()).isEqualTo("Scope");
        assertThat(milestone.startDate()).isEqualTo("2026-05-01");
        assertThat(milestone.dueDate()).isEqualTo("2026-05-15");
        assertThat(milestone.state()).isEqualTo("active");
    }

    @Test
    @DisplayName("preserves nullable description and dates from GitLab milestone DTO")
    void preservesNullableDescriptionAndDatesFromGitLabMilestoneDto() {
        final GitLabMilestoneResponse response = GitLabMilestoneResponse.builder()
                .id(301L)
                .iid(13L)
                .title("Release 1.1")
                .description(null)
                .startDate(null)
                .dueDate(null)
                .state("closed")
                .build();

        final Milestone milestone = mapper.toMilestone(response);

        assertThat(milestone.description()).isNull();
        assertThat(milestone.startDate()).isNull();
        assertThat(milestone.dueDate()).isNull();
        assertThat(milestone.state()).isEqualTo("closed");
    }
}
