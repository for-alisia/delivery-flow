package com.gitlabflow.floworchestrator.orchestration.milestones.rest;

import com.gitlabflow.floworchestrator.common.error.ValidationException;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.SearchMilestonesInput;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MilestonesRequestValidator {

    private static final String REQUEST_VALIDATION_FAILED = "Request validation failed";

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

        if (!details.isEmpty()) {
            throw new ValidationException(REQUEST_VALIDATION_FAILED, details);
        }
    }
}
