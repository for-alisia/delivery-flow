package com.gitlabflow.floworchestrator.orchestration.issues.rest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlabflow.floworchestrator.common.error.ErrorCode;
import com.gitlabflow.floworchestrator.common.error.IntegrationException;
import com.gitlabflow.floworchestrator.common.web.GlobalExceptionHandler;
import com.gitlabflow.floworchestrator.orchestration.issues.IssuesService;
import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.Issue;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueState;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.CreateIssueRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueFiltersRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.PaginationRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesResponse;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.mapper.IssuesRequestMapper;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.mapper.IssuesResponseMapper;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = IssuesController.class)
@Import(GlobalExceptionHandler.class)
@SuppressWarnings("null")
class IssuesControllerIT {

    private static final String SEARCH_ENDPOINT = "/api/issues/search";
    private static final String CREATE_ENDPOINT = "/api/issues";
    private static final String CODE_PATH = "$.code";
    private static final String VALIDATION_CODE = "VALIDATION_ERROR";
    private static final String OPENED = "opened";
    private static final String JOHN_DOE = "john.doe";
    private static final MediaType APPLICATION_JSON = Objects.requireNonNull(MediaType.APPLICATION_JSON);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IssuesService issuesService;

    @MockBean
    private IssuesRequestMapper issuesRequestMapper;

    @MockBean
    private IssuesResponseMapper issuesResponseMapper;

    @Test
    @DisplayName("accepts missing body and returns 200")
    void acceptsMissingBody() throws Exception {
        final IssueQuery query = new IssueQuery(1, 40, null, null, null, null);
        final IssuePage issuePage = new IssuePage(List.of(), 0, 1);
        final SearchIssuesResponse response = new SearchIssuesResponse(List.of(), 0, 1);
        when(issuesRequestMapper.toIssueQuery(null)).thenReturn(query);
        when(issuesService.getIssues(query)).thenReturn(issuePage);
        when(issuesResponseMapper.toSearchIssuesResponse(issuePage)).thenReturn(response);

        mockMvc.perform(post(SEARCH_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.page").value(1));

        verify(issuesRequestMapper).toIssueQuery(null);
        verify(issuesService).getIssues(query);
    }

    @Test
    @DisplayName("accepts valid filter body and returns 200")
    void acceptsValidFilterBody() throws Exception {
        final SearchIssuesRequest request = new SearchIssuesRequest(
                new PaginationRequest(2, 20),
                new IssueFiltersRequest(OPENED, List.of("bug"), List.of(JOHN_DOE), List.of("M1")));
        final IssueQuery query = new IssueQuery(2, 20, IssueState.OPENED, "bug", JOHN_DOE, "M1");
        final IssuePage issuePage = new IssuePage(
                List.of(new Issue(123L, 5L, "Title", "Desc", OPENED, List.of("bug"), JOHN_DOE, "M1", 42L)), 1, 2);
        final SearchIssuesResponse response = new SearchIssuesResponse(
                List.of(new IssueDto(123L, 5L, "Title", "Desc", OPENED, List.of("bug"), JOHN_DOE, "M1", 42L)), 1, 2);

        when(issuesRequestMapper.toIssueQuery(any(SearchIssuesRequest.class))).thenReturn(query);
        when(issuesService.getIssues(query)).thenReturn(issuePage);
        when(issuesResponseMapper.toSearchIssuesResponse(issuePage)).thenReturn(response);

        mockMvc.perform(post(SEARCH_ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.items[0].id").value(123))
                .andExpect(jsonPath("$.items[0].issueId").value(5));
    }

    @Test
    @DisplayName("rejects multiple labels")
    void rejectsMultipleLabels() throws Exception {
        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "filters": {
                                    "labels": ["bug", "infra"]
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE));
    }

    @Test
    @DisplayName("rejects multiple assignees")
    void rejectsMultipleAssignees() throws Exception {
        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "filters": {
                                    "assignee": ["john", "jane"]
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE));
    }

    @Test
    @DisplayName("rejects multiple milestones")
    void rejectsMultipleMilestones() throws Exception {
        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "filters": {
                                    "milestone": ["m1", "m2"]
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE));
    }

    @Test
    @DisplayName("rejects invalid pagination values")
    void rejectsInvalidPaginationValues() throws Exception {
        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "pagination": {
                                    "page": 0,
                                    "perPage": -1
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE));
    }

    @Test
    @DisplayName("rejects malformed json body")
    void rejectsMalformedJsonBody() throws Exception {
        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details[0]").value("Malformed JSON request body"));
    }

