# Implementation Plan: Delete Issue API

**Artifact path:** `artifacts/implementation-plans/delete-issue-api.plan.md`
**Task name:** `delete-issue-api`
**Plan date:** `2026-04-06`
**Status:** `Draft`

## Business Goal

Allow clients to remove invalid or obsolete work items by project-scoped issue number via a single DELETE endpoint, forwarding the request to GitLab and returning correct HTTP status codes for success, not-found, and permission-denied outcomes.

## Requirement Lock / Source Of Truth

- Original request source: `artifacts/user-prompts/feature-4-delete-issue-api.md`
- Non-negotiable input constraint: `issueId` path parameter is the project-scoped issue number (GitLab `iid`), not an internal key.
- Non-negotiable contract constraint: Success returns HTTP 204 No Content with no response body.
- Non-negotiable external-system constraint: Forward to GitLab `DELETE /projects/:id/issues/:issue_iid` using URL-encoded project path as `:id`.
- Non-negotiable error mapping: GitLab 404 → our 404; GitLab 403 → our 403. Both use the standard `ErrorResponse` format with no GitLab internals exposed.
- MVP scope constraint: No domain logic — pure pass-through.

## Payload Examples

```
// Request
DELETE /api/issues/42
(no body)

// Success Response
HTTP 204 No Content
(no body)

// Error Response — not found
HTTP 404
{ "code": "INTEGRATION_NOT_FOUND", "message": "GitLab delete issue operation failed", "details": [] }

// Error Response — forbidden
HTTP 403
{ "code": "INTEGRATION_FORBIDDEN", "message": "GitLab delete issue operation failed", "details": [] }

// Validation Error Response — invalid issueId
HTTP 400
{ "code": "VALIDATION_ERROR", "message": "Request validation failed", "details": ["issueId must be a positive number"] }
```

## Validation Boundary Decision

| Validation Rule | Boundary | Why Here |
|---|---|---|
| `issueId > 0` | Controller (DTO binding) | System boundary — reject bad input before it reaches the service or integration layer |
| `issueId is numeric` | Spring path-variable type coercion | Non-numeric values fail type conversion; handled by `MethodArgumentTypeMismatchException` handler |

## Scope

### In Scope

- `DELETE /api/issues/{issueId}` endpoint returning 204 on success.
- Error-code-aware HTTP status mapping in `GlobalExceptionHandler` (404, 403, 401, 429, 502) — required for this feature's AC 2 and AC 3, and corrects existing behavior system-wide.
- `INTEGRATION_FORBIDDEN` error code and 403 mapping in `GitLabExceptionMapper`.
- `MethodArgumentTypeMismatchException` handler for path-variable type errors (returns 400 instead of 500).
- Smoke-test and `.http` file updates.

### Out of Scope

- Soft-delete, bulk delete, domain precondition checks, audit logging, rollback.
- Any new orchestration models — a primitive `long issueId` is sufficient for a single-parameter pass-through with no domain logic.

## Class Structure

### Affected Classes

| Class (under `com.gitlabflow.floworchestrator`) | Status | Proposed Behavior |
|---|---|---|
| `common/error/ErrorCode` | Modified | Add `INTEGRATION_FORBIDDEN` enum constant |
| `integration/gitlab/GitLabExceptionMapper` | Modified | Map HTTP 403 → `INTEGRATION_FORBIDDEN` |
| `common/web/GlobalExceptionHandler` | Modified | Map `IntegrationException.errorCode()` to HTTP status (NOT_FOUND→404, FORBIDDEN→403, AUTH→401, RATE→429, else→502). Add `MethodArgumentTypeMismatchException` → 400 handler. |
| `orchestration/issues/IssuesPort` | Modified | Add `void deleteIssue(long issueId)` |
| `orchestration/issues/IssuesService` | Modified | Add `void deleteIssue(long issueId)` — delegates to port, logs at INFO |
| `orchestration/issues/rest/IssuesController` | Modified | Add `@DeleteMapping("/{issueId}")` — validates `issueId > 0`, delegates, returns `ResponseEntity.noContent()` |
| `integration/gitlab/issues/GitLabIssuesAdapter` | Modified | Implement `deleteIssue` — `restClient.delete()` to `/projects/{projectPath}/issues/{issueId}`, same error handling pattern as `createIssue` |

## Implementation Slices

### Slice 1 — Error-code-aware HTTP status mapping

- **Goal:** Make `GlobalExceptionHandler` return semantically correct HTTP statuses based on `ErrorCode`, add `INTEGRATION_FORBIDDEN`, and handle path-variable type errors. This is required infrastructure for the delete endpoint's 404/403 contract.
- **Affected scope:** `ErrorCode`, `GitLabExceptionMapper`, `GlobalExceptionHandler`
- **Payload / contract impact:** All `IntegrationException` responses change from flat 502 to error-code-aware statuses (`INTEGRATION_NOT_FOUND`→404, `INTEGRATION_FORBIDDEN`→403, `INTEGRATION_AUTHENTICATION_FAILED`→401, `INTEGRATION_RATE_LIMITED`→429, default→502). `MethodArgumentTypeMismatchException` → 400 with `VALIDATION_ERROR` code.
- **Validation boundary decisions:** N/A (infrastructure slice)
- **Unit tests:**
  - `GitLabExceptionMapperTest` — add test: 403 → `INTEGRATION_FORBIDDEN`
  - `GlobalExceptionHandlerTest` — **update** existing `returnsBadGatewayForIntegrationException` to verify `INTEGRATION_FAILURE` still returns 502. **Add** tests: `INTEGRATION_NOT_FOUND`→404, `INTEGRATION_FORBIDDEN`→403, `INTEGRATION_AUTHENTICATION_FAILED`→401, `INTEGRATION_RATE_LIMITED`→429, `MethodArgumentTypeMismatchException`→400
