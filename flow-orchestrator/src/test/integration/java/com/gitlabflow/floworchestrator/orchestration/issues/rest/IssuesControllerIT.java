package com.gitlabflow.floworchestrator.orchestration.issues.rest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlabflow.floworchestrator.common.error.ErrorCode;
import com.gitlabflow.floworchestrator.common.error.IntegrationException;
import com.gitlabflow.floworchestrator.common.error.ValidationException;
import com.gitlabflow.floworchestrator.common.web.GlobalExceptionHandler;
import com.gitlabflow.floworchestrator.orchestration.common.model.ChangeField;
import com.gitlabflow.floworchestrator.orchestration.common.model.User;
import com.gitlabflow.floworchestrator.orchestration.common.rest.dto.LabelChangeDto;
import com.gitlabflow.floworchestrator.orchestration.common.rest.dto.LabelChangeSetDto;
import com.gitlabflow.floworchestrator.orchestration.common.rest.dto.UserDto;
import com.gitlabflow.floworchestrator.orchestration.issues.IssuesService;
import com.gitlabflow.floworchestrator.orchestration.issues.model.CreateIssueInput;
import com.gitlabflow.floworchestrator.orchestration.issues.model.EnrichedIssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueAuditType;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueDetail;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssuePage;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueState;
import com.gitlabflow.floworchestrator.orchestration.issues.model.IssueSummary;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.CreateIssueRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueDetailDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueFiltersRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.IssueSummaryDto;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.PaginationRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.SearchIssuesResponse;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.mapper.IssuesRequestMapper;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.mapper.IssuesResponseMapper;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.Milestone;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.MilestoneDto;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = IssuesController.class)
@Import(GlobalExceptionHandler.class)
class IssuesControllerIT {

    private static final String SEARCH_ENDPOINT = "/api/issues/search";
    private static final String CREATE_ENDPOINT = "/api/issues";
    private static final String DELETE_ENDPOINT = "/api/issues/{issueId}";
    private static final String GET_SINGLE_ENDPOINT = "/api/issues/{issueId}";
    private static final String CODE_PATH = "$.code";
    private static final String VALIDATION_CODE = "VALIDATION_ERROR";
    private static final String ISSUE_ID_POSITIVE_MESSAGE = "issueId must be a positive number";
    private static final String OPENED = "opened";
    private static final String JOHN_DOE = "john.doe";
    private static final @NonNull MediaType APPLICATION_JSON = Objects.requireNonNull(MediaType.APPLICATION_JSON);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IssuesService issuesService;

    @MockBean
    private IssuesRequestMapper issuesRequestMapper;

    @MockBean
    private IssuesResponseMapper issuesResponseMapper;

