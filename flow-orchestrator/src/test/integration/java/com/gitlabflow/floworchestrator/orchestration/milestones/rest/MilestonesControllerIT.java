package com.gitlabflow.floworchestrator.orchestration.milestones.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlabflow.floworchestrator.common.error.ErrorCode;
import com.gitlabflow.floworchestrator.common.error.IntegrationException;
import com.gitlabflow.floworchestrator.common.web.GlobalExceptionHandler;
import com.gitlabflow.floworchestrator.orchestration.milestones.MilestonesService;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.CreateMilestoneInput;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.Milestone;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.MilestoneState;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.SearchMilestonesInput;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.CreateMilestoneRequest;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.MilestoneFiltersRequest;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.SearchMilestonesRequest;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.mapper.MilestonesRequestMapper;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.mapper.MilestonesResponseMapper;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MilestonesController.class)
@Import({
    GlobalExceptionHandler.class,
    MilestonesRequestValidator.class,
    MilestonesRequestMapper.class,
    MilestonesResponseMapper.class
})
class MilestonesControllerIT {

    private static final String CREATE_ENDPOINT = "/api/milestones";
    private static final String SEARCH_ENDPOINT = "/api/milestones/search";
    private static final String CODE_PATH = "$.code";
    private static final String VALIDATION_CODE = "VALIDATION_ERROR";
    private static final @NonNull MediaType APPLICATION_JSON = Objects.requireNonNull(MediaType.APPLICATION_JSON);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MilestonesService milestonesService;

