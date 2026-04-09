# Implementation Plan: Expose `issueId` Field on Issue API Responses

**Artifact path:** `artifacts/implementation-plans/issue-id-field.plan.md`
**Task name:** `issue-id-field`
**Plan date:** `2025-07-24`
**Status:** `Draft`

---

## Business Goal

Add `issueId` (mapped from GitLab's `iid`, the project-scoped sequential issue number) to every issue object returned by `POST /api/issues/search` and `POST /api/issues`. This is a purely additive change that gives integrating clients a stable, human-meaningful identifier without requiring secondary lookups.

---

## Requirement Lock / Source Of Truth

- **Original request source:** `artifacts/user-prompts/feature-3-refactori-issue-api.md`
- **Non-negotiable contract constraints:**
  - Client-facing field name MUST be `issueId`; `iid` must NEVER appear in any API response payload.
  - Both `POST /api/issues/search` and `POST /api/issues` responses must include `issueId` on every returned issue object.
  - `issueId` maps directly and only from GitLab's `iid` (project-scoped, sequential; distinct from GitLab's global `id`).
- **Non-negotiable change-scope constraints:**
  - Additive only — no existing field is renamed, removed, or retyped.
  - No new endpoints, configuration keys, or architecture boundaries introduced.
- **Non-negotiable documentation constraints:**
  - `documentation/context-map.md` updated to reflect `issueId` on `IssueDto`.
  - `scripts/smoke-test.sh` updated to assert `issueId` is present from both endpoints.
- **Non-negotiable test constraints:**
  - All affected unit, integration, and component tests updated; no existing test may be removed or silenced.
- **Assumptions / unresolved items:**
  - GitLab's `iid` is always a positive integer on every issue response; no null guard for `iid` is required.
  - `IssueDto` is the single client-facing response model; changes are applied there and flow through the mapping layer.

---

## Payload Examples

### `POST /api/issues/search` — Before

```json
{
  "items": [
    {
      "id": 123456,
      "title": "Deploy failure in production",
      "description": "Step 3 failed",
      "state": "opened",
      "labels": ["bug"],
      "assignee": "john.doe",
      "milestone": "M1",
      "parent": 42
    }
  ],
  "count": 1,
  "page": 1
}
```

### `POST /api/issues/search` — After

```json
{
  "items": [
    {
      "id": 123456,
      "issueId": 7,
      "title": "Deploy failure in production",
      "description": "Step 3 failed",
      "state": "opened",
      "labels": ["bug"],
      "assignee": "john.doe",
      "milestone": "M1",
      "parent": 42
    }
  ],
  "count": 1,
  "page": 1
}
```

### `POST /api/issues` — Before

```json
{
  "id": 700,
  "title": "Deploy failure",
  "description": "Step 3 failed",
  "state": "opened",
  "labels": ["bug", "deploy"],
  "assignee": null,
  "milestone": null,
  "parent": null
}
```

### `POST /api/issues` — After

```json
{
  "id": 700,
  "issueId": 12,
  "title": "Deploy failure",
  "description": "Step 3 failed",
  "state": "opened",
  "labels": ["bug", "deploy"],
  "assignee": null,
  "milestone": null,
  "parent": null
}
```

---

## Validation Boundary Decision

| Validation Rule | Boundary | Why Here |
|---|---|---|
| `issueId` is always present and positive | GitLab API guarantee — no application validation required | GitLab's contract guarantees `iid` on every issue object; defensive null-guards for `iid` would add noise without value |
| `issueId` field name (not `iid`) in response | `IssueDto` record component name | Jackson serializes record component names as-is; naming the field `issueId` in the record is sufficient to enforce the contract |
| `issueId` must not leak as `iid` in responses | `GitLabIssueResponse` is package-private to the integration layer | `GitLabIssueResponse.iid()` is never passed to the orchestration layer directly; it is translated to `Issue.issueId()` by `GitLabIssuesMapper` at the integration boundary |

---

## Scope

### In Scope

