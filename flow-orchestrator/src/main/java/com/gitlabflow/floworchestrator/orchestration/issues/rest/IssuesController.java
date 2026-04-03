package com.gitlabflow.floworchestrator.orchestration.issues.rest;

import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.Issue;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.IssuesService;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.CreateIssueRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesResponse;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.mapper.IssuesRequestMapper;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.mapper.IssuesResponseMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
public class IssuesController {

    private final IssuesService issuesService;
    private final IssuesRequestMapper issuesRequestMapper;
    private final IssuesResponseMapper issuesResponseMapper;

    @PostMapping("/search")
    public SearchIssuesResponse getIssues(@RequestBody(required = false) @Valid final SearchIssuesRequest request) {
        log.info("Issues API request received hasBody={}", request != null);
        final IssueQuery query = issuesRequestMapper.toIssueQuery(request);
        final SearchIssuesResponse response = issuesResponseMapper.toSearchIssuesResponse(issuesService.getIssues(query));
        log.info("Issues API response returned count={} page={}", response.count(), response.page());
        return response;
    }

    @PostMapping
    public ResponseEntity<IssueDto> createIssue(@RequestBody @Valid final CreateIssueRequest request) {
        final List<String> labels = request.labels();
        log.info(
                "Create issue request received descriptionPresent={} labelCount={}",
                request.description() != null,
                labels == null ? 0 : labels.size()
        );
        final CreateIssueInput input = issuesRequestMapper.toCreateIssueInput(request);
        final Issue issue = issuesService.createIssue(input);
        final IssueDto response = issuesResponseMapper.toIssueDto(issue);
        log.info("Create issue response returned id={}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}