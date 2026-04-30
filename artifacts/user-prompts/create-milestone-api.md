Hi Team,

Today we will continue to work on Milestones capability - we already created an API which fetches milestones, today I want you to create API which will create new milestone in GitLab.

Our users should be able to call POST /api/milestones and provide the following payload:

```
{
  "title": "Milestone title",
  "description": "Milestone description",
  "dueDate": "2024-12-31",
  "startDate": "2024-01-01"
}
```

We have only 1 required filed: title, others are optional, but if provided - we should validate that dueDate and startDate are in correct format and that dueDate is after startDate.

Title should be more than 3 charactes long and less than 500 chars long.
Description - no validation needed for now (can be empty, null, missing).

In case of created - return 201 with created milestoneId and title in response body (our milestoneId - it's gitlabs milestone IID, not ID). In case of validation error - return 400 with error message. In case of gitlab error - follow already established error pattern.