- Add `long iid` to `GitLabIssueResponse` record (integration DTO — allows Jackson to deserialize the field from GitLab).
- Add `long issueId` to `Issue` orchestration model record.
- Add `long issueId` to `IssueDto` REST response DTO record.
- Update `GitLabIssuesMapper.toIssue()` to map `issueResponse.iid()` → `issue.issueId()`.
- Update `IssuesResponseMapper.toIssueDto()` to map `issue.issueId()` → `dto.issueId()`.
- Update debug log in `GitLabIssuesMapper.toIssue()` to include `issueId`.
- Add `"iid": 7` to `gitlab-issues-single.json` WireMock stub fixture.
- Update all test files that construct `GitLabIssueResponse`, `Issue`, or `IssueDto` records.
- Add `issueId` assertions to all relevant test methods.
- Update `scripts/smoke-test.sh` with a `check_field` helper and two field-presence checks.
- Update `documentation/context-map.md` to document `issueId` on `IssueDto`.
- Update `flow-orchestrator/http/issues.http` response comments to show `issueId`.

### Out of Scope

- Renaming or removing any existing response field.
- Exposing GitLab's global `id` differently.
- Adding `issueId` to any endpoint other than the two issue endpoints.
- Filtering, sorting, or searching by `issueId`.
- Any change to GitLab API authentication or pagination.

---

## Class Structure

### Affected Classes

| Class Path | Status | Change |
|---|---|---|
| `integration/gitlab/issues/dto/GitLabIssueResponse.java` | Modify | Add `long iid` as 2nd record component (after `id`) |
| `orchestration/issues/model/Issue.java` | Modify | Add `long issueId` as 2nd record component (after `id`) |
| `orchestration/issues/rest/dto/IssueDto.java` | Modify | Add `long issueId` as 2nd record component (after `id`) |
| `integration/gitlab/issues/mapper/GitLabIssuesMapper.java` | Modify | Pass `issueResponse.iid()` as `issueId` in `toIssue()`; add `issueId` to debug log |
| `orchestration/issues/rest/mapper/IssuesResponseMapper.java` | Modify | Pass `issue.issueId()` as `issueId` in `toIssueDto()` |
| `src/test/component/resources/stubs/issues/gitlab-issues-single.json` | Modify | Add `"iid": 7` to stub object |
| `src/test/java/.../mapper/GitLabIssuesMapperTest.java` | Modify | Add `iid` arg to all `GitLabIssueResponse` constructor calls; add `issueId` assertions |
| `src/test/java/.../IssuesServiceTest.java` | Modify | Add `issueId` arg to all `Issue` constructor calls |
| `src/test/java/.../model/IssuePageTest.java` | Modify | Add `issueId` arg to `Issue` constructor call |
| `src/test/java/.../rest/mapper/IssuesResponseMapperTest.java` | Modify | Add `issueId` arg to all `Issue` constructor calls; add `issueId` assertions on `IssueDto` results |
| `src/test/java/.../rest/dto/SearchIssuesResponseTest.java` | Modify | Add `issueId` arg to `IssueDto` constructor call |
| `src/test/integration/.../IssuesControllerIT.java` | Modify | Add `issueId` arg to all `Issue` and `IssueDto` constructor calls; add `jsonPath("$.issueId")` assertions |
| `src/test/component/.../IssuesApiComponentTest.java` | Modify | Add `assertThat(json.path("items").get(0).path("issueId").asLong()).isEqualTo(7L)` and `assertThat(json.path("issueId").asLong()).isEqualTo(12L)` |
| `scripts/smoke-test.sh` | Modify | Add `check_field` function; add two field-presence checks |
| `documentation/context-map.md` | Modify | Document `issueId` field on `IssueDto` |
| `flow-orchestrator/http/issues.http` | Modify | Add `issueId` to response examples in comments |

### Field Position Convention

`issueId` is inserted as the **second record component** in `Issue`, `IssueDto`, and `iid` as the **second component** in `GitLabIssueResponse` (immediately after `id`). This co-locates the two identifiers and keeps the positional ordering consistent across all three records.

