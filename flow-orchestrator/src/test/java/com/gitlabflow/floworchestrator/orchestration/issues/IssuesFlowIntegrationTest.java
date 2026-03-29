package com.gitlabflow.floworchestrator.orchestration.issues;

import com.gitlabflow.floworchestrator.integration.gitlab.GitLabIssuesClient;
import com.gitlabflow.floworchestrator.integration.gitlab.dto.GitLabIssueResponse;
import com.gitlabflow.floworchestrator.integration.gitlab.dto.GitLabUserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.gitlab.url=https://gitlab.com/group-a/project-a",
        "app.gitlab.token=test-token",
        "app.issues-api.default-page-size=40",
        "app.issues-api.max-page-size=100"
})
@AutoConfigureMockMvc
class IssuesFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GitLabIssuesClient gitLabIssuesClient;

    @Test
    @DisplayName("wires controller to use case to adapter and returns mapped response")
    void wiresControllerToUseCaseToAdapterAndReturnsMappedResponse() throws Exception {
        final var issueResponse = new GitLabIssueResponse(
                99L,
                "Mapped issue",
                "opened",
                List.of("backend"),
                List.of(new GitLabUserResponse("alice", "Alice", "https://gitlab.com/alice")),
                "https://gitlab.com/group-a/project-a/-/issues/99",
                OffsetDateTime.parse("2026-03-29T10:00:00Z"),
                OffsetDateTime.parse("2026-03-29T12:00:00Z")
        );

        when(gitLabIssuesClient.listIssues("group-a/project-a", "backend", List.of("alice"), 2, 20))
                .thenReturn(List.of(issueResponse));

        final String payload = """
                {
                  "labels": ["backend"],
                  "assignee": "alice",
                  "page": 2,
                  "pageSize": 20
                }
                """;

        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.issues[0].issueNumber").value(99))
                .andExpect(jsonPath("$.issues[0].assignees[0].username").value("alice"))
                .andExpect(jsonPath("$.issues[0].assignees[0].name").value("Alice"));
    }

    @Test
    @DisplayName("returns empty list when gitlab returns no issues")
    void returnsEmptyListWhenGitLabReturnsNoIssues() throws Exception {
        when(gitLabIssuesClient.listIssues("group-a/project-a", null, null, 1, 40))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issues").isArray())
                .andExpect(jsonPath("$.issues").isEmpty())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(40));
    }
}
