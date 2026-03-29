package com.gitlabflow.floworchestrator.orchestration.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gitlabflow.floworchestrator.orchestration.controllers.models.ListProjectIssuesRequestBody;
import com.gitlabflow.floworchestrator.orchestration.controllers.models.ListProjectIssuesResponse;
import com.gitlabflow.floworchestrator.orchestration.controllers.models.PaginationResponse;
import com.gitlabflow.floworchestrator.orchestration.controllers.models.ProjectIssueResponseItem;
import com.gitlabflow.floworchestrator.orchestration.issues.ListProjectIssuesUseCase;
import com.gitlabflow.floworchestrator.orchestration.issues.models.ListProjectIssuesResult;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects/{projectId}/issues")
public class IssuesController {

    private final ListProjectIssuesUseCase listProjectIssuesUseCase;

    @PostMapping("/search")
    public ResponseEntity<ListProjectIssuesResponse> searchProjectIssues(
            @PathVariable String projectId,
            @RequestBody(required = false) ListProjectIssuesRequestBody requestBody
    ) {
        final Integer page = requestBody == null ? null : requestBody.page();
        final Integer pageSize = requestBody == null ? null : requestBody.pageSize();

        final ListProjectIssuesResult result = listProjectIssuesUseCase.listProjectIssues(projectId, page, pageSize);

        final List<ProjectIssueResponseItem> items = result.items().stream()
                .map(issue -> new ProjectIssueResponseItem(
                        issue.id(),
                        issue.iid(),
                        issue.title(),
                        issue.state(),
                        issue.webUrl()
                ))
                .toList();

        final PaginationResponse pagination = new PaginationResponse(
                result.pagination().currentPage(),
                result.pagination().pageSize(),
                result.pagination().previousPage(),
                result.pagination().nextPage(),
                result.pagination().totalItems(),
                result.pagination().totalPages()
        );

        return ResponseEntity.ok(new ListProjectIssuesResponse(items, pagination));
    }
}
