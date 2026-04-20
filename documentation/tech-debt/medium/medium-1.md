### 1 MEDIUM — Pagination Metadata Too Weak for a Production Search API

**What is happening now:**  
In [GitLabIssuesAdapter.java#L166](../../flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/integration/gitlab/issues/GitLabIssuesAdapter.java#L166), `toIssuePage` sets `count` to `issues.size()` — the number of items in the current page only:

```java
private IssuePage toIssuePage(final IssueQuery query, final List<IssueSummary> issues) {
    return IssuePage.builder()
            .items(issues)
            .count(issues.size())   // ← page-local count, not total
            .page(query.page())
            .build();
}
```

GitLab returns pagination headers (`X-Total`, `X-Total-Pages`, `X-Next-Page`, `X-Per-Page`) on list responses, but the adapter ignores them entirely. The `IssuePage` model has only `count` and `page` — no `totalCount`, `totalPages`, or `hasNext`.

The response DTO `SearchIssuesResponse` mirrors this: `(items, count, page)` with the same ambiguous `count` field.

**Why it is a problem:**  
- **`count` is misleading**: A consumer reading `count=20` cannot tell whether there are 20 total results or 20 on this page out of 500.
- **No "has next page" signal**: Clients must guess (request page N+1 and check if it's empty) or over-fetch to detect the end of results.
- **UI integration difficulty**: Any front-end building a paginated table needs total-count and total-pages to render page controls. Without them, clients must implement their own workarounds.
- **Constitution note**: Constitution Principle 4 states *"pagination mechanics must never appear outside the integration layer"* and Principle 5 states *"Domain models must not carry transport concerns: no pagination metadata."* However, the orchestration layer's `IssuePage` is an orchestration contract, not a domain model. It is the correct place for an abstracted pagination envelope — but the current envelope is incomplete.

**What to prefer:**  

1. **Read GitLab response headers** in the adapter. The `RestClient` response can be retrieved with `.toEntity()` instead of `.body()` to access headers.
2. **Extend `IssuePage`** with `totalCount` (from `X-Total`), `totalPages` (from `X-Total-Pages`), and `hasNext` (derived from `X-Next-Page` presence). These are orchestration-level concepts, not transport leaks.
3. **Rename `count`** to `pageSize` or `itemCount` to make it unambiguously page-local, or replace it with the total count.
4. **Mirror in the DTO**: Update `SearchIssuesResponse` to expose the new pagination fields.

**Expected benefit:**  
Clients can build reliable paginated UIs without guessing. The API becomes self-describing for pagination. Aligns with standard REST pagination practices.

---