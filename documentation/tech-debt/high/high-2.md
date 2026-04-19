### 2 HIGH — `executeGitLabOperation` Catches All `RuntimeException` and Drops the Original Cause

**What is happening now:**  
In [GitLabIssuesAdapter.java#L204](../../flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/integration/gitlab/issues/GitLabIssuesAdapter.java#L204), `executeGitLabOperation` has a broad `catch (RuntimeException)` block:

```java
try {
    return operation.get();
} catch (final RestClientResponseException exception) {
    throw mapHttpFailure(exception, resource);
} catch (final RuntimeException exception) {
    throw mapTransportFailure(exception.getClass().getSimpleName(), resource);
}
```

The `mapTransportFailure` method ([line 209](../../flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/integration/gitlab/issues/GitLabIssuesAdapter.java#L209)) only passes the exception class name as a string — not the exception itself. The resulting `IntegrationException` ([IntegrationException.java#L8](../../flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/common/error/IntegrationException.java#L8)) is created via `super(message)` without a `cause` parameter, so the original stack trace is permanently lost. The log line also omits the exception object:

```java
log.error("GitLab transport failure resource={} category={}", resource, category);
// ← no exception passed as last argument → no stack trace in logs
```

**Why it is a problem:**  
- **Mapper bugs become invisible**: If `gitLabIssuesMapper.toIssue()` throws an NPE due to an unexpected null field from GitLab, it is caught as a `RuntimeException`, logged only as `category=NullPointerException`, and surfaced as a generic `INTEGRATION_FAILURE` (502). The actual field and stack trace are lost.
- **Provider contract drift is undetectable**: If GitLab changes a response shape and a mapper fails, the same generic error appears. There is no way to distinguish a true transport failure from an internal mapping bug without adding debug-level logging and reproducing the issue.
- **Violates exception chain best practice**: `IntegrationException` has no `cause` constructor. Even if the catch block wanted to preserve the cause, it cannot.
- **Constitution alignment**: Principle 7 requires *"Exceptions crossing a layer boundary must be typed and carry a semantic error category."* The current catch-all conflates two fundamentally different failure types (transport failure vs. internal bug) under the same `INTEGRATION_FAILURE` category.

**What to prefer:**  

1. **Add a cause-accepting constructor** to `IntegrationException`:
   ```java
   public IntegrationException(ErrorCode errorCode, String message, String source, Throwable cause) {
       super(message, cause);
       this.errorCode = errorCode;
       this.source = source;
   }
   ```

2. **Narrow the catch in `executeGitLabOperation`**: Catch only known transport exceptions (`RestClientException`, `ResourceAccessException`) as `INTEGRATION_FAILURE`. Let unexpected `RuntimeException` propagate naturally — the `GlobalExceptionHandler.handleUnhandledException` already catches `Exception.class` and maps it to `INTERNAL_ERROR` (500) with a full stack trace log.

3. **Always pass the exception to `log.error`**:
   ```java
   log.error("GitLab transport failure resource={}", resource, exception);
   ```

**Expected benefit:**  
Mapper bugs surface as 500 (internal error) with full stack traces instead of masquerading as 502 (integration failure). Transport failures preserve the cause chain for diagnostics. Debugging time for production incidents drops significantly.

---