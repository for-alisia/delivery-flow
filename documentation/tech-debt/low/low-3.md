### 3 LOW — Missing Unit Tests for `ChangeField.fromValue()`

**What is happening now:**  
`ChangeField` ([ChangeField.java](../../flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/orchestration/common/model/ChangeField.java)) has a `@JsonCreator` method `fromValue(String)` with both label-match and name-match paths, and an error path. No direct unit tests exist for this method. The `@JsonValue` serialization is tested indirectly through the `SearchIssuesResponseTest` and `IssuesControllerIT`, but the `fromValue()` parsing logic (case-insensitive label match, enum name match, unsupported value error) is untested.

**Why it is a problem:**  
Test instructions require: *"Every non-trivial logic, validation, defaulting, mapping, or error translation change requires unit coverage."* The `fromValue` method contains branching logic (3 outcomes) that could regress silently.

**What to prefer:**  
Add a `ChangeFieldTest` class covering:
- `fromValue("label")` → `LABEL`
- `fromValue("LABEL")` → `LABEL` (case-insensitive)
- `fromValue("unknown")` → `IllegalArgumentException`

**Expected benefit:**  
Direct coverage of parsing logic; guards the wire-format contract established by the refactoring.

---