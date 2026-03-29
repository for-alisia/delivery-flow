package com.gitlabflow.floworchestrator.orchestration.controllers;

import com.gitlabflow.floworchestrator.common.errors.ValidationException;
import com.gitlabflow.floworchestrator.common.web.GlobalExceptionHandler;
import com.gitlabflow.floworchestrator.orchestration.issues.ListIssuesUseCase;
import com.gitlabflow.floworchestrator.orchestration.issues.model.ListIssuesResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IssuesController.class)
@Import(GlobalExceptionHandler.class)
class IssuesControllerSlice1WebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ListIssuesUseCase listIssuesUseCase;

    @Test
    @DisplayName("accepts POST request with no body")
    void acceptsPostRequestWithNoBody() throws Exception {
        when(listIssuesUseCase.listIssues(any(), any(), any(), any()))
                .thenReturn(new ListIssuesResult(List.of(), 1, 40));

        mockMvc.perform(post("/api/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issues").isArray())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(40));
    }

    @Test
    @DisplayName("returns validation error for multiple labels")
    void returnsValidationErrorForMultipleLabels() throws Exception {
        final var body = """
                {
                  "labels": ["bug", "backend"]
                }
                """;

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("returns validation error for invalid pagination")
    void returnsValidationErrorForInvalidPagination() throws Exception {
        final var body = """
                {
                  "page": 0,
                  "pageSize": 0
                }
                """;

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("returns sanitized validation response from use case")
    void returnsSanitizedValidationResponseFromUseCase() throws Exception {
        when(listIssuesUseCase.listIssues(any(), any(), any(), any()))
                .thenThrow(new ValidationException("Invalid payload", List.of("pageSize must be <= 100")));

        mockMvc.perform(post("/api/issues").contentType(MediaType.APPLICATION_JSON_VALUE).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0]").value("pageSize must be <= 100"));
    }
}