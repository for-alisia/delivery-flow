package com.gitlabflow.floworchestrator.orchestration.milestones.rest;

import com.gitlabflow.floworchestrator.common.error.ValidationException;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.SearchMilestonesInput;
import com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto.CreateMilestoneRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MilestonesRequestValidator {

    private static final String REQUEST_VALIDATION_FAILED = "Request validation failed";

    public void validateCreateRequest(final CreateMilestoneRequest request) {
        final List<String> details = new ArrayList<>();

        final LocalDate startDate = parseDate("startDate", request.startDate(), details);
        final LocalDate dueDate = parseDate("dueDate", request.dueDate(), details);
        validateDateOrder(startDate, dueDate, details);

        throwIfInvalid(details);
    }

    public void validateSearchInput(final SearchMilestonesInput input) {
        final List<String> details = new ArrayList<>();
        final List<Long> milestoneIds = input.milestoneIds();
        final Set<Long> seenMilestoneIds = new HashSet<>();
        boolean hasDuplicateMilestoneIds = false;

        for (int index = 0; index < milestoneIds.size(); index++) {
            final Long milestoneId = milestoneIds.get(index);
            if (milestoneId == null) {
                details.add("filters.milestoneIds[" + index + "] must not be null");
                continue;
            }
            if (milestoneId <= 0) {
                details.add("filters.milestoneIds[" + index + "] must be a positive integer");
                continue;
            }
            if (!seenMilestoneIds.add(milestoneId)) {
                hasDuplicateMilestoneIds = true;
            }
        }

        if (hasDuplicateMilestoneIds) {
            details.add("filters.milestoneIds must not contain duplicate values");
        }

        throwIfInvalid(details);
    }

    private LocalDate parseDate(final String fieldName, final String value, final List<String> details) {
        if (value == null) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (final DateTimeParseException exception) {
            details.add(fieldName + " must be a valid ISO date (YYYY-MM-DD)");
            return null;
        }
    }

    private void validateDateOrder(final LocalDate startDate, final LocalDate dueDate, final List<String> details) {
        if (startDate == null || dueDate == null) {
            return;
        }

        if (!dueDate.isAfter(startDate)) {
            details.add("dueDate must be after startDate");
        }
    }

    private void throwIfInvalid(final List<String> details) {
        if (!details.isEmpty()) {
            throw new ValidationException(REQUEST_VALIDATION_FAILED, details);
        }
    }
}
