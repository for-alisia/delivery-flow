### 4 LOW — `IssueState` and `IssueAuditType` Lack Dedicated Unit Tests

**What is happening now:**  
Both [IssueState.java](../../flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/model/IssueState.java) and [IssueAuditType.java](../../flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/model/IssueAuditType.java) have `fromValue()` methods with validation and error paths. Neither has direct unit tests. Coverage comes indirectly through `IssuesRequestMapper` usage in higher-level tests, but the error branches (unsupported values, null input) are not explicitly verified at the unit level.

**Why it is a problem:**  
Same reasoning as 4.8. These enums define the accepted wire-format vocabulary for filters and audit types. Regression in parsing would be caught late.

**What to prefer:**  
Add `IssueStateTest` and `IssueAuditTypeTest` covering valid values, case-insensitive matching, and unsupported-value error paths.

**Expected benefit:**  
Direct coverage of filter-parsing contracts; fast regression detection.

---