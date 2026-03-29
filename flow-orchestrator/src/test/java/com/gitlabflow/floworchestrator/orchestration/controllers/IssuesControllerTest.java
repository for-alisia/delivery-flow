package com.gitlabflow.floworchestrator.orchestration.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.gitlabflow.floworchestrator.common.errors.ErrorCode;
import com.gitlabflow.floworchestrator.common.errors.IntegrationException;
import com.gitlabflow.floworchestrator.integration.gitlab.GitlabIssuesClient;
import com.gitlabflow.floworchestrator.common.errors.ValidationException;
import com.gitlabflow.floworchestrator.orchestration.issues.ListProjectIssuesUseCase;
import com.gitlabflow.floworchestrator.orchestration.issues.models.IssueSummary;
import com.gitlabflow.floworchestrator.orchestration.issues.models.ListProjectIssuesResult;
import com.gitlabflow.floworchestrator.orchestration.issues.models.PaginationMetadata;

@WebMvcTest(IssuesController.class)
@DisplayName("IssuesController")
class IssuesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ListProjectIssuesUseCase listProjectIssuesUseCase;

        @MockBean
        private GitlabIssuesClient gitlabIssuesClient;

    @Test
    @DisplayName("given omitted body when search issues then defaults are applied by use case")
    void givenOmittedBodyWhenSearchIssuesThenDefaultsAreAppliedByUseCase() throws Exception {
        when(listProjectIssuesUseCase.listProjectIssues(anyString(), any(), any())).thenReturn(sampleResult(1, 20));

        mockMvc.perform(post("/api/projects/group%2Fproject/issues/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1001))
                .andExpect(jsonPath("$.pagination.currentPage").value(1))
                .andExpect(jsonPath("$.pagination.pageSize").value(20));

                verify(listProjectIssuesUseCase).listProjectIssues("group%2Fproject", null, null);
    }

    @Test
    @DisplayName("given explicit pagination body when search issues then request values are bound")
    void givenExplicitPaginationBodyWhenSearchIssuesThenRequestValuesAreBound() throws Exception {
        when(listProjectIssuesUseCase.listProjectIssues(anyString(), any(), any())).thenReturn(sampleResult(2, 5));

        mockMvc.perform(post("/api/projects/123/issues/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "page": 2,
                                  "pageSize": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.currentPage").value(2))
                .andExpect(jsonPath("$.pagination.pageSize").value(5));

        verify(listProjectIssuesUseCase).listProjectIssues("123", 2, 5);
    }

    @Test
    @DisplayName("given invalid pagination from use case validation when search issues then returns 400")
    void givenInvalidPaginationFromUseCaseValidationWhenSearchIssuesThenReturns400() throws Exception {
        when(listProjectIssuesUseCase.listProjectIssues(anyString(), any(), any()))
                .thenThrow(new ValidationException("page must be positive"));

        mockMvc.perform(post("/api/projects/123/issues/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "page": 0,
                                  "pageSize": 20
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("page must be positive"));
    }

    @Test
    @DisplayName("given malformed json body when search issues then returns 400 before use case invocation")
    void givenMalformedJsonBodyWhenSearchIssuesThenReturns400BeforeUseCaseInvocation() throws Exception {
        mockMvc.perform(post("/api/projects/123/issues/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\": 1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));

        verifyNoInteractions(listProjectIssuesUseCase);
    }

    @Test
    @DisplayName("given unknown project from integration when search issues then returns sanitized 404")
    void givenUnknownProjectFromIntegrationWhenSearchIssuesThenReturnsSanitized404() throws Exception {
        when(listProjectIssuesUseCase.listProjectIssues(anyString(), any(), any()))
                .thenThrow(new IntegrationException(
                        ErrorCode.INTEGRATION_NOT_FOUND,
                        "gitlab",
                        404,
                        "Requested project was not found or is not accessible.",
                        null,
                        null
                ));

        mockMvc.perform(post("/api/projects/404/issues/search"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Requested project was not found or is not accessible."));
    }

    @Test
    @DisplayName("given upstream is rate limited when search issues then returns 429 with retry after")
    void givenUpstreamIsRateLimitedWhenSearchIssuesThenReturns429WithRetryAfter() throws Exception {
        when(listProjectIssuesUseCase.listProjectIssues(anyString(), any(), any())).thenThrow(
                new IntegrationException(
                        ErrorCode.INTEGRATION_RATE_LIMITED,
                        "gitlab",
                        429,
                        "Issue retrieval is temporarily rate-limited by the integration provider.",
                        30L,
                        null
                )
        );

        mockMvc.perform(post("/api/projects/123/issues/search"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.retryAfterSeconds").value(30));
    }

    @Test
    @DisplayName("given upstream unavailable when search issues then returns 503")
    void givenUpstreamUnavailableWhenSearchIssuesThenReturns503() throws Exception {
        when(listProjectIssuesUseCase.listProjectIssues(anyString(), any(), any())).thenThrow(
                new IntegrationException(
                        ErrorCode.INTEGRATION_UNAVAILABLE,
                        "gitlab",
                        503,
                        "Issue retrieval is temporarily unavailable from the integration provider.",
                        null,
                        null
                )
        );

        mockMvc.perform(post("/api/projects/123/issues/search"))
                .andExpect(status().isServiceUnavailable());
    }

    private static ListProjectIssuesResult sampleResult(int page, int pageSize) {
        return new ListProjectIssuesResult(
                List.of(new IssueSummary(1001L, 101L, "Issue title", "opened", "https://gitlab.example.com/i/101")),
                new PaginationMetadata(page, pageSize, page > 1 ? page - 1 : null, page + 1, 50L, 10L)
        );
    }
}
