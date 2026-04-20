### 2 MEDIUM — `GlobalExceptionHandler.mapStatus` Uses `Objects.requireNonNull` on a Structurally Non-Null Switch

**What is happening now:**  
In [GlobalExceptionHandler.java#L107](../../flow-orchestrator/src/main/java/com/gitlabflow/floworchestrator/common/web/GlobalExceptionHandler.java#L107), `Objects.requireNonNull(mapStatus(exception.errorCode()))` wraps a call to `mapStatus()`, which is an exhaustive switch expression with a `default` branch. The switch structurally cannot return null.

**Why it is a problem:**  
This violates the code-guidance rule: *"Do not use `Objects.requireNonNull` on compile-time constants, framework-provided non-null values, or methods that structurally cannot return null."* The `Objects.requireNonNull` is misleading since it implies the return could be null.

**What to prefer:**  
Remove the `Objects.requireNonNull` wrapper. The switch expression already covers all cases.

**Expected benefit:**  
Cleaner code; removes a false signal about nullability. Aligns with code-guidance gate.

---