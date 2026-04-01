package com.gitlabflow.floworchestrator.orchestration.issues.api;

import com.gitlabflow.floworchestrator.config.IssuesApiProperties;
import com.gitlabflow.floworchestrator.orchestration.issues.IssueQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.IssueState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IssuesRequestMapperTest {

    private final IssuesRequestMapper mapper = new IssuesRequestMapper(new IssuesApiProperties(40, 100));

    @Test
    @DisplayName("maps null body to default page and perPage")
    void mapsNullBodyToDefaults() {
        final IssueQuery query = mapper.toIssueQuery(null);

        assertThat(query.page()).isEqualTo(1);
        assertThat(query.perPage()).isEqualTo(40);
        assertThat(query.state()).isNull();
        assertThat(query.label()).isNull();
        assertThat(query.assignee()).isNull();
        assertThat(query.milestone()).isNull();
    }

    @Test
    @DisplayName("maps nested body values into domain query")
    void mapsNestedValuesIntoQuery() {
        final IssuesRequest request = new IssuesRequest(
                new PaginationRequest(2, 20),
                new IssueFiltersRequest("opened", List.of("bug"), List.of("john.doe"), List.of("M1"))
        );

        final IssueQuery query = mapper.toIssueQuery(request);

        assertThat(query.page()).isEqualTo(2);
        assertThat(query.perPage()).isEqualTo(20);
        assertThat(query.state()).isEqualTo(IssueState.OPENED);
        assertThat(query.label()).isEqualTo("bug");
        assertThat(query.assignee()).isEqualTo("john.doe");
        assertThat(query.milestone()).isEqualTo("M1");
    }

    @Test
    @DisplayName("maps missing nested objects to defaults")
    void mapsMissingNestedObjectsToDefaults() {
        final IssueQuery query = mapper.toIssueQuery(new IssuesRequest(null, null));

        assertThat(query.page()).isEqualTo(1);
        assertThat(query.perPage()).isEqualTo(40);
    }

    @Test
    @DisplayName("extracts only first non-null filter value")
    void extractsSingleFilterValue() {
        final IssuesRequest request = new IssuesRequest(
                null,
                new IssueFiltersRequest("all", Arrays.asList(null, "bug"), List.of("alice"), List.of("v1"))
        );

        final IssueQuery query = mapper.toIssueQuery(request);

        assertThat(query.state()).isEqualTo(IssueState.ALL);
        assertThat(query.label()).isEqualTo("bug");
        assertThat(query.assignee()).isEqualTo("alice");
        assertThat(query.milestone()).isEqualTo("v1");
    }
}
