package com.gitlabflow.floworchestrator.orchestration.milestones.rest;

import com.gitlabflow.floworchestrator.common.util.ElapsedTime;
import com.gitlabflow.floworchestrator.orchestration.milestones.MilestonesService;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.CreateMilestoneInput;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.SearchMilestonesInput;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.CreateMilestoneRequest;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.CreateMilestoneResponse;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.SearchMilestonesRequest;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.SearchMilestonesResponse;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.mapper.MilestonesRequestMapper;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.mapper.MilestonesResponseMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/milestones")
@RequiredArgsConstructor
public class MilestonesController {

    private final MilestonesService milestonesService;
    private final MilestonesRequestMapper milestonesRequestMapper;
    private final MilestonesResponseMapper milestonesResponseMapper;
    private final MilestonesRequestValidator milestonesRequestValidator;

    @PostMapping
    public ResponseEntity<CreateMilestoneResponse> createMilestone(
            @RequestBody @Valid final CreateMilestoneRequest request) {
        milestonesRequestValidator.validateCreateRequest(request);
        final String title = request.title();
        final int titleLength = title == null ? 0 : title.length();
        log.info(
                "Create milestone request received titleLength={} descriptionPresent={} startDate={} dueDate={}",
                titleLength,
                request.description() != null,
                request.startDate(),
                request.dueDate());

        final CreateMilestoneInput input = milestonesRequestMapper.toCreateMilestoneInput(request);
        final CreateMilestoneResponse response =
                milestonesResponseMapper.toCreateMilestoneResponse(milestonesService.createMilestone(input));

        log.info("Create milestone response returned milestoneId={}", response.milestoneId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/search")
    public SearchMilestonesResponse searchMilestones(
            @RequestBody(required = false) @Valid final SearchMilestonesRequest request) {
        final long startedAt = System.nanoTime();

        final SearchMilestonesInput input = milestonesRequestMapper.toSearchMilestonesInput(request);
        log.info(
                "Milestones search request received state={} titleSearch={} milestoneIdCount={}",
                input.state().value(),
                input.titleSearch(),
                input.milestoneIds().size());

        milestonesRequestValidator.validateSearchInput(input);

        final var milestones = milestonesService.searchMilestones(input);
        final SearchMilestonesResponse response = milestonesResponseMapper.toSearchMilestonesResponse(milestones);
        log.info(
                "Milestones search response returned resultCount={} durationMs={}",
                response.milestones().size(),
                ElapsedTime.toDurationMs(startedAt));
        return response;
    }
}
