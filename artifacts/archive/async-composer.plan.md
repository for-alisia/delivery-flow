# Implementation Plan: Async Composer Shared Parallel Execution Foundation

**Artifact path:** `artifacts/implementation-plans/async-composer.plan.md`
**Task name:** `async-composer`
**Plan date:** `2026-04-09`
**Status:** `Revised`

## Business Goal

Extract a shared orchestration utility for fail-fast parallel composition, preserve the current issue-detail enrichment behavior, and enforce GitLab HTTP timeouts so future fan-out features reuse one provider-agnostic pattern instead of duplicating concurrency code.

## Requirement Lock / Source Of Truth

- Original request source: `artifacts/user-prompts/aasync-composer.md`
- Story: `artifacts/user-stories/async-composer.story.md`
- Non-negotiable source constraints:
  - `AsyncComposer` stays in `orchestration/common/async/`.
  - `AsyncComposer` has zero imports from `integration`, `config`, or capability packages and no provider-specific error types.
  - First failure cancels siblings and returns immediately without waiting for all tasks.
  - Existing `IssuesService.getIssueDetail` behavior and response shape are preserved.
  - GitLab REST calls gain connect/read timeouts; timeouts are the primary hung-request defense.
- Verified external references:
  - GitLab REST auth still supports the `PRIVATE-TOKEN` header.
  - GitLab project resources still expose `/projects/:id/issues` and `/projects/:id/issues/:issueId/resource_label_events`; no endpoint/path change is part of this feature.
- Assumptions:
  - `flow-orchestrator/pom.xml` is authoritative: Java 21 and Spring Boot 3.3.5 are available.
  - `StructuredTaskScope` is rejected because preview flags are not enabled in the build.
  - `application-local.yml` is user-managed and must not be modified.

## Payload Examples

No contract change is introduced; these examples document the preserved boundary shape.

```json
// Request
GET /api/issues/42
```

```json
// Success Response
{
  "issueId": 42,
  "title": "Fix login bug",
  "description": "SSO broken",
  "state": "opened",
  "labels": ["bug"],
  "assignees": [
    {
      "id": 10,
      "username": "john.doe",
      "name": "John Doe"
    }
  ],
  "milestone": {
    "milestoneId": 3,
    "title": "Sprint 12",
    "state": "active",
    "dueDate": "2026-04-30"
  },
  "createdAt": "2026-01-04T15:31:51.081Z",
  "updatedAt": "2026-03-12T09:00:00Z",
  "closedAt": null,
  "changeSets": []
}
```

```json
// Error Response
{
  "code": "INTEGRATION_FAILURE",
  "message": "GitLab get label events operation failed",
  "status": 502,
  "details": []
}
```

```json
// Validation Error Response
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "status": 400,
  "details": ["issueId must be a positive number"]
}
```

## Validation Boundary Decision

| Validation Rule | Boundary | Why Here |
|---|---|---|
| `app.gitlab.connect-timeout-seconds > 0` | Configuration binding | Startup fail-fast belongs in validated immutable properties per Constitution Principle 5. |
| `app.gitlab.read-timeout-seconds > 0` | Configuration binding | Same reason; invalid timeout config must fail before serving traffic. |
| `issueId` positivity | DTO binding | Existing REST validation already owns request-shape validation; no change. |
| `perPage <= maxPageSize` | Orchestration use case | Existing project-specific rule stays in `IssuesService`; unrelated to async extraction. |
| failure unwrapping and sibling cancellation | `orchestration.common.async` utility | This is cross-call composition behavior, not request validation and not adapter-specific error mapping. |

## Scope

### In Scope

- Choose the threading model and record the rationale in `AsyncComposer` class-level documentation.
- Introduce shared fail-fast composition support under `orchestration/common/async/`.
- Migrate `IssuesService.getIssueDetail` from raw common-pool `CompletableFuture` usage to `AsyncComposer`.
- Add connect/read timeout fields to GitLab configuration and wire them into the shared `RestClient`.
- Enforce the new `orchestration.common` boundary with ArchUnit.
- Add focused unit, integration, component, and architecture tests for the new behavior.

