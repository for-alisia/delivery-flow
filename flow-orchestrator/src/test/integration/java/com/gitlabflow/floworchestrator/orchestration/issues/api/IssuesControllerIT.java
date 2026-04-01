package com.gitlabflow.floworchestrator.orchestration.issues.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlabflow.floworchestrator.common.errors.ErrorCode;
import com.gitlabflow.floworchestrator.common.errors.IntegrationException;
import com.gitlabflow.floworchestrator.common.web.GlobalExceptionHandler;
import com.gitlabflow.floworchestrator.orchestration.issues.GetIssuesService;
import com.gitlabflow.floworchestrator.orchestration.issues.Issue;
import com.gitlabflow.floworchestrator.orchestration.issues.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.IssueQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.IssueState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IssuesController.class)
@Import(GlobalExceptionHandler.class)
@SuppressWarnings("null")
class IssuesControllerIT {

    private static final String ENDPOINT = "/api/issues";
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
    private GetIssuesService getIssuesService;

    @MockBean
    private IssuesRequestMapper issuesRequestMapper;

    @MockBean
    private IssuesResponseMapper issuesResponseMapper;

    @Test
    @DisplayName("accepts missing body and returns 200")
    void acceptsMissingBody() throws Exception {
        final IssueQuery query = new IssueQuery(1, 40, null, null, null, null);
        final IssuePage issuePage = new IssuePage(List.of(), 0, 1);
        final IssuesResponse response = new IssuesResponse(List.of(), 0, 1);
        when(issuesRequestMapper.toIssueQuery(null)).thenReturn(query);
        when(getIssuesService.getIssues(query)).thenReturn(issuePage);
        when(issuesResponseMapper.toResponse(issuePage)).thenReturn(response);

        mockMvc.perform(post(ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.page").value(1));

        verify(issuesRequestMapper).toIssueQuery(null);
        verify(getIssuesService).getIssues(query);
    }

    @Test
    @DisplayName("accepts valid filter body and returns 200")
    void acceptsValidFilterBody() throws Exception {
        final IssuesRequest request = new IssuesRequest(
                new PaginationRequest(2, 20),
                new IssueFiltersRequest(OPENED, List.of("bug"), List.of(JOHN_DOE), List.of("M1"))
        );
        final IssueQuery query = new IssueQuery(2, 20, IssueState.OPENED, "bug", JOHN_DOE, "M1");
        final IssuePage issuePage = new IssuePage(
                List.of(new Issue(123L, "Title", "Desc", OPENED, List.of("bug"), JOHN_DOE, "M1", 42L)),
                1,
                2
        );
        final IssuesResponse response = new IssuesResponse(
                List.of(new IssueResponseItem(123L, "Title", "Desc", OPENED, List.of("bug"), JOHN_DOE, "M1", 42L)),
                1,
                2
        );

        when(issuesRequestMapper.toIssueQuery(any(IssuesRequest.class))).thenReturn(query);
        when(getIssuesService.getIssues(query)).thenReturn(issuePage);
        when(issuesResponseMapper.toResponse(issuePage)).thenReturn(response);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.items[0].id").value(123));
    }

    @Test
    @DisplayName("rejects multiple labels")
    void rejectsMultipleLabels() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                                                                                                .contentType(APPLICATION_JSON)
                        .content("""
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
        mockMvc.perform(post(ENDPOINT)
                                                                                                .contentType(APPLICATION_JSON)
                        .content("""
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
        mockMvc.perform(post(ENDPOINT)
                                                                                                .contentType(APPLICATION_JSON)
                        .content("""
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
        mockMvc.perform(post(ENDPOINT)
                                                                                                .contentType(APPLICATION_JSON)
                        .content("""
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
        mockMvc.perform(post(ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details[0]").value("Malformed JSON request body"));
    }

    @Test
    @DisplayName("maps integration exceptions to bad gateway")
    void mapsIntegrationExceptionsToBadGateway() throws Exception {
        when(issuesRequestMapper.toIssueQuery(any(IssuesRequest.class)))
                .thenReturn(new IssueQuery(1, 40, null, null, null, null));
        when(getIssuesService.getIssues(any(IssueQuery.class))).thenThrow(new IntegrationException(
                ErrorCode.INTEGRATION_RATE_LIMITED,
                "Unable to retrieve issues from GitLab",
                "gitlab"
        ));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath(CODE_PATH).value("INTEGRATION_RATE_LIMITED"))
                .andExpect(jsonPath("$.message").value("Unable to retrieve issues from GitLab"));
    }
}
