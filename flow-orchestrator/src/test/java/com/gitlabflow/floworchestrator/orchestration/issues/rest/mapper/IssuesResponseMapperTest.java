package com.gitlabflow.floworchestrator.orchestration.issues.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitlabflow.floworchestrator.orchestration.issues.model.EnrichedIssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.Issue;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueDetail.AssigneeDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueDetail.MilestoneDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDetailDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IssuesResponseMapperTest {

    private final IssuesResponseMapper mapper = new IssuesResponseMapper();

    @Test
    @DisplayName("maps issue page to API response contract")
    void mapsIssuePageToApiResponse() {
        final IssuePage issuePage = new IssuePage(
                List.of(new Issue(123L, 5L, "Title", "Description", "opened", List.of("bug"), "john", "M1", 42L)),
                1,
                2);

        final SearchIssuesResponse response = mapper.toSearchIssuesResponse(issuePage);

        assertThat(response.count()).isEqualTo(1);
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().id()).isEqualTo(123L);
        assertThat(response.items().getFirst().issueId()).isEqualTo(5L);
        assertThat(response.items().getFirst().assignee()).isEqualTo("john");
        assertThat(response.items().getFirst().milestone()).isEqualTo("M1");
        assertThat(response.items().getFirst().parent()).isEqualTo(42L);
    }

    @Test
    @DisplayName("keeps nullable fields as null")
    void keepsNullableFieldsAsNull() {
        final IssuePage issuePage =
                new IssuePage(List.of(new Issue(1L, 2L, "T", null, "closed", List.of(), null, null, null)), 1, 1);

        final SearchIssuesResponse response = mapper.toSearchIssuesResponse(issuePage);

        assertThat(response.items().getFirst().assignee()).isNull();
        assertThat(response.items().getFirst().milestone()).isNull();
        assertThat(response.items().getFirst().parent()).isNull();
    }

    @Test
    @DisplayName("maps empty page to empty response items")
    void mapsEmptyPageToEmptyResponseItems() {
        final IssuePage issuePage = new IssuePage(List.of(), 0, 3);

        final SearchIssuesResponse response = mapper.toSearchIssuesResponse(issuePage);

        assertThat(response.items()).isEmpty();
        assertThat(response.count()).isZero();
        assertThat(response.page()).isEqualTo(3);
    }

    @Test
    @DisplayName("maps multiple issues into multiple response items")
    void mapsMultipleIssuesIntoMultipleResponseItems() {
        final IssuePage issuePage = new IssuePage(
                List.of(
                        new Issue(11L, 3L, "A", "Desc A", "opened", List.of("bug"), "alice", "M1", 1L),
                        new Issue(12L, 4L, "B", "Desc B", "closed", List.of("infra"), "bob", "M2", 2L)),
                2,
                4);

        final SearchIssuesResponse response = mapper.toSearchIssuesResponse(issuePage);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().getFirst().id()).isEqualTo(11L);
        assertThat(response.items().getFirst().issueId()).isEqualTo(3L);
        assertThat(response.items().get(1).id()).isEqualTo(12L);
        assertThat(response.items().get(1).issueId()).isEqualTo(4L);
        assertThat(response.items().get(1).assignee()).isEqualTo("bob");
        assertThat(response.count()).isEqualTo(2);
        assertThat(response.page()).isEqualTo(4);
    }

    @Test
    @DisplayName("maps issue fields to issue dto")
    void mapsIssueFieldsToIssueDto() {
        final Issue issue = new Issue(
                84L, 6L, "Deploy failure", "Step 3 failed", "opened", List.of("bug", "deploy"), "john", "M1", 42L);

        final IssueDto response = mapper.toIssueDto(issue);

        assertThat(response.id()).isEqualTo(84L);
        assertThat(response.issueId()).isEqualTo(6L);
        assertThat(response.title()).isEqualTo("Deploy failure");
        assertThat(response.description()).isEqualTo("Step 3 failed");
        assertThat(response.state()).isEqualTo("opened");
        assertThat(response.labels()).containsExactly("bug", "deploy");
        assertThat(response.assignee()).isEqualTo("john");
        assertThat(response.milestone()).isEqualTo("M1");
        assertThat(response.parent()).isEqualTo(42L);
    }

    @Test
    @DisplayName("keeps nullable description as null for issue dto")
    void keepsNullableDescriptionAsNullForIssueDto() {
        final Issue issue = new Issue(85L, 7L, "Reporting bug", null, "opened", List.of(), null, null, null);

        final IssueDto response = mapper.toIssueDto(issue);

        assertThat(response.id()).isEqualTo(85L);
        assertThat(response.description()).isNull();
        assertThat(response.labels()).isEmpty();
    }

    @Test
    @DisplayName("maps issueId from issue to dto")
    void mapsIssueIdFromIssueToDto() {
        final Issue issue = new Issue(1L, 99L, "Title", null, "opened", List.of(), null, null, null);

        final IssueDto dto = mapper.toIssueDto(issue);

        assertThat(dto.issueId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("toIssueDetailDto maps all fields")
    void toIssueDetailDtoMapsAllFields() {
        final var assignee = new AssigneeDetail(10L, "john.doe", "John Doe");
        final var milestone = new MilestoneDetail(5L, 3L, "Sprint 12", "active", "2026-04-30");
        final var createdAt = OffsetDateTime.parse("2026-01-04T15:31:51.081Z");
        final var updatedAt = OffsetDateTime.parse("2026-03-12T09:00:00.000Z");
        final var detail = new IssueDetail(
                42L,
                "Fix login bug",
                "SSO broken",
                "opened",
                List.of("bug"),
                List.of(assignee),
                milestone,
                createdAt,
                updatedAt,
                null);
        final var enriched = new EnrichedIssueDetail(detail, List.of());

        final IssueDetailDto dto = mapper.toIssueDetailDto(enriched);

        assertThat(dto.issueId()).isEqualTo(42L);
        assertThat(dto.title()).isEqualTo("Fix login bug");
        assertThat(dto.description()).isEqualTo("SSO broken");
        assertThat(dto.state()).isEqualTo("opened");
        assertThat(dto.labels()).containsExactly("bug");
        assertThat(dto.assignees()).hasSize(1);
        assertThat(dto.assignees().getFirst().id()).isEqualTo(10L);
        assertThat(dto.assignees().getFirst().username()).isEqualTo("john.doe");
        assertThat(dto.assignees().getFirst().name()).isEqualTo("John Doe");
        final var milestoneDto = Objects.requireNonNull(dto.milestone());
        assertThat(milestoneDto.milestoneId()).isEqualTo(3L);
        assertThat(dto.createdAt()).isEqualTo(createdAt);
        assertThat(dto.updatedAt()).isEqualTo(updatedAt);
        assertThat(dto.closedAt()).isNull();
        assertThat(dto.changeSets()).isEmpty();
    }

    @Test
    @DisplayName("toIssueDetailDto maps null milestone to null")
    void toIssueDetailDtoMapsNullMilestoneToNull() {
        final var createdAt = OffsetDateTime.parse("2026-01-04T15:31:51.081Z");
        final var updatedAt = OffsetDateTime.parse("2026-03-12T09:00:00.000Z");
        final var detail = new IssueDetail(
                7L, "No milestone", null, "closed", List.of(), List.of(), null, createdAt, updatedAt, null);
        final var enriched = new EnrichedIssueDetail(detail, List.of());

        final IssueDetailDto dto = mapper.toIssueDetailDto(enriched);

        assertThat(dto.milestone()).isNull();
    }

    @Test
    @DisplayName("toIssueDetailDto maps empty assignees list to empty")
    void toIssueDetailDtoMapsEmptyAssigneesToEmpty() {
        final var createdAt = OffsetDateTime.parse("2026-01-04T15:31:51.081Z");
        final var updatedAt = OffsetDateTime.parse("2026-03-12T09:00:00.000Z");
        final var detail = new IssueDetail(
                7L, "No assignees", null, "opened", List.of(), List.of(), null, createdAt, updatedAt, null);
        final var enriched = new EnrichedIssueDetail(detail, List.of());

        final IssueDetailDto dto = mapper.toIssueDetailDto(enriched);

        assertThat(dto.assignees()).isEmpty();
    }

    @Test
    @DisplayName("toIssueDetailDto changeSets is always empty list")
    void toIssueDetailDtoChangeSetsIsAlwaysEmpty() {
        final var createdAt = OffsetDateTime.parse("2026-01-04T15:31:51.081Z");
        final var updatedAt = OffsetDateTime.parse("2026-03-12T09:00:00.000Z");
        final var detail =
                new IssueDetail(7L, "T", null, "opened", List.of(), List.of(), null, createdAt, updatedAt, null);
        final var enriched = new EnrichedIssueDetail(detail, List.of());

        final IssueDetailDto dto = mapper.toIssueDetailDto(enriched);

        assertThat(dto.changeSets()).isEmpty();
    }
}