    @Test
    @DisplayName("accepts missing body and returns 200")
    void acceptsMissingBody() throws Exception {
        final IssueQuery query = new IssueQuery(1, 20, null, null, null, null, List.of());
        final IssuePage issuePage = new IssuePage(List.of(), 0, 1);
        final SearchIssuesResponse response = new SearchIssuesResponse(List.of(), 0, 1);
        when(issuesRequestMapper.toIssueQuery(null)).thenReturn(query);
        when(issuesService.getIssues(query)).thenReturn(issuePage);
        when(issuesResponseMapper.toSearchIssuesResponse(issuePage)).thenReturn(response);

        mockMvc.perform(post(SEARCH_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.page").value(1));

        verify(issuesRequestMapper).toIssueQuery(null);
        verify(issuesService).getIssues(query);
    }

    @Test
    @DisplayName("accepts valid filter body and returns 200")
    void acceptsValidFilterBody() throws Exception {
        final SearchIssuesRequest request = new SearchIssuesRequest(
                new PaginationRequest(2, 20),
                new IssueFiltersRequest(OPENED, List.of("bug"), List.of(JOHN_DOE), List.of("M1"), List.of()));
        final IssueQuery query = new IssueQuery(2, 20, IssueState.OPENED, "bug", JOHN_DOE, "M1", List.of());
        final IssuePage issuePage = new IssuePage(
                List.of(new IssueSummary(123L, 5L, "Title", "Desc", OPENED, List.of("bug"), JOHN_DOE, "M1", 42L, null)),
                1,
                2);
        final SearchIssuesResponse response = new SearchIssuesResponse(
                List.of(new IssueSummaryDto(
                        123L, 5L, "Title", "Desc", OPENED, List.of("bug"), JOHN_DOE, "M1", 42L, null)),
                1,
                2);

        when(issuesRequestMapper.toIssueQuery(any(SearchIssuesRequest.class))).thenReturn(query);
        when(issuesService.getIssues(query)).thenReturn(issuePage);
        when(issuesResponseMapper.toSearchIssuesResponse(issuePage)).thenReturn(response);

        mockMvc.perform(post(SEARCH_ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.items[0].id").value(123))
                .andExpect(jsonPath("$.items[0].issueId").value(5));
    }

    @Test
    @DisplayName("returns lowercase change field for search audit response")
    void returnsLowercaseChangeFieldForSearchAuditResponse() throws Exception {
        final SearchIssuesRequest request = new SearchIssuesRequest(
                new PaginationRequest(1, 20),
                new IssueFiltersRequest(OPENED, List.of("bug"), List.of(JOHN_DOE), List.of("M1"), List.of("label")));
        final IssueQuery query =
                new IssueQuery(1, 20, IssueState.OPENED, "bug", JOHN_DOE, "M1", List.of(IssueAuditType.LABEL));
        final IssuePage issuePage = new IssuePage(List.of(), 0, 1);
        final IssueSummaryDto responseItem = IssueSummaryDto.builder()
                .id(123L)
                .issueId(5L)
                .title("Title")
                .description("Desc")
                .state(OPENED)
                .labels(List.of("bug"))
                .assignee(JOHN_DOE)
                .milestone("M1")
                .parent(42L)
                .changeSets(List.of(LabelChangeSetDto.builder()
                        .changeType("add")
                        .changedBy(UserDto.builder()
                                .id(1L)
                                .username("root")
                                .name("Administrator")
                                .build())
                        .change(LabelChangeDto.builder()
                                .field(ChangeField.LABEL)
                                .id(73L)
                                .name("bug")
                                .build())
                        .changedAt(null)
                        .build()))
                .build();
        final SearchIssuesResponse response = new SearchIssuesResponse(List.of(responseItem), 1, 1);

        when(issuesRequestMapper.toIssueQuery(any(SearchIssuesRequest.class))).thenReturn(query);
        when(issuesService.getIssues(query)).thenReturn(issuePage);
        when(issuesResponseMapper.toSearchIssuesResponse(issuePage)).thenReturn(response);

        mockMvc.perform(post(SEARCH_ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].changeSets[0].change.field").value("label"));
    }

    @Test
    @DisplayName("rejects multiple labels")
    void rejectsMultipleLabels() throws Exception {
        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "filters": {
                                    "labels": ["bug", "infra"]
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE));
    }

    @Test
    @DisplayName("rejects multiple assignees")
    void rejectsMultipleAssignees() throws Exception {
        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "filters": {
                                    "assignee": ["john", "jane"]
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE));
    }

    @Test
    @DisplayName("rejects multiple milestones")
    void rejectsMultipleMilestones() throws Exception {
        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "filters": {
                                    "milestone": ["m1", "m2"]
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE));
    }

    @Test
    @DisplayName("rejects unsupported audit filter values")
    void rejectsUnsupportedAuditFilterValues() throws Exception {
        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                                                                                                                {
                                                                                                                                        "filters": {
                                                                                                                                                "audit": ["milestone"]
                                                                                                                                        }
                                                                                                                                }
                                                                                                                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details[0]", Objects.requireNonNull(containsString("filters.audit[0]"))));
    }

    @Test
    @DisplayName("rejects invalid pagination values")
    void rejectsInvalidPaginationValues() throws Exception {
        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "pagination": {
                                    "page": 0,
                                    "perPage": -1
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE));
    }

    @Test
    @DisplayName("rejects malformed json body")
    void rejectsMalformedJsonBody() throws Exception {
        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details[0]").value("Malformed JSON request body"));
    }

    @Test
    @DisplayName("maps search integration rate limit to too many requests")
    void mapsSearchIntegrationRateLimitToTooManyRequests() throws Exception {
        when(issuesRequestMapper.toIssueQuery(any(SearchIssuesRequest.class)))
                .thenReturn(new IssueQuery(1, 20, null, null, null, null, List.of()));
        when(issuesService.getIssues(any(IssueQuery.class)))
                .thenThrow(new IntegrationException(
                        ErrorCode.INTEGRATION_RATE_LIMITED, "GitLab issues operation failed", "gitlab"));

        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath(CODE_PATH).value("INTEGRATION_RATE_LIMITED"))
                .andExpect(jsonPath("$.message").value("GitLab issues operation failed"));
    }

