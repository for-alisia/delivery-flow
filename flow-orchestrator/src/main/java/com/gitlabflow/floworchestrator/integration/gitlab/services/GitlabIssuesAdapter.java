package com.gitlabflow.floworchestrator.integration.gitlab.services;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import com.gitlabflow.floworchestrator.integration.gitlab.GitlabIssuesClient;
import com.gitlabflow.floworchestrator.integration.gitlab.models.dto.GitLabIssueResponseDTO;
import com.gitlabflow.floworchestrator.orchestration.issues.IssuesProvider;
import com.gitlabflow.floworchestrator.orchestration.issues.models.IssueSummary;
import com.gitlabflow.floworchestrator.orchestration.issues.models.ListProjectIssuesQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.models.ListProjectIssuesResult;
import com.gitlabflow.floworchestrator.orchestration.issues.models.PaginationMetadata;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GitlabIssuesAdapter implements IssuesProvider {

    private final GitlabIssuesClient gitlabIssuesClient;

    @Override
    public ListProjectIssuesResult listProjectIssues(ListProjectIssuesQuery query) {
        final String encodedProjectId = UriUtils.encodePathSegment(query.projectId(), StandardCharsets.UTF_8);
        final ResponseEntity<List<GitLabIssueResponseDTO>> response = gitlabIssuesClient.listProjectIssues(
                encodedProjectId,
                query.page(),
                query.pageSize()
        );

        final List<GitLabIssueResponseDTO> body = response.getBody() == null ? Collections.emptyList() : response.getBody();
        final List<IssueSummary> items = body.stream()
                .map(this::mapIssue)
                .toList();

        final PaginationMetadata paginationMetadata = new PaginationMetadata(
                query.page(),
                query.pageSize(),
                parseIntegerHeader(response.getHeaders(), "x-prev-page"),
                parseIntegerHeader(response.getHeaders(), "x-next-page"),
                parseLongHeader(response.getHeaders(), "x-total"),
                parseLongHeader(response.getHeaders(), "x-total-pages")
        );

        return new ListProjectIssuesResult(items, paginationMetadata);
    }

    private IssueSummary mapIssue(GitLabIssueResponseDTO issue) {
        return new IssueSummary(
                issue.id(),
                issue.iid(),
                issue.title(),
                issue.state(),
                issue.webUrl()
        );
    }

    private Integer parseIntegerHeader(HttpHeaders headers, String key) {
        String raw = headers.getFirst(key);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Integer.parseInt(raw);
    }

    private Long parseLongHeader(HttpHeaders headers, String key) {
        String raw = headers.getFirst(key);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Long.parseLong(raw);
    }
}