### Out of Scope

- Any REST contract or DTO shape change.
- New observability features beyond the existing issue-service and adapter logs.
- Backpressure, rate limiting, bulkhead, queue, or circuit-breaker mechanics.
- Any change to user-managed `application-local.yml`.

## Class Structure

### Model Definitions

| Model | Type | Fields / Contract | Notes |
|---|---|---|---|
| `com.gitlabflow.floworchestrator.config.GitLabProperties` | `record` | `String url` non-null, `String token` non-null, `int connectTimeoutSeconds` positive, `int readTimeoutSeconds` positive | Keep `@Builder`, `@Validated`, `@ConfigurationProperties(prefix = "app.gitlab")`; no collection fields, no defensive copy needed. |

No new interface, enum, or sealed hierarchy is introduced by this feature.

### Affected Classes

| Class Path | Status | Proposed Behavior |
|---|---|---|
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/config/AsyncExecutionConfig.java` | New | `@Configuration` exposing `@Bean(destroyMethod = "close") ExecutorService asyncComposerExecutor()` backed by `Executors.newVirtualThreadPerTaskExecutor()`. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/common/async/AsyncComposer.java` | New | `@Component` utility that depends on the shared virtual-thread `ExecutorService`. Public API is exactly `public <T> CompletableFuture<T> submit(Supplier<T> supplier)` and `public void joinFailFast(List<? extends CompletableFuture<?>> futures)`. `joinFailFast` returns normally only when every future completes successfully; it declares no checked `throws`, cancels unfinished siblings on first failure, and must unwrap `CompletionException` by following `getCause()` until the first non-`CompletionException` cause before applying the failure policy. After unwrapping, it rethrows the original `RuntimeException`, rethrows `Error`, and wraps checked causes or a missing root cause in `IllegalStateException`. Class javadoc records why virtual threads were chosen over a bounded pool and `StructuredTaskScope`. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/issues/IssuesService.java` | Change | Inject `AsyncComposer`, submit `getIssueDetail` and `getLabelEvents` through it, call `joinFailFast(List.of(issueDetailFuture, changeSetsFuture))` before reading results, and keep existing INFO logs and `EnrichedIssueDetail` output unchanged. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/config/GitLabProperties.java` | Change | Add validated timeout fields; preserve existing URL/token contract. |
| `flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/integration/gitlab/GitLabRestClientConfig.java` | Change | Build `HttpClient` with connect timeout, wrap it in `JdkClientHttpRequestFactory`, set read timeout, preserve base URL and `PRIVATE-TOKEN` header wiring. |
| `flow-orchestrator/src/main/resources/application.yml` | Change | Add non-secret defaults for `app.gitlab.connect-timeout-seconds` and `app.gitlab.read-timeout-seconds`. Use `5` and `30`. |
| `flow-orchestrator/src/test/java/com/gitlabflow/floworchestrator/orchestration/common/async/AsyncComposerTest.java` | New | Unit tests for all-success, first-failure cancellation, immediate return, runtime/checked/error unwrapping, and empty-input edge case. The cancellation test must use a cooperatively interruptible supplier that waits on a latch and polls `Thread.currentThread().isInterrupted()`; do not rely on `Thread.sleep()` alone. |
| `flow-orchestrator/src/test/java/com/gitlabflow/floworchestrator/orchestration/issues/IssuesServiceTest.java` | Change | Construct `IssuesService` with a real `AsyncComposer` backed by a test-owned virtual-thread `ExecutorService` created in `@BeforeEach` and closed in `@AfterEach`, then verify issue-detail behavior is unchanged after migration. |
| `flow-orchestrator/src/test/integration/java/com/gitlabflow/floworchestrator/integration/gitlab/GitLabRestClientConfigIT.java` | New | Spring-backed bean-inspection integration test proving timeout properties bind into the shared `RestClient` request factory. The test must not make any outbound network call. |
| `flow-orchestrator/src/test/java/com/gitlabflow/floworchestrator/integration/gitlab/issues/GitLabIssuesAdapterTest.java` | Change | Add a timeout-specific transport-failure case and preserve existing endpoint/mapping assertions. |
| `flow-orchestrator/src/test/component/java/com/gitlabflow/floworchestrator/issues/IssuesApiComponentTest.java` | Change | Add a delayed GitLab stub scenario proving read timeout terminates the call within a bounded duration and maps to the existing 502 error contract. |
| `flow-orchestrator/src/test/component/java/com/gitlabflow/floworchestrator/issues/support/GitLabLabelEventsStubSupport.java` | Change | Add a reusable delayed label-events stub helper for the component timeout scenario; keep existing request-path verification unchanged. |
| `flow-orchestrator/src/test/architecture/java/com/gitlabflow/floworchestrator/architecture/FlowOrchestratorArchitectureTest.java` | Change | Add boundary rule(s) ensuring `orchestration.common..` does not depend on capability packages and `AsyncComposer` does not depend on `integration..` or `config..`. |

