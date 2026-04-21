package com.gitlabflow.floworchestrator.orchestration.milestones;

import com.gitlabflow.floworchestrator.orchestration.milestones.model.Milestone;
import com.gitlabflow.floworchestrator.orchestration.milestones.model.SearchMilestonesInput;
import java.util.List;

public interface MilestonesPort {

    List<Milestone> searchMilestones(SearchMilestonesInput input);
}