```java
// GitLabIssueResponse (integration DTO)
public record GitLabIssueResponse(
    long id,
    long iid,          // NEW — deserializes GitLab JSON "iid" field
    String title,
    ...

// Issue (orchestration model)
public record Issue(
    long id,
    long issueId,      // NEW — project-scoped issue number
    String title,
    ...

// IssueDto (REST response DTO)
public record IssueDto(
    long id,
    long issueId,      // NEW — serializes as "issueId" in JSON response
    String title,
    ...
```

### Mapping Logic

```java
// GitLabIssuesMapper.toIssue() — integration → orchestration
return new Issue(
    issueResponse.id(),
    issueResponse.iid(),   // iid → issueId
    issueResponse.title(),
    ...

// IssuesResponseMapper.toIssueDto() — orchestration → REST
return new IssueDto(
    issue.id(),
    issue.issueId(),       // issueId → issueId (pass-through)
    issue.title(),
    ...
```

---

## Implementation Slices

### Slice 1 — Add `iid` to `GitLabIssueResponse`; update stub fixture and unit test constructors

- **Goal:** Make the integration DTO capable of deserializing GitLab's `iid` field. Update the WireMock stub fixture so component tests can verify the value end-to-end. Update `GitLabIssuesMapperTest` constructor calls so the unit test suite compiles.
- **Affected scope:** Integration DTO, stub JSON, one unit test class.
- **Payload / contract impact:** Internal only. `GitLabIssueResponse` is not serialized to clients.
- **Validation boundary decisions:** No validation. `iid` is a required, non-null `long`.

**Files changed:**
1. `flow-orchestrator/src/main/java/.../integration/gitlab/issues/dto/GitLabIssueResponse.java`
   - Add `long iid` as 2nd record component (between `id` and `title`).
   - No `@JsonProperty` annotation needed — field name matches GitLab JSON.
2. `flow-orchestrator/src/test/component/resources/stubs/issues/gitlab-issues-single.json`
   - Add `"iid": 7` after `"id": 123`. Value 7 is intentionally different from id=123 to prove the mapping does not confuse the two fields.
   - `gitlab-create-issue-response.json` already contains `"iid": 12` — no change needed.
3. `flow-orchestrator/src/test/java/.../integration/gitlab/issues/mapper/GitLabIssuesMapperTest.java`
   - All `new GitLabIssueResponse(...)` constructor calls: insert `iid` value as 2nd argument.
     - "mapsLabelsMilestoneEpicAndAssigneeList": `iid=5L`
     - "fallsBackToSingleAssigneeAndNullables": `iid=6L`
     - "returnsNullAssigneeWhenBothSourcesAreMissing": `iid=7L`
     - "fallsBackToSingleAssigneeWhenAssigneeListHasNullUsernames": `iid=8L`
   - No `issueId` assertions in this slice (mapper does not map `iid` yet — that is Slice 2).

**Unit tests required:**
- `GitLabIssuesMapperTest` — all existing tests must compile and pass with updated constructor calls.
- Edge case: no new assertion in this slice; the mapping is covered in Slice 2.

**Integration / Web tests:** None in this slice.

**Edge / failure coverage:** `iid` stub value differs from `id` to catch any field-swapping bug.

---

### Slice 2 — Add `issueId` to `Issue`; map `iid → issueId` in `GitLabIssuesMapper`

- **Goal:** Propagate `issueId` through the orchestration model and prove the mapping with assertions. Update all unit test classes that construct `Issue`.
- **Affected scope:** Orchestration model, integration mapper, four unit test classes.
- **Payload / contract impact:** Internal only. `Issue` is not serialized directly to clients.

**Files changed:**
1. `flow-orchestrator/src/main/java/.../orchestration/issues/model/Issue.java`
   - Add `long issueId` as 2nd record component (between `id` and `title`).
