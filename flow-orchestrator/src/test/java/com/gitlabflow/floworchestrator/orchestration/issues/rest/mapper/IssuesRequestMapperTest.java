package com.gitlabflow.floworchestrator.orchestration.issues.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitlabflow.floworchestrator.config.IssuesApiProperties;
import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueState;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.CreateIssueRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueFiltersRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.PaginationRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesRequest;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        final SearchIssuesRequest request = new SearchIssuesRequest(
                new PaginationRequest(2, 20),
                new IssueFiltersRequest("opened", List.of("bug"), List.of("john.doe"), List.of("M1")));

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
        final IssueQuery query = mapper.toIssueQuery(new SearchIssuesRequest(null, null));

        assertThat(query.page()).isEqualTo(1);
        assertThat(query.perPage()).isEqualTo(40);
    }

    @Test
    @DisplayName("extracts only first non-null filter value")
    void extractsSingleFilterValue() {
        final SearchIssuesRequest request = new SearchIssuesRequest(
                null, new IssueFiltersRequest("all", Arrays.asList(null, "bug"), List.of("alice"), List.of("v1")));

        final IssueQuery query = mapper.toIssueQuery(request);

        assertThat(query.state()).isEqualTo(IssueState.ALL);
        assertThat(query.label()).isEqualTo("bug");
        assertThat(query.assignee()).isEqualTo("alice");
        assertThat(query.milestone()).isEqualTo("v1");
    }

    @Test
    @DisplayName("maps create issue request with null labels to empty list")
    void mapsCreateIssueRequestWithNullLabelsToEmptyList() {
        final CreateIssueRequest request = new CreateIssueRequest("Deploy failure", "Step 3 failed", null);

        final CreateIssueInput input = mapper.toCreateIssueInput(request);

        assertThat(input.title()).isEqualTo("Deploy failure");
        assertThat(input.description()).isEqualTo("Step 3 failed");
        assertThat(input.labels()).isEmpty();
    }

    @Test
    @DisplayName("maps create issue request with labels unchanged")
    void mapsCreateIssueRequestWithLabelsUnchanged() {
        final CreateIssueRequest request = new CreateIssueRequest("Reporting bug", null, List.of("bug", "backend"));

        final CreateIssueInput input = mapper.toCreateIssueInput(request);

        assertThat(input.title()).isEqualTo("Reporting bug");
        assertThat(input.description()).isNull();
        assertThat(input.labels()).containsExactly("bug", "backend");
    }
}
