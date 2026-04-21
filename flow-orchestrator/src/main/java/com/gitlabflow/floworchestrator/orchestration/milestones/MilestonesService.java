package com.gitlabflow.floworchestrator.orchestration.milestones;

import com.gitlabflow.floworchestrator.common.util.ElapsedTime;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.Milestone;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.SearchMilestonesInput;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilestonesService {

    private final MilestonesPort milestonesPort;

    public List<Milestone> searchMilestones(final SearchMilestonesInput input) {
        final long startedAt = System.nanoTime();
        log.info(
                "Searching milestones state={} titleSearch={} milestoneIdCount={}",
                input.state().value(),
                input.titleSearch(),
                input.milestoneIds().size());

        final List<Milestone> milestones = milestonesPort.searchMilestones(input);
        log.info(
                "Milestones search completed resultCount={} durationMs={}",
                milestones.size(),
                ElapsedTime.toDurationMs(startedAt));
        return milestones;
    }
}
