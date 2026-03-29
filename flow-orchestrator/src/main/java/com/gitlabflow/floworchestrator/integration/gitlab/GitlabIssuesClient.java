package com.gitlabflow.floworchestrator.integration.gitlab;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.gitlabflow.floworchestrator.integration.gitlab.models.dto.GitLabIssueResponseDTO;

@FeignClient(
        name = "gitlabIssuesClient",
        url = "#{@gitlabApiBaseUrl}",
        configuration = GitlabIntegrationConfiguration.class
)
public interface GitlabIssuesClient {

    @GetMapping("/api/v4/projects/{id}/issues")
    ResponseEntity<List<GitLabIssueResponseDTO>> listProjectIssues(
            @PathVariable("id") String projectId,
            @RequestParam("page") int page,
            @RequestParam("per_page") int perPage
    );
}
