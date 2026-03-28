package com.gitlabflow.floworchestrator.orchestration.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gitlabflow.floworchestrator.common.errors.ErrorCode;
import com.gitlabflow.floworchestrator.common.errors.IntegrationException;
import com.gitlabflow.floworchestrator.common.errors.ValidationException;
import com.gitlabflow.floworchestrator.orchestration.issues.GetIssuesRequestValidator;
import com.gitlabflow.floworchestrator.orchestration.issues.IssuesProvider;
import com.gitlabflow.floworchestrator.orchestration.issues.models.GetIssuesRequest;

@WebMvcTest(IssuesController.class)
@DisplayName("IssuesController")
class IssuesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IssuesProvider issuesProvider;

    @MockBean
    private GetIssuesRequestValidator requestValidator;

    @Test
    @DisplayName("given no params when get issues then delegates with empty request and returns 200")
    void givenNoParamsWhenGetIssuesThenDelegatesWithEmptyRequestAndReturns200() throws Exception {
        when(issuesProvider.fetchIssues(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isOk());

        ArgumentCaptor<GetIssuesRequest> captor = ArgumentCaptor.forClass(GetIssuesRequest.class);
        verify(requestValidator).validate(captor.capture());
        verify(issuesProvider).fetchIssues(any(GetIssuesRequest.class));

        GetIssuesRequest request = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(request.assigneeId()).isNull();
        org.assertj.core.api.Assertions.assertThat(request.perPage()).isNull();
        org.assertj.core.api.Assertions.assertThat(request.page()).isNull();
    }

    @Test
    @DisplayName("given assignee id query param when get issues then binds assignee id")
    void givenAssigneeIdQueryParamWhenGetIssuesThenBindsAssigneeId() throws Exception {
        when(issuesProvider.fetchIssues(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/issues").param("assignee_id", "123"))
                .andExpect(status().isOk());

        ArgumentCaptor<GetIssuesRequest> captor = ArgumentCaptor.forClass(GetIssuesRequest.class);
        verify(requestValidator).validate(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().assigneeId()).isEqualTo(123L);
    }

    @Test
    @DisplayName("given per page and page query params when get issues then binds paging values")
    void givenPerPageAndPageQueryParamsWhenGetIssuesThenBindsPagingValues() throws Exception {
        when(issuesProvider.fetchIssues(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/issues").param("per_page", "20").param("page", "2"))
                .andExpect(status().isOk());

        ArgumentCaptor<GetIssuesRequest> captor = ArgumentCaptor.forClass(GetIssuesRequest.class);
        verify(requestValidator).validate(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().perPage()).isEqualTo(20);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().page()).isEqualTo(2);
    }

    @Test
    @DisplayName("given state query param when get issues then binds state")
    void givenStateQueryParamWhenGetIssuesThenBindsState() throws Exception {
        when(issuesProvider.fetchIssues(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/issues").param("state", "opened"))
                .andExpect(status().isOk());

        ArgumentCaptor<GetIssuesRequest> captor = ArgumentCaptor.forClass(GetIssuesRequest.class);
        verify(requestValidator).validate(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().state()).isEqualTo("opened");
    }

    @Test
    @DisplayName("given validator throws when get issues then returns 400")
    void givenValidatorThrowsWhenGetIssuesThenReturns400() throws Exception {
        doThrow(new ValidationException("Invalid state"))
                .when(requestValidator)
                .validate(any(GetIssuesRequest.class));

        mockMvc.perform(get("/api/issues").param("state", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid state"));
    }

    @Test
    @DisplayName("given provider integration error when get issues then returns 502")
    void givenProviderIntegrationErrorWhenGetIssuesThenReturns502() throws Exception {
        when(issuesProvider.fetchIssues(any())).thenThrow(
                new IntegrationException(
                        ErrorCode.INTEGRATION_UNAUTHORIZED,
                        "gitlab",
                        401,
                        "GitLab unauthorized",
                        null,
                        null
                )
        );

        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isBadGateway());
    }
}
