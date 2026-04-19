# User Story: Shared Parallel Execution Foundation

**Feature name:** `async-composer`
**Story date:** `2026-04-09`

## Business Goal

Establish a reusable, reliable foundation for concurrent data fetching across all orchestration features. This eliminates duplication of parallel execution logic and ensures consistent, fail-fast behavior in all parallel operations — critical as the product scales to handle search-audit enrichment and future multi-resource orchestration scenarios.

## Problem Statement

The current issue-detail enrichment fetches issue data and label-event history in parallel using ad-hoc concurrent patterns. As additional features require parallel fetching (e.g., search-audit with up to 60 concurrent label-event calls), duplicating this logic creates maintenance risk, inconsistent timeout behavior, and missed opportunities for centralized observability and reliability improvements.

## User Story

As an architect and development team,
I want a shared, reusable parallel execution utility that can safely coordinate multiple concurrent GitLab API calls,
so that all features have consistent fail-fast semantics, clear exception handling, and a stable foundation for scaling concurrent operations without reimplementing the same patterns.

## Locked Request Constraints

These are the non-negotiable constraints from Team Lead.

- The utility (`AsyncComposer`) must remain fully provider-agnostic — zero references to provider names, integration classes, or provider-specific error types.
- Exception handling must support fail-fast behavior: when one call fails, remaining concurrent calls are canceled immediately, and the failure is returned without waiting for all tasks to complete.
- Configuration must include HTTP client timeouts (connect and read) wired into all GitLab REST calls; HTTP timeouts are the primary defense against hung requests.
- The existing issue-detail fetch behavior (parallel issue + label-event calls, fail-fast on first failure, sibling cancellation) must be preserved and all existing tests must pass after migration.
- The utility must live in `orchestration/common/async/` — never in integration or provider-specific packages.
- Architecture boundary isolation rule: `orchestration.common` must not depend on any capability-specific packages (`orchestration.issues`, `orchestration.projects`, etc.).

## Business Context and Constraints

- **Stakeholders**: Development team, architects, and future feature teams building parallel operations.
- **Business terminology**: "Parallel composition" means coordinating multiple concurrent API calls with coordinated timeout and failure semantics. "Fail-fast" means returning immediately on first failure instead of waiting for all tasks.
- **Architectural principle alignment**: This work enforces shared infrastructure reuse (extraction principle: 2nd instance = extract now) and ensures layer boundaries remain clean and provider-agnostic.
- **Threading strategy**: The architect must evaluate threading options (virtual threads, bounded thread pool, or structured concurrency) and document the decision rationale before implementation begins.

## Scope

### In Scope

- Architectural decision on threading model: virtual threads (Option A — recommended), bounded thread pool (Option B), or structured task scope (Option C), with documented rationale.
- Design and implementation of `AsyncComposer` utility with fail-fast parallel join and consistent exception unwrapping.
- HTTP client timeout configuration (connect and read timeouts) integrated with all GitLab REST client calls.
- Migration of existing `IssuesService.getIssueDetail` to use `AsyncComposer` instead of raw concurrent calls.
- Executor bean configuration (if the chosen threading model requires it), with startup validation per Constitution Principle 5.
- ArchUnit rule enforcement: `orchestration.common` must not depend on any capability packages.
- Full test coverage: unit tests for `AsyncComposer` fail-fast behavior, exception unwrapping, and cancellation; integration tests for timeout wiring; ArchUnit boundary tests.

### Out of Scope

- Changes to domain models or business logic outside the async composition pattern.
- Auditing or logging of failed parallel operations (that belongs to per-feature observability patterns, not the shared utility).
- Generic queue, backpressure, or circuit-breaker mechanics beyond what is necessary for fail-fast parallel join (those are future orthogonal concerns).
- Provider rotation or multi-provider failover (that is a future orchestration pattern, not part of this foundation).

## Acceptance Criteria

1. **Threading decision recorded:** Architect has evaluated and documented the chosen threading model (virtual threads, bounded pool, or structured scope) with explicit rationale, stored in `AsyncComposer` class documentation or a linked ADR.

2. **AsyncComposer isolation verified:** Static analysis (ArchUnit test) confirms `AsyncComposer` has zero imports from `integration`, `config`, or any capability package; all provider-specific input comes from callers as POJOs.

3. **getIssueDetail behavior preserved:** Refactored `getIssueDetail` uses `AsyncComposer` for parallel fetch, all existing tests pass unchanged, and the observable behavior is identical to the pre-refactoring implementation (same parallel semantics, same exceptions, same return shape).

4. **Fail-fast semantics verified:** Unit test demonstrates that when one concurrent task fails, remaining tasks are canceled within a bounded time and the failure is returned immediately (not after waiting for other tasks to complete).

5. **HTTP timeouts enforced:** `GitLabRestClient` is configured with connect and read timeouts via `GitLabProperties`; integration test verifies a simulated timeout adheres to the configured limits (e.g., request that takes longer than read timeout is terminated and raises an exception).

6. **Boundary isolation enforced:** ArchUnit test confirms `orchestration.common` has no dependencies on capability packages. Test must fail if any such dependency is later introduced.

7. **Quality gates pass:** `scripts/verify-quick.sh` (compile + unit/component tests) and `scripts/final-check.sh` (full build + coverage + code analysis) run successfully with no new violations.

## Dependencies and Assumptions

- **External dependency:** GitLab REST API is available at configured endpoints; timeout handling assumes GitLab HTTP server respects socket-level timeouts.
- **Internal dependency:** Existing `IssuesService.getIssueDetail` implementation is stable and test-covered; migration assumes all parallel-fetch logic can be delegated to `AsyncComposer` without domain-logic changes.
- **Assumption:** The chosen threading model (virtual threads, bounded pool, or structured scope) will be available in the project's Java 21 + Spring Boot 3.3.5 runtime.
- **Assumption:** Architecture team confirms that this shared utility does not conflict with any planned monitoring or observability framework.

## Open Questions

- **Resolved by Architect:** Which threading model (virtual threads, bounded pool, or structured scope) is the right choice for this project, and what is the justification?
- **Resolved by Architect:** If a bounded thread pool is chosen, what are the recommended default values for core and max pool size, and how should they be tuned for the expected concurrent load?
