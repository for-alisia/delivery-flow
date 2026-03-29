package com.gitlabflow.floworchestrator.integration.gitlab;

import com.gitlabflow.floworchestrator.integration.gitlab.dto.GitLabIssueResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "gitLabIssuesClient", url = "${app.gitlab.api-base-fallback-url:https://gitlab.com/api/v4}")
public interface GitLabIssuesClient {

    @GetMapping("/projects/{projectId}/issues")
    List<GitLabIssueResponse> listIssues(@PathVariable("projectId") String projectId,
                                         @RequestParam(name = "labels", required = false) String labels,
                                         @RequestParam(name = "assignee_username[]", required = false) List<String> assigneeUsernames,
                                         @RequestParam("page") int page,
                                         @RequestParam("per_page") int perPage);
}
