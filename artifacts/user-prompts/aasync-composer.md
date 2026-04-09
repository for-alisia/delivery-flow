## Feature: Shared Async Fan-Out Infrastructure

### Goal

Extract a shared, provider-agnostic parallel execution utility (`AsyncComposer`) from the
existing `IssuesService.getIssueDetail` fan-out pattern. This utility will serve as the
reusable foundation for all current and future parallel blocking-I/O composition in the
orchestration layer — starting with issue-detail enrichment and upcoming search-audit
enrichment (up to 60 concurrent label-event calls).

### Problem Statement

The current `getIssueDetail` uses raw `CompletableFuture.supplyAsync()` on the JVM common
`ForkJoinPool`. A second fan-out path is needed for search-audit enrichment, triggering the
architecture-guidance shared infrastructure extraction rule (2nd instance = extract now).

The utility must handle: task submission, fail-fast join with sibling cancellation, exception
unwrapping, and must remain fully provider-agnostic (Constitution Principles 2 and 13 — no
provider identity in shared orchestration code).

### Architectural Decision: Threading Model

Before designing the API surface, the architect must evaluate and decide between these
Java 21 + Spring Boot 3.3.5 threading strategies. The decision must be justified and recorded.

**Option A — Virtual Threads (recommended to evaluate first):**
- Java 21 virtual threads via `Executors.newVirtualThreadPerTaskExecutor()` or
  Spring Boot's `spring.threads.virtual.enabled: true`
- Virtual threads are lightweight (~1KB), designed for blocking I/O, no bounded pool needed
- Eliminates: pool sizing, queue capacity, rejection policy, `RejectedExecutionException`
  translation, partial-submission cleanup — none of these problems exist with virtual threads
- AsyncComposer becomes a thin utility with only `joinAllFailFast` (fail-fast sentinel
  pattern) and `unwrapCompletionFailure` — no submission wrapper needed
- HTTP client timeouts (already planned via `GitLabProperties` + `JdkClientHttpRequestFactory`)
  remain the primary timeout mechanism — virtual threads block naturally on I/O and respect
  socket timeouts
- Spring Boot 3.2+ auto-configures `SimpleAsyncTaskExecutor` with virtual threads when enabled
- Concurrency control, if needed later, can use a `Semaphore` (simpler than pool tuning)

**Option B — Bounded `ThreadPoolTaskExecutor`:**
- Custom `GitLabTaskExecutorConfig` with `corePoolSize`, `maxPoolSize`, `queueCapacity`
- Requires solving: `RejectedExecutionException` translation (provider-agnostic),
  partial-submission sibling cancellation, fail-fast join, pool sizing defaults, startup
  validation of pool config
- More moving parts, but provides explicit backpressure via queue + rejection policy

**Option C — `StructuredTaskScope` (JEP 453, preview in Java 21):**
- `ShutdownOnFailure` scope handles fail-fast + automatic sibling cancellation natively
- Cleanest API for fan-out-then-join, but requires `--enable-preview` compiler/runtime flag
- The architect must decide if preview features are acceptable for this project

**Decision criteria:** simplicity, number of custom components, testability, alignment with
blocking-I/O-heavy workload, Constitution Principle 13 (expansion readiness).

### Scope

The following must be delivered as part of this story:

1. **Threading model decision** — documented with rationale
2. **`AsyncComposer`** in `orchestration/common/async/` — provider-agnostic utility with:
   - Fail-fast parallel join (first failure cancels siblings, returns immediately)
   - Exception unwrapping (`CompletionException` → cause; `RuntimeException` passthrough;
     `Error` rethrow; checked → `IllegalStateException`)
   - API surface appropriate to the chosen threading model
   - Zero references to any provider name, integration class, or `ErrorCode`
3. **Executor bean** (if Option B chosen) — `@ConfigurationProperties`-validated config with
   startup fail-fast per Constitution Principle 5
4. **HTTP client timeouts** — `GitLabProperties` with `connectTimeoutSeconds` and
   `readTimeoutSeconds`, wired into `GitLabRestClientConfig` via `JdkClientHttpRequestFactory`
5. **Migration of `IssuesService.getIssueDetail`** — refactored to use `AsyncComposer`
   instead of raw `CompletableFuture` calls
6. **ArchUnit rule** — `orchestration.common..` must not depend on any capability package
   (`orchestration.issues..`, `orchestration.projects..`, etc.), not just `issues`
7. **Tests** — unit tests for `AsyncComposer` (fail-fast behavior, cancellation, exception
   unwrapping), integration test for timeout wiring, ArchUnit boundary test

### Constraints

- `AsyncComposer` must live in `orchestration/common/async/` — never in `integration/`
- No provider name, `ErrorCode`, or `IntegrationException` inside `AsyncComposer` — callers
  are responsible for translating infrastructure exceptions to domain exceptions if needed
- The existing `IssuesService.getIssueDetail` behavior must be preserved (parallel fetch of
  issue detail + label events, fail-fast on first failure, cancel sibling)
- HTTP client timeouts are the primary defense against hung calls — the threading model
  provides concurrency, not timeout enforcement
- Constitution Principle 5: all configuration records must be `@Validated` with fail-fast
  startup behavior
- Constitution Principle 7: exceptions crossing layer boundaries must be typed — but that
  typing happens at the CALLER level, not inside the shared utility

### Acceptance Criteria

1. `AsyncComposer` has zero imports from `integration`, `config`, or any capability package
2. `getIssueDetail` uses `AsyncComposer` and passes all existing tests
3. First-failure in a parallel batch cancels remaining tasks and returns within a bounded time
   (not waiting for all tasks to complete)
4. `GitLabRestClient` has configured connect and read timeouts
5. ArchUnit test enforces `orchestration.common` → capability-package isolation
6. All quality gates pass (`scripts/verify-quick.sh`, `scripts/final-check.sh`)