- **Integration / Web tests:**
  - `IssuesControllerIT` — **update** `mapsIntegrationExceptionsToBadGateway` (RATE_LIMITED) to expect 429. **Update** `mapsCreateIntegrationExceptionsToBadGateway` (AUTH_FAILED) to expect 401. Rename test methods to match new behavior.
- **Edge / failure coverage:** All five error codes are covered by dedicated unit tests. Type-mismatch edge case covered.
- **INFO logging:** None
- **WARN logging:** Existing `GlobalExceptionHandler` warn log unchanged. Add warn-level log for `MethodArgumentTypeMismatchException` including the parameter name.
- **ERROR logging:** None
- **Documentation updates:** None

### Slice 2 — Delete issue endpoint (pass-through)

- **Goal:** Wire `DELETE /api/issues/{issueId}` from controller through service and port to GitLab adapter.
- **Affected scope:** `IssuesPort`, `IssuesService`, `IssuesController`, `GitLabIssuesAdapter`
- **Payload / contract impact:** New endpoint `DELETE /api/issues/{issueId}` → 204 (success), 404 (not found), 403 (forbidden), 400 (invalid issueId).
- **Validation boundary decisions:** `issueId > 0` check in controller using manual check + `ValidationException`.
- **Unit tests:**
  - `IssuesServiceTest` — add test: `deleteIssue` delegates to port (verify interaction)
  - `GitLabIssuesAdapterTest` — add tests: successful delete (204 from GitLab, no exception), GitLab 404 → `IntegrationException(NOT_FOUND)`, GitLab 403 → `IntegrationException(FORBIDDEN)`, transport failure → `IntegrationException(INTEGRATION_FAILURE)`
  - `IssuesController` unit test (if exists) or inline in IT — add test: `issueId ≤ 0` → `ValidationException`
- **Integration / Web tests:**
  - `IssuesControllerIT` — add tests: successful delete returns 204 with no body, invalid `issueId` (0) returns 400, service throws `IntegrationException(NOT_FOUND)` returns 404, service throws `IntegrationException(FORBIDDEN)` returns 403
- **Edge / failure coverage:** `issueId = 0`, `issueId = -1` (validation), non-numeric issueId (type mismatch from Slice 1), GitLab 403, GitLab 404, transport failure.
- **INFO logging:** Controller: `"Delete issue request received issueId={}"`. Service: `"Deleting issue issueId={}"` and `"Issue deleted issueId={}"`. Adapter: `"GitLab issue deleted issueId={}"`.
- **WARN logging:** Adapter: reuse existing `mapHttpFailure` pattern (logs resource, status, category).
- **ERROR logging:** Adapter: reuse existing `mapTransportFailure` pattern.
- **Documentation updates:**
  - `flow-orchestrator/http/issues.http` — add delete happy-path request, delete non-existent issue request.
  - `scripts/smoke-test.sh` — add: create an issue, extract `issueId` from response, `DELETE /api/issues/{issueId}` expect 204, `DELETE /api/issues/{issueId}` again expect 404, `DELETE /api/issues/0` expect 400.

## Testing Matrix

| Level | Required | Planned Coverage | Evidence Target |
|---|---|---|---|
| Unit (`*Test`) | Yes | ErrorCode, GitLabExceptionMapper (403), GlobalExceptionHandler (5 error codes + type mismatch), IssuesService (delete delegation), GitLabIssuesAdapter (delete success/404/403/transport), controller validation | `src/test/java` |
| Integration (`*IT`) | Yes | Delete 204, delete validation 400, delete not-found 404, delete forbidden 403, updated existing error-status IT tests | `src/test/integration/java` |
| Component (`*ComponentTest`) | No | Existing `IssuesApiComponentTest` unchanged — delete endpoint component test deferred unless CI WireMock stub patterns are trivially available | N/A |
| Smoke | Yes | `scripts/smoke-test.sh` create→delete→re-delete flow | Running app |

## Acceptance Criteria

1. `DELETE /api/issues/{issueId}` with valid existing issue → 204 No Content, no body.
2. `DELETE /api/issues/{issueId}` with non-existent issue → 404 with standard `ErrorResponse`.
3. `DELETE /api/issues/{issueId}` when PAT lacks permission → 403 with standard `ErrorResponse`.
4. No GitLab internals, stack traces, or raw errors in any response.
5. `issueId` maps to GitLab `iid` using URL-encoded project path.

## Final Verification

- `scripts/verify-quick.sh` after each slice
- `scripts/final-check.sh` before handoff
- Repository-supported startup verification
- `scripts/smoke-test.sh` with running app (includes new delete checks)
