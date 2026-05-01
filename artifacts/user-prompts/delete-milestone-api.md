Hi Team,

Today we will continue to work on Milestones capability - we already created an API which fetches milestones, creates milestones and today I want you to create API which will delete existing milestone in GitLab.

Our users should be able to call DELETE /api/milestones/milestoneId, where milestoneId is gitlab iid of the milestones.

In case of success - return 200 with no payload, with error - throw according error, but I want you to properly translate GitLab error to our API - for example if milestone doesn't exist - provide explicit re-usable error like ENTITY_NOT_FOUND instead of too broad and unclear INTEGRATION_FAILED. Follow already established error pattern for other errors.

Also as a part of the previous task you left broken karate tests as Gitlab API doesn't allow to create milestones with not unique titles. Now you are doing DELETE API, so I want you to enhance your karate tests to create and delete milestone in order for it to be possible to re-run (no separate delete milestone test is needed, creation/deletion can be covered as a part of the same scenario)

Also I found it confusing that with not unique title we get INTEGRATION_FAILED error, so I want you to handle this case properly in your API and return clear error message that milestone with such title already exists (ENTITY_ALREADY_EXISTS - something like this). Please also add test for this scenario.