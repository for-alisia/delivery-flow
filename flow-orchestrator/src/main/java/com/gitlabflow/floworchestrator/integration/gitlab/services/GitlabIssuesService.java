package com.gitlabflow.floworchestrator.integration.gitlab.services;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.gitlabflow.floworchestrator.common.errors.ErrorCode;
import com.gitlabflow.floworchestrator.common.errors.IntegrationException;
import com.gitlabflow.floworchestrator.integration.gitlab.GitlabIssuesClient;
import com.gitlabflow.floworchestrator.integration.gitlab.GitlabProperties;
import com.gitlabflow.floworchestrator.integration.gitlab.models.dto.GitLabIssueResponseDTO;
import com.gitlabflow.floworchestrator.orchestration.issues.IssuesProvider;
import com.gitlabflow.floworchestrator.orchestration.issues.models.IssueSummary;
import com.gitlabflow.floworchestrator.orchestration.issues.models.GetIssuesRequest;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitlabIssuesService implements IssuesProvider {

    private final GitlabIssuesClient client;
    private final GitlabIssuesMapper mapper;
    private final GitlabProperties properties;

    @Override
    public List<IssueSummary> fetchIssues(GetIssuesRequest request) {
        Map<String, Object> queryParams = mapper.buildQueryParams(request);
        log.debug("Fetching issues from GitLab base {} with {} active filters", properties.getBaseUrl(), queryParams.size());

        try {
            List<GitLabIssueResponseDTO> response = client.getIssues(queryParams);
            List<IssueSummary> issues = response.stream().map(mapper::toIssueSummary).toList();
            log.info("Successfully retrieved {} issues from GitLab", issues.size());
            return issues;
        } catch (IntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("GitLab API unreachable: {}", ex.getMessage());
            throw new IntegrationException(
                    ErrorCode.INTEGRATION_UNAVAILABLE,
                    "gitlab",
                    null,
                    "GitLab is currently unreachable",
                    null,
                    ex
            );
        }
    }
}
