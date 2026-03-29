package com.gitlabflow.floworchestrator.orchestration.issues;

import org.springframework.stereotype.Service;

import com.gitlabflow.floworchestrator.orchestration.issues.models.ListProjectIssuesQuery;
import com.gitlabflow.floworchestrator.orchestration.issues.models.ListProjectIssuesResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListProjectIssuesUseCase {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final IssuesProvider issuesProvider;
    private final ListProjectIssuesRequestValidator requestValidator;

    public ListProjectIssuesResult listProjectIssues(String projectId, Integer page, Integer pageSize) {
        final int resolvedPage = page == null ? DEFAULT_PAGE : page;
        final int resolvedPageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        final ListProjectIssuesQuery query = new ListProjectIssuesQuery(projectId, resolvedPage, resolvedPageSize);

        requestValidator.validate(query);
        return issuesProvider.listProjectIssues(query);
    }
}