2. `flow-orchestrator/src/main/java/.../integration/gitlab/issues/mapper/GitLabIssuesMapper.java`
   - In `toIssue()`: pass `issueResponse.iid()` as the `issueId` argument.
   - Update the existing `log.debug(...)` call to include `issueId={}` after `id={}`.
3. `flow-orchestrator/src/test/java/.../integration/gitlab/issues/mapper/GitLabIssuesMapperTest.java`
   - Add `assertThat(issue.issueId()).isEqualTo(5L)` to "mapsLabelsMilestoneEpicAndAssigneeList".
   - Add a new test `@DisplayName("maps iid to issueId")` that constructs a `GitLabIssueResponse` with a known `iid` and asserts `issue.issueId()` equals that value. Use `iid=42L`.
4. `flow-orchestrator/src/test/java/.../orchestration/issues/IssuesServiceTest.java`
   - All `new Issue(...)` calls: insert `issueId` as 2nd argument.
     - "delegatesCreateInputToPortAndReturnsIssue": `new Issue(84L, 10L, "Deploy failure", ...)`
     - "returnsNullDescriptionUnchanged": `new Issue(85L, 11L, "Reporting bug", ...)`
5. `flow-orchestrator/src/test/java/.../orchestration/issues/model/IssuePageTest.java`
   - `new Issue(1L, "A", ...)` → `new Issue(1L, 1L, "A", ...)`

**Unit tests required:**
- `GitLabIssuesMapperTest` — existing tests still pass; new test "maps iid to issueId" verifies the mapping explicitly.
- `IssuesServiceTest` — all existing tests compile and pass (no new assertions needed; service does not inspect `issueId`).
- `IssuePageTest` — compiles and passes.

**Integration / Web tests:** None in this slice.

**Edge / failure coverage:** New test uses a distinct `iid` value (42L) to confirm the mapping assignment, not just default zero.

---

### Slice 3 — Add `issueId` to `IssueDto`; map in `IssuesResponseMapper`; update response unit tests

- **Goal:** Expose `issueId` in the client-facing DTO and verify mapping logic end-to-end from `Issue` → `IssueDto`.
- **Affected scope:** REST DTO, response mapper, two unit test classes.
- **Payload / contract impact:** `issueId` now appears in every JSON response from both endpoints.

**Files changed:**
1. `flow-orchestrator/src/main/java/.../orchestration/issues/rest/dto/IssueDto.java`
   - Add `long issueId` as 2nd record component (between `id` and `title`).
2. `flow-orchestrator/src/main/java/.../orchestration/issues/rest/mapper/IssuesResponseMapper.java`
   - In `toIssueDto()`: pass `issue.issueId()` as the `issueId` argument.
3. `flow-orchestrator/src/test/java/.../orchestration/issues/rest/mapper/IssuesResponseMapperTest.java`
   - All `new Issue(...)` calls: insert `issueId` as 2nd argument.
     - "mapsIssuePageToApiResponse" Issue: `new Issue(123L, 5L, "Title", "Description", "opened", List.of("bug"), "john", "M1", 42L)`
     - "keepsNullableFieldsAsNull" Issue: `new Issue(1L, 2L, "T", null, "closed", List.of(), null, null, null)`
     - "mapsMultipleIssuesIntoMultipleResponseItems" issues: `new Issue(11L, 3L, "A", ...)`, `new Issue(12L, 4L, "B", ...)`
     - "mapsIssueFieldsToIssueDto" Issue: add `issueId=6L`
     - "keepsNullableDescriptionAsNullForIssueDto" Issue: add `issueId=7L`
   - Add assertions on `issueId` in `IssueDto` results:
     - "mapsIssuePageToApiResponse": `assertThat(response.items().getFirst().issueId()).isEqualTo(5L)`
     - "mapsMultipleIssuesIntoMultipleResponseItems": assert `items[0].issueId() == 3L` and `items[1].issueId() == 4L`
     - "mapsIssueFieldsToIssueDto": `assertThat(response.issueId()).isEqualTo(6L)`
   - Add a new test `@DisplayName("maps issueId from issue to dto")` as an explicit boundary test.
