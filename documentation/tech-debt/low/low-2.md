### 2 LOW — Logging Gap in `getIssues` Adapter Method

**What is happening now:**  
In [GitLabIssuesAdapter.java](../../flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/integration/gitlab/issues/GitLabIssuesAdapter.java), the `getIssues()` method does not log the intent before the call (unlike `createIssue`, `deleteIssue`, `getIssueDetail`, and `getLabelEvents` which all have a `log.info("Fetching/Creating/Deleting...")` before the API call). The outcome is logged inside `toIssuePage()` but the intent log is missing.

**Why it is a problem:**  
The Logging Gate requires adapter methods to: *"log before the external call (intent) and after (outcome)."* The `getIssues` adapter method breaks this pattern, making it harder to trace request flow for search calls.

**What to prefer:**  
Add a `log.info("Fetching GitLab issues page={} perPage={}", query.page(), query.perPage())` at the start of the `getIssues()` method body (before the `executeGitLabOperation` call or inside it before `fetchIssues`).

**Expected benefit:**  
Consistent adapter logging pattern across all five operations.

---
