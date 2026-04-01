package com.gitlabflow.floworchestrator.orchestration.issues.api;

import com.gitlabflow.floworchestrator.orchestration.issues.GetIssuesService;
import com.gitlabflow.floworchestrator.orchestration.issues.IssueQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
public class IssuesController {

    private final GetIssuesService getIssuesService;
    private final IssuesRequestMapper issuesRequestMapper;
    private final IssuesResponseMapper issuesResponseMapper;

    @PostMapping
    public IssuesResponse getIssues(@RequestBody(required = false) @Valid final IssuesRequest request) {
        log.info("Issues API request received hasBody={}", request != null);
        final IssueQuery query = issuesRequestMapper.toIssueQuery(request);
        final IssuesResponse response = issuesResponseMapper.toResponse(getIssuesService.getIssues(query));
        log.info("Issues API response returned count={} page={}", response.count(), response.page());
        return response;
    }
}