4. `flow-orchestrator/src/test/java/.../orchestration/issues/rest/dto/SearchIssuesResponseTest.java`
   - `new IssueDto(1L, "A", ...)` → `new IssueDto(1L, 1L, "A", ...)`

**Unit tests required:**
- `IssuesResponseMapperTest` — updated existing tests plus new explicit mapping test.
- `SearchIssuesResponseTest` — compiles and passes.

**Integration / Web tests:** None in this slice.

**Edge / failure coverage:** Use distinct, non-zero `issueId` values in all `Issue` and `IssueDto` constructions so tests are not trivially passing on zero-default values.

---

### Slice 4 — Update integration tests (`IssuesControllerIT`)

- **Goal:** Verify the full request → response pipeline at the Spring MVC layer, proving `issueId` appears in the HTTP response JSON.
- **Affected scope:** One Spring `@WebMvcTest` integration test class.
- **Payload / contract impact:** `jsonPath("$.issueId")` and `jsonPath("$.items[0].issueId")` assertions added.

**Files changed:**
1. `flow-orchestrator/src/test/integration/.../IssuesControllerIT.java`
   - All `new Issue(...)` constructor calls: insert `issueId` as 2nd argument.
     - Line ~96: `new Issue(123L, 5L, "Title", "Desc", OPENED, List.of("bug"), JOHN_DOE, "M1", 42L)`
     - Line ~203: `new Issue(84L, 10L, "Deploy failure", "Step 3 failed", OPENED, List.of("bug", "deploy"), null, null, null)`
     - Line ~230: `new Issue(85L, 11L, "Reporting bug", null, OPENED, List.of(), null, null, null)`
   - All `new IssueDto(...)` constructor calls: insert `issueId` as 2nd argument using the same value as the corresponding `Issue`.
     - Line ~98: `new IssueDto(123L, 5L, "Title", "Desc", OPENED, List.of("bug"), JOHN_DOE, "M1", 42L)`
     - Line ~204: `new IssueDto(84L, 10L, "Deploy failure", ...)`
     - Line ~231: `new IssueDto(85L, 11L, "Reporting bug", null, OPENED, List.of(), null, null, null)`
   - Add `jsonPath` assertions in search test "acceptsValidFilterBody":
     - `.andExpect(jsonPath("$.items[0].issueId").value(5))`
   - Add `jsonPath` assertions in create test "createsIssueWithFullPayloadAndReturns201":
     - `.andExpect(jsonPath("$.issueId").value(10))`
   - Add `jsonPath` assertions in create test "createsIssueWithTitleOnlyAndReturns201":
     - `.andExpect(jsonPath("$.issueId").value(11))`

**Unit tests:** None in this slice.

**Integration / Web tests required:**
- `IssuesControllerIT` — all existing tests compile and pass; new assertions verify `issueId` presence in HTTP response.

**Edge / failure coverage:** Assert `issueId` value is correct (not just present) using known test fixture values.

---

### Slice 5 — Update component tests (`IssuesApiComponentTest`)

- **Goal:** Verify `issueId` is present and correct in live-over-WireMock responses from both endpoints, confirming the full serialization path including Jackson deserialization of `iid` from the GitLab stub.
- **Affected scope:** One component test class. Relies on `gitlab-issues-single.json` updated in Slice 1 (with `"iid": 7`).

**Files changed:**
1. `flow-orchestrator/src/test/component/.../IssuesApiComponentTest.java`
   - In "returnsMappedIssuesForRequestWithoutBody":
     - Add: `assertThat(json.path("items").get(0).path("issueId").asLong()).isEqualTo(7L);`
   - In "createsIssueAndReturnsMappedResponse":
     - Add: `assertThat(json.path("issueId").asLong()).isEqualTo(12L);`
     - (Value 12 corresponds to `"iid": 12` already in `gitlab-create-issue-response.json`.)

**Unit tests:** None in this slice.

**Integration / Web tests:** None in this slice.

**Component tests required:**
- `IssuesApiComponentTest` — two updated test methods assert correct `issueId` values end-to-end through WireMock.