    @Test
    @DisplayName("maps integration exceptions to bad gateway")
    void mapsIntegrationExceptionsToBadGateway() throws Exception {
        when(issuesRequestMapper.toIssueQuery(any(SearchIssuesRequest.class)))
                .thenReturn(new IssueQuery(1, 40, null, null, null, null));
        when(issuesService.getIssues(any(IssueQuery.class)))
                .thenThrow(new IntegrationException(
                        ErrorCode.INTEGRATION_RATE_LIMITED, "GitLab issues operation failed", "gitlab"));

        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath(CODE_PATH).value("INTEGRATION_RATE_LIMITED"))
                .andExpect(jsonPath("$.message").value("GitLab issues operation failed"));
    }

    @Test
    @DisplayName("creates issue with full payload and returns 201")
    void createsIssueWithFullPayloadAndReturns201() throws Exception {
        final CreateIssueRequest request =
                new CreateIssueRequest("Deploy failure", "Step 3 failed", List.of("bug", "deploy"));
        final CreateIssueInput input =
                new CreateIssueInput("Deploy failure", "Step 3 failed", List.of("bug", "deploy"));
        final Issue issue = new Issue(
                84L, 10L, "Deploy failure", "Step 3 failed", OPENED, List.of("bug", "deploy"), null, null, null);
        final IssueDto response = new IssueDto(
                84L, 10L, "Deploy failure", "Step 3 failed", OPENED, List.of("bug", "deploy"), null, null, null);

        when(issuesRequestMapper.toCreateIssueInput(any(CreateIssueRequest.class)))
                .thenReturn(input);
        when(issuesService.createIssue(input)).thenReturn(issue);
        when(issuesResponseMapper.toIssueDto(issue)).thenReturn(response);

        mockMvc.perform(post(CREATE_ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(84L))
                .andExpect(jsonPath("$.issueId").value(10))
                .andExpect(jsonPath("$.title").value("Deploy failure"))
                .andExpect(jsonPath("$.description").value("Step 3 failed"))
                .andExpect(jsonPath("$.state").value(OPENED))
                .andExpect(jsonPath("$.labels[0]").value("bug"))
                .andExpect(jsonPath("$.labels[1]").value("deploy"));

        verify(issuesService).createIssue(input);
    }

    @Test
    @DisplayName("creates issue with title only and returns 201")
    void createsIssueWithTitleOnlyAndReturns201() throws Exception {
        final CreateIssueInput input = new CreateIssueInput("Reporting bug", null, List.of());
        final Issue issue = new Issue(85L, 11L, "Reporting bug", null, OPENED, List.of(), null, null, null);
        final IssueDto response = new IssueDto(85L, 11L, "Reporting bug", null, OPENED, List.of(), null, null, null);

        when(issuesRequestMapper.toCreateIssueInput(any(CreateIssueRequest.class)))
                .thenReturn(input);
        when(issuesService.createIssue(input)).thenReturn(issue);
        when(issuesResponseMapper.toIssueDto(issue)).thenReturn(response);

        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "title": "Reporting bug"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(85L))
                .andExpect(jsonPath("$.issueId").value(11))
                .andExpect(jsonPath("$.title").value("Reporting bug"))
                .andExpect(jsonPath("$.description").isEmpty())
                .andExpect(jsonPath("$.state").value(OPENED))
                .andExpect(jsonPath("$.labels").isArray());
    }

    @Test
    @DisplayName("rejects create request with blank title")
    void rejectsCreateRequestWithBlankTitle() throws Exception {
        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "title": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details[0]", containsString("title")))
                .andExpect(jsonPath("$.details[0]", containsString("must not be blank")));
    }

    @Test
    @DisplayName("rejects create request with blank label element")
    void rejectsCreateRequestWithBlankLabelElement() throws Exception {
        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "title": "Deploy failure",
                                  "labels": ["bug", " "]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details[0]", containsString("labels")))
                .andExpect(jsonPath("$.details[0]", containsString("must not be blank")));
    }

    @Test
    @DisplayName("rejects malformed json body for create request")
    void rejectsMalformedJsonBodyForCreateRequest() throws Exception {
        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details[0]").value("Malformed JSON request body"));
    }

    @Test
    @DisplayName("maps create integration exceptions to bad gateway")
    void mapsCreateIntegrationExceptionsToBadGateway() throws Exception {
        final CreateIssueInput input = new CreateIssueInput("Deploy failure", null, List.of());
        when(issuesRequestMapper.toCreateIssueInput(any(CreateIssueRequest.class)))
                .thenReturn(input);
        when(issuesService.createIssue(input))
                .thenThrow(new IntegrationException(
                        ErrorCode.INTEGRATION_AUTHENTICATION_FAILED, "GitLab create issue operation failed", "gitlab"));

        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "title": "Deploy failure"
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath(CODE_PATH).value("INTEGRATION_AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("GitLab create issue operation failed"));
    }

    @Test
    @DisplayName("rejects create request with missing body")
    void rejectsCreateRequestWithMissingBody() throws Exception {
        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE));
    }

    @Test
    @DisplayName("rejects create request with empty object")
    void rejectsCreateRequestWithEmptyObject() throws Exception {
        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details[0]", containsString("title")));
    }
}