## Composition Strategy

- Independent calls go parallel: `IssuesPort.getIssueDetail(issueId)` and `IssuesPort.getLabelEvents(issueId)` have no data dependency and must execute concurrently through `AsyncComposer`.
- Dependent calls remain sequential: each adapter method still wraps exactly one GitLab REST call and returns one orchestration contract; no adapter-side composition is introduced.

## Shared Infrastructure Impact

- Reused shared mechanisms: `GitLabRestClientConfig`, `GitLabProjectLocator`, `GitLabExceptionMapper`, existing `IssuesPort` orchestration boundary, existing global error contract.
- New shared extraction: `orchestration/common/async/AsyncComposer` plus `config/AsyncExecutionConfig` because this is the second fan-out use case and the mechanism is clearly reusable.
- No new capability documentation file is needed because this is shared infrastructure, not a new capability.

## Implementation Slices

### Slice 1 - Shared Async Foundation And Issue-Detail Migration

- Goal: replace ad-hoc issue-detail concurrency with a provider-agnostic shared utility, explicitly preserve `IntegrationException` propagation by unwrapping `CompletionException`, and enforce the new orchestration boundary.
- Affected scope: `AsyncExecutionConfig`, `AsyncComposer`, `IssuesService`, `FlowOrchestratorArchitectureTest`, `IssuesServiceTest`, `AsyncComposerTest`.
- Payload / contract impact: none; `GET /api/issues/{issueId}` output and error schema stay identical.
- Validation boundary decisions: no new HTTP validation; async failure handling stays in `AsyncComposer` and service output composition stays in `IssuesService`.
- Unit tests:
  - `AsyncComposerTest` all-success returns after both tasks complete and preserves task order at the call site.
  - `AsyncComposerTest` first failing task cancels a cooperatively interruptible slow sibling within bounded time and rethrows the original runtime exception.
  - `AsyncComposerTest` when a future fails as `new CompletionException(integrationException)`, `joinFailFast` throws that same `IntegrationException` instance, not `CompletionException` and not a generic `RuntimeException` assertion.
  - `AsyncComposerTest` checked exception becomes `IllegalStateException`; `Error` is rethrown.
  - `AsyncComposerTest` empty future list returns immediately without error.
  - `IssuesServiceTest` existing success and failure scenarios remain unchanged with real async execution by creating the virtual-thread executor in `@BeforeEach`, reusing it for `AsyncComposer`, and closing it in `@AfterEach`; the failing label-events or detail path must assert `assertThrowsExactly(IntegrationException.class, ...)` so the service contract cannot regress to `CompletionException`.
- Integration / Web tests: none in this slice.
- Edge / failure coverage: interruptible slow task cancellation via latch + interrupt polling, wrapped `CompletionException` unwrapped to the exact root cause, simultaneous sibling start, no-task edge case.
- INFO logging: keep existing `IssuesService` start/completion lines; `AsyncComposer` logs `None`.
- WARN logging: `None`.
- ERROR logging: `None`.
- Documentation updates: add rationale in `AsyncComposer` class javadoc; no `.http` change.

### Slice 2 - GitLab Timeout Wiring