    @Test
    @DisplayName("maps perPage above max to validation error")
    void mapsPerPageAboveMaxToValidationError() throws Exception {
        final IssueQuery query = new IssueQuery(1, 41, null, null, null, null, List.of());
        when(issuesRequestMapper.toIssueQuery(any(SearchIssuesRequest.class))).thenReturn(query);
        when(issuesService.getIssues(query))
                .thenThrow(new ValidationException(
                        "Request validation failed", List.of("pagination.perPage must be less than or equal to 40")));

        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                                                                                                                {
                                                                                                                                        "pagination": {
                                                                                                                                                "page": 1,
                                                                                                                                                "perPage": 41
                                                                                                                                        }
                                                                                                                                }
                                                                                                                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details[0]").value("pagination.perPage must be less than or equal to 40"));
    }

    @Test
    @DisplayName("creates issue with full payload and returns 201")
    void createsIssueWithFullPayloadAndReturns201() throws Exception {
        final CreateIssueRequest request =
                new CreateIssueRequest("Deploy failure", "Step 3 failed", List.of("bug", "deploy"));
        final CreateIssueInput input =
                new CreateIssueInput("Deploy failure", "Step 3 failed", List.of("bug", "deploy"));
        final IssueSummary issue = new IssueSummary(
                84L, 10L, "Deploy failure", "Step 3 failed", OPENED, List.of("bug", "deploy"), null, null, null, null);
        final IssueSummaryDto response = new IssueSummaryDto(
                84L, 10L, "Deploy failure", "Step 3 failed", OPENED, List.of("bug", "deploy"), null, null, null, null);

        when(issuesRequestMapper.toCreateIssueInput(any(CreateIssueRequest.class)))
                .thenReturn(input);
        when(issuesService.createIssue(input)).thenReturn(issue);
        when(issuesResponseMapper.toIssueSummaryDto(issue)).thenReturn(response);

        mockMvc.perform(post(CREATE_ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(84L))
                .andExpect(jsonPath("$.issueId").value(10))
                .andExpect(jsonPath("$.title").value("Deploy failure"))
                .andExpect(jsonPath("$.description").value("Step 3 failed"))
                .andExpect(jsonPath("$.state").value(OPENED))
                .andExpect(jsonPath("$.labels[0]").value("bug"))
                .andExpect(jsonPath("$.labels[1]").value("deploy"));

        verify(issuesService).createIssue(input);
    }

    @Test
    @DisplayName("creates issue with title only and returns 201")
    void createsIssueWithTitleOnlyAndReturns201() throws Exception {
        final CreateIssueInput input = new CreateIssueInput("Reporting bug", null, List.of());
        final IssueSummary issue =
                new IssueSummary(85L, 11L, "Reporting bug", null, OPENED, List.of(), null, null, null, null);
        final IssueSummaryDto response =
                new IssueSummaryDto(85L, 11L, "Reporting bug", null, OPENED, List.of(), null, null, null, null);

        when(issuesRequestMapper.toCreateIssueInput(any(CreateIssueRequest.class)))
                .thenReturn(input);
        when(issuesService.createIssue(input)).thenReturn(issue);
        when(issuesResponseMapper.toIssueSummaryDto(issue)).thenReturn(response);

        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "title": "Reporting bug"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(85L))
                .andExpect(jsonPath("$.issueId").value(11))
                .andExpect(jsonPath("$.title").value("Reporting bug"))
                .andExpect(jsonPath("$.description").isEmpty())
                .andExpect(jsonPath("$.state").value(OPENED))
                .andExpect(jsonPath("$.labels").isArray());
    }

