Hi Team,

We are starting to work on our exciting project and we already came with our first task. As a reminder right now we have very basic skeleton of the application (you can find more information about what we build in `artifacts/project-overview.md`), but we don't have any user stories yet. So our first task is to create a user story for the first feature we want to implement, which is the "First Issues API".

The story is pretty easy from our users perspective, but they wanna know that we are moving into right direction. We want to present them that our application can return them issues from gitLab. We don't need any domain specific logic for now, just get request with payload (users prefer json payload over parameters) and return matching issues from gitlab. also users want to adjust how many items per request they want and pagination. Good news that gitLab api should support it already (team, please recheck it and confirm). They want to call API, provide payload when needed (should not be mandatory, use defaults if not provided) and get the list of their issues back. I think gitlab API which takes project id and returns list of issues should work for us (but also re-check it and confirm). As a part of this story you should set up the proper structure based on our constiturion and think about a lot of things like error handling, test set up according our strategy, documentation update and so on. I hope already existing documentation will be usefull and please keep in mind that we are building enterprise pjoject which we'll be used for years, and we will definitely add more and more fetatures over time, so please take your time to organise the structure which fits perfectly for our needs and is easy to maintain and extend in the future. Also to mention: link to our dev repo on gitLab you can find in application-local.yml file, so please check it out and make sure you have access to it. The same for PAT token, you can find it in application-local.yml file, but if you have any issues with access please let me know and I will help you to get it. In this repo we created couple of testing items for you - so you'll be able to verify that your implementation works as expected and also you can use it as a reference for your test set up. Also please keep in mind that we want to have good coverage for our code, so please make sure to cover all the important cases and logic with tests - both units an all levels of integration ones.

Also want to highlight how our client are going to use this SAAS: the team has it's own project on Gitlab where they keep their stories and epics. They want this url to be put under configuration (application-local.yml is ok and you can find the value there already), they should not use multiple different projects, so they do not need to swith it too often, so all our APIs (this on and in the future should use this url as baseline and our clients don't wnat to specify project each time they use our tool). 

Just to summarize: client wants to call our API at somthing like: POST /api/issues with payload:

```json
{
  "pagination": {
    "page": 1, // default if not provided
    "perPage": 40 // default if not povided
  },
  "filters": {
    "state": "opened",
    "labels": ["bug"], // only 1 label is accepted for now, validate and return error if provided more
    "assignee": ["john.doe"], // only 1 value is supported - look at labels for reference
    "milestone": ["Oran"] // only 1 value as above
  }
}
```

and we should return list of issues satisfying this payload. Payload is optional. If not provided return all issues.

```json 
{
"items": [
  {
    "id": 123,
    "title": "Issue title",
    "description": "Issue description",
    "state": "opened",
    "labels": ["bug"],
    "assignee": "john.doe",
    "milestone": "Oran",
    "parent": "id of parent epic if exists, null otherwise"
  },
  ...
],
"count": 10,
"page": 1,
}
```