package com.gitlabflow.floworchestrator.integration.gitlab.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.gitlabflow.floworchestrator.integration.gitlab.models.dto.GitLabIssueResponseDTO;
import com.gitlabflow.floworchestrator.integration.gitlab.models.dto.GitLabUserDTO;
import com.gitlabflow.floworchestrator.orchestration.issues.models.GetIssuesRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.models.IssueSummary;

@Component
public class GitlabIssuesMapper {

    public Map<String, Object> buildQueryParams(GetIssuesRequest request) {
        Map<String, Object> queryParams = new LinkedHashMap<>();
        putIfNotNull(queryParams, "assignee_id", request.assigneeId());
        putIfNotNull(queryParams, "author_id", request.authorId());
        putIfNotNull(queryParams, "milestone", request.milestone());
        putIfNotNull(queryParams, "state", request.state());
        putIfNotNull(queryParams, "search", request.search());
        putIfNotNull(queryParams, "labels", request.labels());
        putIfNotNull(queryParams, "order_by", request.orderBy());
        putIfNotNull(queryParams, "sort", request.sort());
        putIfNotNull(queryParams, "page", request.page());
        putIfNotNull(queryParams, "per_page", request.perPage());
        return queryParams;
    }

    public IssueSummary toIssueSummary(GitLabIssueResponseDTO dto) {
        return new IssueSummary(
                dto.projectId(),
                dto.id(),
                dto.iid(),
                dto.title(),
                dto.description(),
                dto.state(),
                dto.labels() == null ? List.of() : dto.labels(),
                dto.author() == null ? null : dto.author().username(),
                dto.assignees() == null
                        ? List.of()
                    : dto.assignees().stream().map(GitLabUserDTO::username).toList(),
                dto.webUrl(),
                dto.createdAt(),
                dto.updatedAt(),
                dto.closedAt(),
                dto.milestone() == null ? null : dto.milestone().title()
        );
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