    @Test
    @DisplayName("creates milestone with title only and returns minimal 201 payload")
    void createsMilestoneWithTitleOnlyAndReturnsMinimal201Payload() throws Exception {
        when(milestonesService.createMilestone(any(CreateMilestoneInput.class)))
                .thenReturn(new Milestone(401L, 42L, "Release v1.0", null, null, null, "active"));

        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "title": "Release v1.0"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.milestoneId").value(42L))
                .andExpect(jsonPath("$.title").value("Release v1.0"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.startDate").doesNotExist())
                .andExpect(jsonPath("$.dueDate").doesNotExist())
                .andExpect(jsonPath("$.state").doesNotExist());

        final ArgumentCaptor<CreateMilestoneInput> inputCaptor = ArgumentCaptor.forClass(CreateMilestoneInput.class);
        verify(milestonesService).createMilestone(inputCaptor.capture());
        final CreateMilestoneInput input = inputCaptor.getValue();
        assertThat(input.title()).isEqualTo("Release v1.0");
        assertThat(input.description()).isNull();
        assertThat(input.startDate()).isNull();
        assertThat(input.dueDate()).isNull();
    }

    @Test
    @DisplayName("creates milestone with full payload and returns minimal 201 payload")
    void createsMilestoneWithFullPayloadAndReturnsMinimal201Payload() throws Exception {
        final CreateMilestoneRequest request = new CreateMilestoneRequest(
                "Q2 2026 Delivery", "Second quarter release cycle", "2026-06-30", "2026-04-01");
        when(milestonesService.createMilestone(any(CreateMilestoneInput.class)))
                .thenReturn(new Milestone(
                        402L,
                        43L,
                        "Q2 2026 Delivery",
                        "Second quarter release cycle",
                        "2026-04-01",
                        "2026-06-30",
                        "active"));

        mockMvc.perform(post(CREATE_ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.milestoneId").value(43L))
                .andExpect(jsonPath("$.title").value("Q2 2026 Delivery"))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.startDate").doesNotExist())
                .andExpect(jsonPath("$.dueDate").doesNotExist());

        final ArgumentCaptor<CreateMilestoneInput> inputCaptor = ArgumentCaptor.forClass(CreateMilestoneInput.class);
        verify(milestonesService).createMilestone(inputCaptor.capture());
        final CreateMilestoneInput input = inputCaptor.getValue();
        assertThat(input.title()).isEqualTo("Q2 2026 Delivery");
        assertThat(input.description()).isEqualTo("Second quarter release cycle");
        assertThat(input.startDate()).isEqualTo("2026-04-01");
        assertThat(input.dueDate()).isEqualTo("2026-06-30");
    }

    @Test
    @DisplayName("accepts missing body and applies ACTIVE defaults")
    void acceptsMissingBodyAndAppliesActiveDefaults() throws Exception {
        when(milestonesService.searchMilestones(any(SearchMilestonesInput.class)))
                .thenReturn(List.of());

        mockMvc.perform(post(SEARCH_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.milestones").isArray())
                .andExpect(jsonPath("$.milestones").isEmpty());

        final ArgumentCaptor<SearchMilestonesInput> inputCaptor = ArgumentCaptor.forClass(SearchMilestonesInput.class);
        verify(milestonesService).searchMilestones(inputCaptor.capture());
        final SearchMilestonesInput input = inputCaptor.getValue();
        assertThat(input.state()).isEqualTo(MilestoneState.ACTIVE);
        assertThat(input.titleSearch()).isNull();
        assertThat(input.milestoneIds()).isEmpty();
    }

    @Test
    @DisplayName("accepts valid filters and returns mapped milestone response")
    void acceptsValidFiltersAndReturnsMappedMilestoneResponse() throws Exception {
        final SearchMilestonesRequest request =
                new SearchMilestonesRequest(new MilestoneFiltersRequest("all", "  release  ", List.of(1L, 2L, 3L)));
        when(milestonesService.searchMilestones(any(SearchMilestonesInput.class)))
                .thenReturn(List.of(
                        new Milestone(12L, 3L, "Release 1.0", "Version", "2026-05-01", "2026-05-15", "active")));

        mockMvc.perform(post(SEARCH_ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.milestones[0].id").value(12))
                .andExpect(jsonPath("$.milestones[0].milestoneId").value(3))
                .andExpect(jsonPath("$.milestones[0].title").value("Release 1.0"))
                .andExpect(jsonPath("$.milestones[0].description").value("Version"))
                .andExpect(jsonPath("$.milestones[0].startDate").value("2026-05-01"))
                .andExpect(jsonPath("$.milestones[0].dueDate").value("2026-05-15"))
                .andExpect(jsonPath("$.milestones[0].state").value("active"));

        final ArgumentCaptor<SearchMilestonesInput> inputCaptor = ArgumentCaptor.forClass(SearchMilestonesInput.class);
        verify(milestonesService).searchMilestones(inputCaptor.capture());
        final SearchMilestonesInput input = inputCaptor.getValue();
        assertThat(input.state()).isEqualTo(MilestoneState.ALL);
        assertThat(input.titleSearch()).isEqualTo("release");
        assertThat(input.milestoneIds()).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("rejects invalid state")
    void rejectsInvalidState() throws Exception {
        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "filters": {
                                    "state": "opened"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details[0]", containsString("filters.state")))
                .andExpect(jsonPath("$.details[0]", containsString("must be one of: active, closed, all")));

        verify(milestonesService, never()).searchMilestones(any(SearchMilestonesInput.class));
    }

    @Test
    @DisplayName("rejects create request with missing title")
    void rejectsCreateRequestWithMissingTitle() throws Exception {
        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                                                                                                                {
                                                                                                                                        "description": "Second quarter release cycle"
                                                                                                                                }
                                                                                                                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details", hasItem("title must not be blank")));

        verify(milestonesService, never()).createMilestone(any(CreateMilestoneInput.class));
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
                .andExpect(jsonPath("$.details", hasItem("title must not be blank")));

        verify(milestonesService, never()).createMilestone(any(CreateMilestoneInput.class));
    }

    @Test
    @DisplayName("rejects create request with title length equal to three")
    void rejectsCreateRequestWithTitleLengthEqualToThree() throws Exception {
        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                                                                                                                {
                                                                                                                                        "title": "abc"
                                                                                                                                }
                                                                                                                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details", hasItem("title length must be between 4 and 499")));

        verify(milestonesService, never()).createMilestone(any(CreateMilestoneInput.class));
    }

    @Test
    @DisplayName("rejects create request with title length equal to five hundred")
    void rejectsCreateRequestWithTitleLengthEqualToFiveHundred() throws Exception {
        final CreateMilestoneRequest request = new CreateMilestoneRequest("x".repeat(500), null, null, null);

        mockMvc.perform(post(CREATE_ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details", hasItem("title length must be between 4 and 499")));

        verify(milestonesService, never()).createMilestone(any(CreateMilestoneInput.class));
    }

    @Test
    @DisplayName("rejects create request with invalid date format")
    void rejectsCreateRequestWithInvalidDateFormat() throws Exception {
        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                                                                                                                {
                                                                                                                                        "title": "Release v1.0",
                                                                                                                                        "startDate": "2026/04/01"
                                                                                                                                }
                                                                                                                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details[0]", containsString("startDate")));

        verify(milestonesService, never()).createMilestone(any(CreateMilestoneInput.class));
    }

    @Test
    @DisplayName("rejects create request when dueDate equals startDate")
    void rejectsCreateRequestWhenDueDateEqualsStartDate() throws Exception {
        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                                                                                                                {
                                                                                                                                        "title": "Q2 2026 Delivery",
                                                                                                                                        "startDate": "2026-06-30",
                                                                                                                                        "dueDate": "2026-06-30"
                                                                                                                                }
                                                                                                                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details", hasItem("dueDate must be after startDate")));

        verify(milestonesService, never()).createMilestone(any(CreateMilestoneInput.class));
    }

    @Test
    @DisplayName("rejects create request when dueDate is before startDate")
    void rejectsCreateRequestWhenDueDateIsBeforeStartDate() throws Exception {
        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                                                                                                                {
                                                                                                                                        "title": "Q2 2026 Delivery",
                                                                                                                                        "startDate": "2026-06-30",
                                                                                                                                        "dueDate": "2026-04-01"
                                                                                                                                }
                                                                                                                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details", hasItem("dueDate must be after startDate")));

        verify(milestonesService, never()).createMilestone(any(CreateMilestoneInput.class));
    }

    @Test
    @DisplayName("rejects blank titleSearch")
    void rejectsBlankTitleSearch() throws Exception {
        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "filters": {
                                    "titleSearch": "   "
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details[0]", containsString("filters.titleSearch")))
                .andExpect(jsonPath("$.details[0]", containsString("must not be blank")));

        verify(milestonesService, never()).searchMilestones(any(SearchMilestonesInput.class));
    }

    @Test
    @DisplayName("rejects duplicate milestone ids")
    void rejectsDuplicateMilestoneIds() throws Exception {
        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "filters": {
                                    "milestoneIds": [3, 3]
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details[0]").value("filters.milestoneIds must not contain duplicate values"));

        verify(milestonesService, never()).searchMilestones(any(SearchMilestonesInput.class));
    }

    @Test
    @DisplayName("rejects null milestone id elements")
    void rejectsNullMilestoneIdElements() throws Exception {
        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                                                                                                                {
                                                                                                                                        "filters": {
                                                                                                                                                "milestoneIds": [1, null, 3]
                                                                                                                                        }
                                                                                                                                }
                                                                                                                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details", hasItem("filters.milestoneIds[1] must not be null")));

        verify(milestonesService, never()).searchMilestones(any(SearchMilestonesInput.class));
    }

    @Test
    @DisplayName("rejects non positive milestone ids")
    void rejectsNonPositiveMilestoneIds() throws Exception {
        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                {
                                  "filters": {
                                    "milestoneIds": [0, -1]
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.details", hasItem("filters.milestoneIds[0] must be a positive integer")))
                .andExpect(jsonPath("$.details", hasItem("filters.milestoneIds[1] must be a positive integer")));

        verify(milestonesService, never()).searchMilestones(any(SearchMilestonesInput.class));
    }

    @Test
    @DisplayName("rejects malformed json body")
    void rejectsMalformedJsonBody() throws Exception {
        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details[0]").value("Malformed JSON request body"));

        verify(milestonesService, never()).searchMilestones(any(SearchMilestonesInput.class));
    }

    @Test
    @DisplayName("rejects malformed json body for create request")
    void rejectsMalformedJsonBodyForCreateRequest() throws Exception {
        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CODE_PATH).value(VALIDATION_CODE))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details[0]").value("Malformed JSON request body"));

        verify(milestonesService, never()).createMilestone(any(CreateMilestoneInput.class));
    }

    @Test
    @DisplayName("maps integration authentication failure to unauthorized")
    void mapsIntegrationAuthenticationFailureToUnauthorized() throws Exception {
        when(milestonesService.searchMilestones(any(SearchMilestonesInput.class)))
                .thenThrow(new IntegrationException(
                        ErrorCode.INTEGRATION_AUTHENTICATION_FAILED, "GitLab milestones operation failed", "gitlab"));

        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(CODE_PATH).value("INTEGRATION_AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("GitLab milestones operation failed"));
    }

    @Test
    @DisplayName("maps integration rate limited failure to too many requests")
    void mapsIntegrationRateLimitedFailureToTooManyRequests() throws Exception {
        when(milestonesService.searchMilestones(any(SearchMilestonesInput.class)))
                .thenThrow(new IntegrationException(
                        ErrorCode.INTEGRATION_RATE_LIMITED, "GitLab milestones operation failed", "gitlab"));

        mockMvc.perform(post(SEARCH_ENDPOINT).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath(CODE_PATH).value("INTEGRATION_RATE_LIMITED"))
                .andExpect(jsonPath("$.message").value("GitLab milestones operation failed"));
    }

    @Test
    @DisplayName("maps create integration authentication failure to unauthorized")
    void mapsCreateIntegrationAuthenticationFailureToUnauthorized() throws Exception {
        when(milestonesService.createMilestone(any(CreateMilestoneInput.class)))
                .thenThrow(new IntegrationException(
                        ErrorCode.INTEGRATION_AUTHENTICATION_FAILED,
                        "GitLab create milestone operation failed",
                        "gitlab"));

        mockMvc.perform(post(CREATE_ENDPOINT).contentType(APPLICATION_JSON).content("""
                                                                                                                                {
                                                                                                                                        "title": "Release v1.0"
                                                                                                                                }
                                                                                                                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(CODE_PATH).value("INTEGRATION_AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("GitLab create milestone operation failed"));
    }
}