**Edge / failure coverage:** The stub `iid` values (7 and 12) are distinct from `id` values (123 and 700) so any field-confusion bug would produce a failing assertion.

---

### Slice 6 — Smoke test, documentation, and HTTP examples

- **Goal:** Complete the delivery by updating the operational smoke test, the codebase capability map, and the HTTP example file.
- **Affected scope:** Shell script, markdown documentation, HTTP example file.

**Files changed:**

1. `scripts/smoke-test.sh`

   Add a `check_field` helper function immediately before the first `check` call:

   ```bash
   check_field() {
     local name="$1" method="$2" url="$3" field="$4"
     shift 4
     local body
     body=$(curl -s -X "$method" "$@" "$url") || true
     if echo "$body" | grep -q "\"${field}\""; then
       RESULTS+="  PASS  $name (field '${field}' present)\n"
       PASS=$((PASS + 1))
     else
       RESULTS+="  FAIL  $name (field '${field}' missing)\n"
       FAIL=$((FAIL + 1))
     fi
   }
   ```

   Add two field-presence checks after the existing `check` calls (after the create validation error check):

   ```bash
   # issueId field presence — verify additive field appears in both endpoints
   check_field "POST /api/issues/search issueId present" POST "${BASE_URL}/api/issues/search" "issueId" \
     -H "Content-Type: application/json" -d '{}'

   SMOKE_FIELD_TITLE="Smoke field check $(date -u +%Y%m%d%H%M%S)"
   check_field "POST /api/issues issueId present" POST "${BASE_URL}/api/issues" "issueId" \
     -H "Content-Type: application/json" \
     -d "{\"title\":\"${SMOKE_FIELD_TITLE}\",\"description\":\"Created by smoke test\",\"labels\":[\"smoke\",\"api\"]}"
   ```

2. `documentation/context-map.md`

   Update the `IssueDto` description line (currently reads: `IssueDto is the single API response shape for all issue endpoints`). Add a bullet or inline note:

   ```markdown
   - `IssueDto` is the single API response shape for all issue endpoints; fields: `id` (GitLab global id), `issueId` (project-scoped number, maps from GitLab `iid`), `title`, `description`, `state`, `labels`, `assignee`, `milestone`, `parent`
   ```

   Also update the field list on line 23 (DTOs listing) if it enumerates `IssueDto` fields.

3. `flow-orchestrator/http/issues.http`

   Add `issueId` to the response comment blocks. For example, after each successful request example, add a `# Response includes: { "id": ..., "issueId": ..., ... }` comment. The `.http` file does not contain literal response bodies today; if comments are added, keep them brief. If the file has no response comments, add one inline comment under the create request:

   ```
   ### Create issue request (full payload)
   # Response: 201 Created — includes issueId (project-scoped number from GitLab iid)
   POST {{baseUrl}}/api/issues
   ...
   ```

---

## Testing Matrix

| Level | Test Class | Slice | Coverage Added |
|---|---|---|---|
| Unit | `GitLabIssuesMapperTest` | 1, 2 | Constructor arity updated; new test `maps iid to issueId` asserts `issue.issueId()` equals `iid` value |
| Unit | `IssuesServiceTest` | 2 | Constructor arity updated; no new assertions (service is transparent to `issueId`) |
| Unit | `IssuePageTest` | 2 | Constructor arity updated |
| Unit | `IssuesResponseMapperTest` | 3 | Constructor arity updated; assertions on `issueId` in mapped `IssueDto`; new test `maps issueId from issue to dto` |
| Unit | `SearchIssuesResponseTest` | 3 | Constructor arity updated |
| Integration | `IssuesControllerIT` | 4 | `jsonPath("$.items[0].issueId").value(5)` on search; `jsonPath("$.issueId").value(10)` and `.value(11)` on create |
| Component | `IssuesApiComponentTest` | 5 | `issueId == 7` from WireMock stub on search; `issueId == 12` from create stub |

