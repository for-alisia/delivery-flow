### 1 LOW — `IssuesApiProperties` Hard-Locks Values That Should Be Constants or Truly Configurable

**What is happening now:**  
[IssuesApiProperties.java#L18](../../flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/config/IssuesApiProperties.java#L18) is declared as `@ConfigurationProperties(prefix = "app.issues-api")` but its compact constructor rejects any value that differs from the hardcoded contract values (20 and 40):

```java
public IssuesApiProperties {
    if (defaultPageSize != DEFAULT_PAGE_SIZE_CONTRACT) {
        throw new IllegalArgumentException("app.issues-api.default-page-size must be 20");
    }
    if (maxPageSize != MAX_PAGE_SIZE_CONTRACT) {
        throw new IllegalArgumentException("app.issues-api.max-page-size must be 40");
    }
}
```

Meanwhile, [IssuesRequestMapper.java#L20](../../flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/rest/mapper/IssuesRequestMapper.java#L20) independently declares `private static final int DEFAULT_PER_PAGE = 20` — a second source of truth for the default page size that is not connected to `IssuesApiProperties`.

**Why it is a problem:**  
- **Contradictory design**: The class is dressed as configuration (`@ConfigurationProperties`, `@Validated`, `@Min`/`@Max`) but functionally behaves as a constant holder. This is confusing for anyone who expects to override these values via `application.yml`.
- **Dual source of truth**: The default page size `20` exists in both `IssuesApiProperties` and `IssuesRequestMapper` with no programmatic link. Changing one without the other creates a silent inconsistency.
- **No environment-specific tuning**: For development, testing, or load-testing profiles, being able to adjust page sizes is valuable. The constructor guard prevents this.

**What to prefer:**  
Choose one of two clean approaches:

1. **Make them true externalized properties**: Remove the constructor guards, keep the `@Min`/`@Max` validation, and inject `IssuesApiProperties.defaultPageSize()` into `IssuesRequestMapper` to eliminate the duplicate constant.
2. **Replace with constants**: If the contract is intentionally fixed, replace `IssuesApiProperties` with a simple constants class (not `@ConfigurationProperties`) and reference it from `IssuesRequestMapper`.

**Expected benefit:**  
Single source of truth for page-size defaults. Either truly configurable for environment tuning, or honestly declared as constants.

---