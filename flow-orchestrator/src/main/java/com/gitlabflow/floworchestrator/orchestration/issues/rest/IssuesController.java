package com.gitlabflow.floworchestrator.orchestration.issues.rest;

import com.gitlabflow.floworchestrator.orchestration.issues.IssuesService;
import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.EnrichedIssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueSummary;
import com.gitlabflow.floworchestrator.orchestration.issues.model.UpdateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.CreateIssueRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDetailDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueSummaryDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesResponse;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.UpdateIssueRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.mapper.IssuesRequestMapper;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.mapper.IssuesResponseMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
public class IssuesController {

    private static final String ISSUE_ID_POSITIVE_MESSAGE = "issueId must be a positive number";

    private final IssuesService issuesService;
    private final IssuesRequestMapper issuesRequestMapper;
    private final IssuesResponseMapper issuesResponseMapper;
    private final IssuesRequestValidator issuesRequestValidator;

    @PostMapping("/search")
    public SearchIssuesResponse getIssues(@RequestBody(required = false) @Valid final SearchIssuesRequest request) {
        log.info("Issues API request received hasBody={}", request != null);
        final IssueQuery query = issuesRequestMapper.toIssueQuery(request);
        final SearchIssuesResponse response =
                issuesResponseMapper.toSearchIssuesResponse(issuesService.getIssues(query));
        log.info("Issues API response returned count={} page={}", response.count(), response.page());
        return response;
    }

    @PostMapping
    public ResponseEntity<IssueSummaryDto> createIssue(@RequestBody @Valid final CreateIssueRequest request) {
        issuesRequestValidator.validateCreateRequest(request);
        final List<String> labels = request.labels();
        log.info(
                "Create issue request received descriptionPresent={} labelCount={}",
                request.description() != null,
                labels == null ? 0 : labels.size());
        final CreateIssueInput input = issuesRequestMapper.toCreateIssueInput(request);
        final IssueSummary issue = issuesService.createIssue(input);
        final IssueSummaryDto response = issuesResponseMapper.toIssueSummaryDto(issue);
        log.info("Create issue response returned id={}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{issueId}")
    public ResponseEntity<IssueSummaryDto> updateIssue(
            @PathVariable @Positive(message = ISSUE_ID_POSITIVE_MESSAGE) final long issueId,
            @RequestBody @Valid final UpdateIssueRequest request) {
        final List<String> addLabels = request.addLabels();
        final List<String> removeLabels = request.removeLabels();
        log.info(
                "Update issue request received issueId={} titlePresent={} descriptionProvided={} addLabelCount={} removeLabelCount={}",
                issueId,
                request.title() != null,
                request.description() != null,
                addLabels == null ? 0 : addLabels.size(),
                removeLabels == null ? 0 : removeLabels.size());

        issuesRequestValidator.validateUpdateRequest(request);

        final UpdateIssueInput input = issuesRequestMapper.toUpdateIssueInput(issueId, request);
        final IssueSummary issue = issuesService.updateIssue(input);
        final IssueSummaryDto response = issuesResponseMapper.toIssueSummaryDto(issue);
        log.info("Update issue response returned issueId={}", issueId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{issueId}")
    public ResponseEntity<Void> deleteIssue(
            @PathVariable @Positive(message = ISSUE_ID_POSITIVE_MESSAGE) final long issueId) {
        log.info("Delete issue request received issueId={}", issueId);
        issuesService.deleteIssue(issueId);
        log.info("Delete issue response returned issueId={}", issueId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{issueId}")
    public IssueDetailDto getIssueDetail(
            @PathVariable @Positive(message = ISSUE_ID_POSITIVE_MESSAGE) final long issueId) {
        log.info("Get issue detail request received issueId={}", issueId);
        final EnrichedIssueDetail enrichedDetail = issuesService.getIssueDetail(issueId);
        final IssueDetailDto response = issuesResponseMapper.toIssueDetailDto(enrichedDetail);
        log.info("Get issue detail response returned issueId={}", issueId);
        return response;
    }
}