    @Test
    @DisplayName("rejects create request with blank title")
    void rejectsCreateRequestWithBlankTitle() throws Exception {
        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "title": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details[0]", Objects.requireNonNull(containsString("title"))))
                .andExpect(jsonPath("$.details[0]", Objects.requireNonNull(containsString("must not be blank"))));
    }

    @Test
    @DisplayName("rejects create request with blank label element")
    void rejectsCreateRequestWithBlankLabelElement() throws Exception {
        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "title": "Deploy failure",
                                  "labels": ["bug", " "]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details[0]", Objects.requireNonNull(containsString("labels"))))
                .andExpect(jsonPath("$.details[0]", Objects.requireNonNull(containsString("must not be blank"))));
    }

    @Test
    @DisplayName("rejects malformed json body for create request")
    void rejectsMalformedJsonBodyForCreateRequest() throws Exception {
        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details[0]").value("Malformed JSON request body"));
    }

    @Test
    @DisplayName("maps create integration auth failure to unauthorized")
    void mapsCreateIntegrationAuthFailureToUnauthorized() throws Exception {
        final CreateIssueInput input = new CreateIssueInput("Deploy failure", null, List.of());
        when(issuesRequestMapper.toCreateIssueInput(any(CreateIssueRequest.class)))
                .thenReturn(input);
        when(issuesService.createIssue(input))
                .thenThrow(new IntegrationException(
                        ErrorCode.INTEGRATION_AUTHENTICATION_FAILED, "GitLab create issue operation failed", "gitlab"));

        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "title": "Deploy failure"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(CODE_PATH).value("INTEGRATION_AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("GitLab create issue operation failed"));
    }

    @Test
    @DisplayName("deletes issue and returns 204 with empty body")
    void deletesIssueAndReturns204WithEmptyBody() throws Exception {
        mockMvc.perform(delete(DELETE_ENDPOINT, 42L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(issuesService).deleteIssue(42L);
    }

    @Test
    @DisplayName("rejects delete request when issue id is zero")
    void rejectsDeleteRequestWhenIssueIdIsZero() throws Exception {
        mockMvc.perform(delete(DELETE_ENDPOINT, 0L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details[0]").value(ISSUE_ID_POSITIVE_MESSAGE));
    }

    @Test
    @DisplayName("rejects delete request when issue id is negative")
    void rejectsDeleteRequestWhenIssueIdIsNegative() throws Exception {
        mockMvc.perform(delete(DELETE_ENDPOINT, -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details[0]").value(ISSUE_ID_POSITIVE_MESSAGE));
    }

    @Test
    @DisplayName("rejects delete request when issue id is non numeric")
    void rejectsDeleteRequestWhenIssueIdIsNonNumeric() throws Exception {
        mockMvc.perform(delete(DELETE_ENDPOINT, "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details[0]").value(ISSUE_ID_POSITIVE_MESSAGE));
    }

    @Test
    @DisplayName("maps delete integration not found to 404")
    void mapsDeleteIntegrationNotFoundTo404() throws Exception {
        doThrow(new IntegrationException(
                        ErrorCode.RESOURCE_NOT_FOUND, "GitLab delete issue operation failed", "gitlab"))
                .when(issuesService)
                .deleteIssue(777L);

        mockMvc.perform(delete(DELETE_ENDPOINT, 777L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(CODE_PATH).value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("GitLab delete issue operation failed"));
    }

    @Test
    @DisplayName("maps delete integration forbidden to 403")
    void mapsDeleteIntegrationForbiddenTo403() throws Exception {
        doThrow(new IntegrationException(
                        ErrorCode.INTEGRATION_FORBIDDEN, "GitLab delete issue operation failed", "gitlab"))
                .when(issuesService)
                .deleteIssue(778L);

        mockMvc.perform(delete(DELETE_ENDPOINT, 778L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(CODE_PATH).value("INTEGRATION_FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("GitLab delete issue operation failed"));
    }

    @Test
    @DisplayName("rejects create request with missing body")
    void rejectsCreateRequestWithMissingBody() throws Exception {
        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE));
    }

    @Test
    @DisplayName("rejects create request with empty object")
    void rejectsCreateRequestWithEmptyObject() throws Exception {
        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details[0]", Objects.requireNonNull(containsString("title"))));
    }

    @Test
    @DisplayName("returns 200 with IssueDetailDto for valid issueId")
    void returnsIssueDetailDtoForValidIssueId() throws Exception {
        final IssueDetail detail = new IssueDetail(
                42L,
                "Fix login bug",
                "SSO broken",
                OPENED,
                List.of("bug"),
                List.of(new User(10L, JOHN_DOE, "John Doe")),
                new Milestone(5L, 3L, "Sprint 12", "active", "2026-04-30"),
                java.time.OffsetDateTime.parse("2026-01-04T15:31:51.081Z"),
                java.time.OffsetDateTime.parse("2026-03-12T09:00:00.000Z"),
                null);
        final var enriched = new EnrichedIssueDetail(detail, List.of());
        final IssueDetailDto dto = new IssueDetailDto(
                42L,
                "Fix login bug",
                "SSO broken",
                OPENED,
                List.of("bug"),
                List.of(new UserDto(10L, JOHN_DOE, "John Doe")),
                new MilestoneDto(5L, 3L, "Sprint 12", "active", "2026-04-30"),
                java.time.OffsetDateTime.parse("2026-01-04T15:31:51.081Z"),
                java.time.OffsetDateTime.parse("2026-03-12T09:00:00.000Z"),
                null,
                List.of());
        when(issuesService.getIssueDetail(42L)).thenReturn(enriched);
        when(issuesResponseMapper.toIssueDetailDto(enriched)).thenReturn(dto);

        mockMvc.perform(get(GET_SINGLE_ENDPOINT, 42L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueId").value(42))
                .andExpect(jsonPath("$.title").value("Fix login bug"))
                .andExpect(jsonPath("$.state").value(OPENED))
                .andExpect(jsonPath("$.assignees[0].username").value(JOHN_DOE))
                .andExpect(jsonPath("$.milestone.title").value("Sprint 12"))
                .andExpect(jsonPath("$.changeSets").isArray());

        verify(issuesService).getIssueDetail(42L);
    }

    @Test
    @DisplayName("returns 400 validation error when issueId is zero")
    void returns400WhenIssueIdIsZero() throws Exception {
        mockMvc.perform(get(GET_SINGLE_ENDPOINT, 0L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details[0]").value(ISSUE_ID_POSITIVE_MESSAGE));
    }

    @Test
    @DisplayName("returns 400 validation error when issueId is negative")
    void returns400WhenIssueIdIsNegative() throws Exception {
        mockMvc.perform(get(GET_SINGLE_ENDPOINT, -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details[0]").value(ISSUE_ID_POSITIVE_MESSAGE));
    }

    @Test
    @DisplayName("returns 502 when service throws IntegrationException")
    void returns502WhenServiceThrowsIntegrationException() throws Exception {
        when(issuesService.getIssueDetail(42L))
                .thenThrow(new IntegrationException(
                        ErrorCode.INTEGRATION_FAILURE, "Integration error calling gitlab", "gitlab"));

        mockMvc.perform(get(GET_SINGLE_ENDPOINT, 42L))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath(CODE_PATH).value("INTEGRATION_FAILURE"));
    }
}
