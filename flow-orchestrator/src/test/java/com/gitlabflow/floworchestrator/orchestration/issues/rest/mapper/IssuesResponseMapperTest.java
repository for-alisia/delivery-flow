package com.gitlabflow.floworchestrator.orchestration.issues.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitlabflow.floworchestrator.orchestration.issues.model.Issue;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IssuesResponseMapperTest {

    private final IssuesResponseMapper mapper = new IssuesResponseMapper();

    @Test
    @DisplayName("maps issue page to API response contract")
    void mapsIssuePageToApiResponse() {
        final IssuePage issuePage = new IssuePage(
                List.of(new Issue(123L, "Title", "Description", "opened", List.of("bug"), "john", "M1", 42L)), 1, 2);

        final SearchIssuesResponse response = mapper.toSearchIssuesResponse(issuePage);

        assertThat(response.count()).isEqualTo(1);
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().id()).isEqualTo(123L);
        assertThat(response.items().getFirst().assignee()).isEqualTo("john");
        assertThat(response.items().getFirst().milestone()).isEqualTo("M1");
        assertThat(response.items().getFirst().parent()).isEqualTo(42L);
    }

    @Test
    @DisplayName("keeps nullable fields as null")
    void keepsNullableFieldsAsNull() {
        final IssuePage issuePage =
                new IssuePage(List.of(new Issue(1L, "T", null, "closed", List.of(), null, null, null)), 1, 1);

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
                        new Issue(11L, "A", "Desc A", "opened", List.of("bug"), "alice", "M1", 1L),
                        new Issue(12L, "B", "Desc B", "closed", List.of("infra"), "bob", "M2", 2L)),
                2,
                4);

        final SearchIssuesResponse response = mapper.toSearchIssuesResponse(issuePage);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().getFirst().id()).isEqualTo(11L);
        assertThat(response.items().get(1).id()).isEqualTo(12L);
        assertThat(response.items().get(1).assignee()).isEqualTo("bob");
        assertThat(response.count()).isEqualTo(2);
        assertThat(response.page()).isEqualTo(4);
    }

    @Test
    @DisplayName("maps issue fields to issue dto")
    void mapsIssueFieldsToIssueDto() {
        final Issue issue = new Issue(
                84L, "Deploy failure", "Step 3 failed", "opened", List.of("bug", "deploy"), "john", "M1", 42L);

        final IssueDto response = mapper.toIssueDto(issue);

        assertThat(response.id()).isEqualTo(84L);
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
        final Issue issue = new Issue(85L, "Reporting bug", null, "opened", List.of(), null, null, null);

        final IssueDto response = mapper.toIssueDto(issue);

        assertThat(response.id()).isEqualTo(85L);
        assertThat(response.description()).isNull();
        assertThat(response.labels()).isEmpty();
    }
}
