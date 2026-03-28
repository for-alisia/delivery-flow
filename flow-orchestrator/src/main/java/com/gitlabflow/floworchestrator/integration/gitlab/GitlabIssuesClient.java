package com.gitlabflow.floworchestrator.integration.gitlab;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

import com.gitlabflow.floworchestrator.integration.gitlab.models.dto.GitLabIssueResponseDTO;

public interface GitlabIssuesClient {

    @GetMapping("/issues")
    List<GitLabIssueResponseDTO> getIssues(@SpringQueryMap Map<String, Object> queryParams);
}
