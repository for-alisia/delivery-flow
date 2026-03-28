package com.gitlabflow.floworchestrator.orchestration.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gitlabflow.floworchestrator.orchestration.issues.GetIssuesRequestValidator;
import com.gitlabflow.floworchestrator.orchestration.issues.IssuesProvider;
import com.gitlabflow.floworchestrator.orchestration.issues.models.IssueSummary;
import com.gitlabflow.floworchestrator.orchestration.issues.models.GetIssuesRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
public class IssuesController {

    private final IssuesProvider issuesProvider;
    private final GetIssuesRequestValidator requestValidator;

    @GetMapping
    public List<IssueSummary> getIssues(
            @RequestParam(name = "assignee_id", required = false) Long assigneeId,
            @RequestParam(name = "author_id", required = false) Long authorId,
            @RequestParam(name = "milestone", required = false) String milestone,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "labels", required = false) String labels,
            @RequestParam(name = "order_by", required = false) String orderBy,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "per_page", required = false) Integer perPage) {
        GetIssuesRequest request = new GetIssuesRequest(
                assigneeId,
                authorId,
                milestone,
                state,
                search,
                labels,
                orderBy,
                sort,
                page,
                perPage
        );
            requestValidator.validate(request);
        log.debug("Incoming request to list issues with filters: {}", request);
        List<IssueSummary> issues = issuesProvider.fetchIssues(request);
        log.info("Retrieved {} issues", issues.size());
        return issues;
    }

}
