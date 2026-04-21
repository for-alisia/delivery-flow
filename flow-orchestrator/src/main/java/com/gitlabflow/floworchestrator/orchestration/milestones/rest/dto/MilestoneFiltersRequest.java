package com.gitlabflow.floworchestrator.orchestration.milestones.rest.dto;

import com.gitlabflow.floworchestrator.common.util.ImmutableListCopies;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record MilestoneFiltersRequest(
        @Nullable @Pattern(regexp = "active|closed|all", message = "must be one of: active, closed, all")
        String state,

        @Nullable @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        String titleSearch,

        List<Long> milestoneIds) {

    public MilestoneFiltersRequest {
        milestoneIds = ImmutableListCopies.copyPreservingNullsOrEmpty(milestoneIds);
    }

    @Override
    public List<Long> milestoneIds() {
        return ImmutableListCopies.copyPreservingNullsOrEmpty(milestoneIds);
    }
}
