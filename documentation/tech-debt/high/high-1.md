
### 1 HIGH — 1+N Outbound Calls for `audit=label` Search

**What is happening now:**  
`POST /api/issues/search` with `audit=label` triggers 1 + N outbound GitLab calls: one list call in [IssuesService.java#L44](../../flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/IssuesService.java#L44), then one `getLabelEvents` per returned issue in [IssuesService.java#L118](../../flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/IssuesService.java#L118). With the current max page size of 40 ([IssuesApiProperties.java#L13](../../flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/config/IssuesApiProperties.java#L13)), a single client request can become 41 GitLab API calls.

```
POST /api/issues/search  (audit=label, perPage=40)
  → 1 × getIssues()
  → 40 × getLabelEvents(issueId)    ← parallel via AsyncComposer
  = 41 outbound HTTP requests
```

The `AsyncComposer` parallelizes the fan-out, which helps latency, but does not reduce the total call count.

**Why it is a problem:**  
- **Rate-limit risk**: GitLab's default rate limit is 300 requests/minute for authenticated users. A client paginating through 8 pages with audit=label generates 8 × 41 = 328 GitLab calls, potentially exhausting the budget in a single user session.
- **Latency amplification**: Even with parallelism, 40 concurrent outbound calls contend for connections and increase tail latency. A single slow GitLab response blocks the entire fan-out via `joinFailFast`.
- **Cascading failure risk**: Under load, the 1+N pattern multiplies downstream pressure. If multiple clients search concurrently, the GitLab call volume grows multiplicatively.
- **No throttling or circuit-breaking**: There is currently no rate-limit awareness, backpressure mechanism, or caching layer between the service and GitLab.

**What to prefer:**  
Two complementary strategies (not mutually exclusive):

1. **Move audit enrichment to the detail endpoint**: The `GET /api/issues/{issueId}` endpoint already fetches label events. Make `audit=label` on search a future opt-in feature with explicit documentation about the cost, or remove it from search entirely and let clients fetch label events per-issue on demand.
2. **If keeping audit on search**: Add a stricter page-size cap when audit is enabled (e.g., max 10), implement request-level rate-limit awareness (read GitLab's `RateLimit-Remaining` header and back off), and consider caching label events with a short TTL.

**Expected benefit:**  
Reduces worst-case GitLab call fan-out from 41 to 1 per search request; eliminates the rate-limit exhaustion risk; makes latency predictable.