- Goal: make GitLab REST calls fail within configured connect/read limits without changing endpoint contracts.
- Affected scope: `GitLabProperties`, `GitLabRestClientConfig`, `application.yml`, `GitLabRestClientConfigIT`, `GitLabIssuesAdapterTest`, `IssuesApiComponentTest`, `GitLabLabelEventsStubSupport`.
- Payload / contract impact: none; timeout failures continue to surface as the existing sanitized integration error response.
- Validation boundary decisions: timeout values validate at startup through `GitLabProperties`; adapter transport failures keep using existing `GitLabExceptionMapper` output.
- Unit tests:
  - `GitLabIssuesAdapterTest` timeout-like transport exception such as a `ResourceAccessException`/timeout cause maps to `ErrorCode.INTEGRATION_FAILURE`.
  - Existing adapter request URI and body tests stay green unchanged.
- Integration / Web tests:
  - `GitLabRestClientConfigIT` proves through bean inspection only that the shared `RestClient` bean uses `JdkClientHttpRequestFactory` with the configured connect/read timeout values; it must not call GitLab or any external URL.
- Component tests:
  - `IssuesApiComponentTest` uses a delayed WireMock label-events response with low `app.gitlab.read-timeout-seconds`, asserts `GET /api/issues/{issueId}` returns `502` with `INTEGRATION_FAILURE`, and completes before the stub delay fully elapses.
- Edge / failure coverage: zero/negative timeout config rejected at startup, delayed downstream response, unchanged success path when downstream is fast.
- INFO logging: `GitLabRestClientConfig` `None`; adapter INFO logs remain unchanged.
- WARN logging: no new WARN log; existing mapped HTTP warning remains unchanged.
- ERROR logging: existing adapter transport-failure error line remains the only ERROR path for timeout-triggered transport failures.
- Documentation updates: no `.http` or capability-doc change because no API contract, endpoint behavior, or capability inventory changes are introduced by this feature.

## Testing Matrix And Verification

| Level | Required | Planned Coverage | Evidence Target |
|---|---|---|---|
| Unit | Yes | `AsyncComposerTest`, `IssuesServiceTest`, `GitLabIssuesAdapterTest` | `scripts/verify-quick.sh` |
| Integration | Yes | `GitLabRestClientConfigIT` for Spring wiring of timeout settings | `scripts/verify-quick.sh` |
| Component | Yes | delayed GitLab timeout scenario in `IssuesApiComponentTest` | `scripts/verify-quick.sh` |
| Architecture | Yes | updated `FlowOrchestratorArchitectureTest` boundary rules | `scripts/verify-quick.sh` |
| Karate smoke | Yes | run existing issues smoke suite; no new feature file because no endpoint or contract changed | `scripts/karate-test.sh` |
| Static analysis / coverage | Yes | checkstyle, PMD, CPD, SpotBugs, JaCoCo | `scripts/final-check.sh` |

### ArchUnit checklist

- [x] New ArchUnit rule required because `orchestration.common` introduces a new package boundary not covered today.
- [x] Existing `FlowOrchestratorArchitectureTest.java` rules reviewed; the plan stays within current layer and naming constraints.

## Acceptance Criteria

- Virtual-thread decision is documented in `AsyncComposer` class documentation with explicit rejection of bounded-pool and preview structured-concurrency alternatives.
- `AsyncComposer` remains provider-agnostic and architecture tests fail on forbidden dependencies.
- `IssuesService.getIssueDetail` preserves current behavior while delegating concurrency to `AsyncComposer`, and integration failures still cross the orchestration boundary as `IntegrationException` rather than `CompletionException`.
- GitLab connect/read timeouts are validated, wired, and proven by automated tests.
- Repository verification workflow remains green with no new quality-gate violations.

## Final Verification Expectations

- Run from `flow-orchestrator/`: `scripts/verify-quick.sh`.
- Run from `flow-orchestrator/`: `scripts/final-check.sh` and record generated reports under `flow-orchestrator/target/`.
- Run from `flow-orchestrator/`: `scripts/karate-test.sh`; no new Karate artifact is expected because no endpoint changed.
- Mark any command that cannot run as `BLOCKED` with the exact command and observed error.