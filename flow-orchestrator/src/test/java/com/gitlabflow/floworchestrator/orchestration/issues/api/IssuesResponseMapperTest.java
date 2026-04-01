package com.gitlabflow.floworchestrator.orchestration.issues.api;

import com.gitlabflow.floworchestrator.orchestration.issues.Issue;
import com.gitlabflow.floworchestrator.orchestration.issues.IssuePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IssuesResponseMapperTest {

    private final IssuesResponseMapper mapper = new IssuesResponseMapper();

    @Test
    @DisplayName("maps issue page to API response contract")
    void mapsIssuePageToApiResponse() {
        final IssuePage issuePage = new IssuePage(
                List.of(new Issue(123L, "Title", "Description", "opened", List.of("bug"), "john", "M1", 42L)),
                1,
                2
        );

        final IssuesResponse response = mapper.toResponse(issuePage);

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
        final IssuePage issuePage = new IssuePage(
                List.of(new Issue(1L, "T", null, "closed", List.of(), null, null, null)),
                1,
                1
        );

        final IssuesResponse response = mapper.toResponse(issuePage);

        assertThat(response.items().getFirst().assignee()).isNull();
        assertThat(response.items().getFirst().milestone()).isNull();
        assertThat(response.items().getFirst().parent()).isNull();
    }

    @Test
    @DisplayName("maps empty page to empty response items")
    void mapsEmptyPageToEmptyResponseItems() {
        final IssuePage issuePage = new IssuePage(List.of(), 0, 3);

        final IssuesResponse response = mapper.toResponse(issuePage);

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
                        new Issue(12L, "B", "Desc B", "closed", List.of("infra"), "bob", "M2", 2L)
                ),
                2,
                4
        );

        final IssuesResponse response = mapper.toResponse(issuePage);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().getFirst().id()).isEqualTo(11L);
        assertThat(response.items().get(1).id()).isEqualTo(12L);
        assertThat(response.items().get(1).assignee()).isEqualTo("bob");
        assertThat(response.count()).isEqualTo(2);
        assertThat(response.page()).isEqualTo(4);
    }
}
