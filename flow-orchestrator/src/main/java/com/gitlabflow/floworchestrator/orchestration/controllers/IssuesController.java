package com.gitlabflow.floworchestrator.orchestration.controllers;

import com.gitlabflow.floworchestrator.orchestration.controllers.dto.AssigneeResponseBody;
import com.gitlabflow.floworchestrator.orchestration.controllers.dto.IssueResponseBody;
import com.gitlabflow.floworchestrator.orchestration.controllers.dto.ListIssuesRequestBody;
import com.gitlabflow.floworchestrator.orchestration.controllers.dto.ListIssuesResponseBody;
import com.gitlabflow.floworchestrator.orchestration.issues.ListIssuesUseCase;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueSummary;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
public class IssuesController {

    private final ListIssuesUseCase listIssuesUseCase;

    @PostMapping
    public ListIssuesResponseBody listIssues(@Valid @RequestBody(required = false) final ListIssuesRequestBody requestBody) {
        final List<String> labels = requestBody == null ? null : requestBody.labels();
        final String assignee = requestBody == null ? null : requestBody.assignee();
        final Integer page = requestBody == null ? null : requestBody.page();
        final Integer pageSize = requestBody == null ? null : requestBody.pageSize();

        final var result = listIssuesUseCase.listIssues(labels, assignee, page, pageSize);

        final List<IssueResponseBody> issues = result.issues().stream()
                .map(this::toResponseBody)
                .toList();

        return new ListIssuesResponseBody(issues, result.page(), result.pageSize());
    }

    private IssueResponseBody toResponseBody(final IssueSummary summary) {
        final List<AssigneeResponseBody> assignees = summary.assignees().stream()
                .map(assignee -> new AssigneeResponseBody(assignee.username(), assignee.name(), assignee.webUrl()))
                .toList();

        return new IssueResponseBody(
                summary.issueNumber(),
                summary.title(),
                summary.state(),
                summary.labels(),
                assignees,
                summary.webUrl(),
                summary.createdAt(),
                summary.updatedAt()
        );
    }
}