**Coverage notes:**
- Every level proves something different: unit validates mapping logic in isolation; integration validates Spring MVC serialization and JSON path; component validates end-to-end through the deserialization of a realistic GitLab stub.
- No scenario is duplicated across levels — the distinct WireMock `iid` values at component level cannot be validated at unit level.

---

## Logging Requirements

One existing log statement in `GitLabIssuesMapper.toIssue()` must be updated:

**Current:**
```java
log.debug(
    "Mapped GitLab issue id={} state={} labels={} assigneePresent={} milestonePresent={} parentPresent={}",
    issueResponse.id(),
    issueResponse.state(),
    ...
```

**Updated:**
```java
log.debug(
    "Mapped GitLab issue id={} issueId={} state={} labels={} assigneePresent={} milestonePresent={} parentPresent={}",
    issueResponse.id(),
    issueResponse.iid(),
    issueResponse.state(),
    ...
```

No new `INFO` or `WARN` log statements. No `ERROR` log statements. The existing `DEBUG` log update is sufficient to make `issueId` traceable in developer/diagnostic output.

---

## Documentation Update Requirements

| Document | Change Required |
|---|---|
| `documentation/context-map.md` | Add `issueId` to the `IssueDto` field description; add note that `issueId` maps from GitLab `iid`. Update test count if affected. |
| `flow-orchestrator/http/issues.http` | Add inline response comment showing `issueId` in the create issue example. |
| `scripts/smoke-test.sh` | Add `check_field` function and two field-presence assertions as specified in Slice 6. |
| `artifacts/implementation-plans/issue-id-field.plan.md` | This document — no further updates needed post-implementation. |

`README.md` does not need updating — it does not enumerate individual response fields.

---

## Acceptance Criteria Traceability

| AC# | Story Acceptance Criterion | Verified By |
|---|---|---|
| AC1 | `POST /api/issues/search` returns `issueId` on every issue object | `IssuesControllerIT` (Slice 4) + `IssuesApiComponentTest` (Slice 5) |
| AC2 | `POST /api/issues` returns `issueId` on the created issue | `IssuesControllerIT` (Slice 4) + `IssuesApiComponentTest` (Slice 5) |
| AC3 | Field is named exactly `issueId`; `iid` never appears in responses | `IssueDto` record component name enforces serialization name; `IssuesControllerIT` `jsonPath("$.issueId")` |
| AC4 | `issueId` equals the GitLab `iid` for a known issue | `GitLabIssuesMapperTest` "maps iid to issueId" (Slice 2); `IssuesApiComponentTest` stub fixture values (Slice 5) |
| AC5 | `scripts/smoke-test.sh` asserts `issueId` from both endpoints | `check_field` assertions in Slice 6 |
| AC6 | All tests pass | `scripts/verify-quick.sh` green; `scripts/final-check.sh` green |
| AC7 | `context-map.md` documents `issueId` on `IssueDto` | Documentation update in Slice 6 |
| AC8 | No existing field renamed, removed, or retyped | Additive record component insertion; enforced by existing tests still asserting all prior fields |

---

## Final Verification

1. **Fast gate:** `scripts/verify-quick.sh` — must be green after each slice.
2. **Full gate:** `scripts/final-check.sh` — must be green before handoff (applies Spotless formatting, then runs `mvn clean verify` with Checkstyle, PMD, SpotBugs, JaCoCo ≥ 85% line coverage).
3. **Application startup:** `mvn spring-boot:run` with `SPRING_PROFILES_ACTIVE=local` — must start without errors.
4. **Smoke test:** `scripts/smoke-test.sh` — must report `0 failed`; the two new `check_field` assertions must both show `PASS`.
5. **Expected report artifacts after `scripts/final-check.sh`:**
   - `flow-orchestrator/target/checkstyle-result.xml`
   - `flow-orchestrator/target/pmd.xml`
   - `flow-orchestrator/target/cpd.xml`
   - `flow-orchestrator/target/spotbugsXml.xml`
   - `flow-orchestrator/target/site/jacoco/jacoco.xml`
