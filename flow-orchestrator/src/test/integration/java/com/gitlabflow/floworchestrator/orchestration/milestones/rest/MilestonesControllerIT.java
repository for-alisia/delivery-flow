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
import com.gitlabflow.floworchestrator.orchestration.milestones.model.Milestone;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.MilestoneState;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.SearchMilestonesInput;
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
}
