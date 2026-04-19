# Review Commands

Flow-log tracks architectural risks and code findings as durable state. Both follow a structured dialogue: reviewer records → author responds → reviewer adjudicates.

## Architectural Risks

Used during the architecture review loop (between Architecture Reviewer and Java Architect).

### Record a risk

```bash
node flow-log/flow-log.mjs add-risk \
  --feature my-feature \
  --description "Wrong composition strategy — data dependency requires sequential. Will cause runtime failures when parallel fetch races on shared state." \
  --suggested-fix "Switch fetchInParallel to sequential with justification in plan" \
  --by ArchitectureReviewer
```

Severity is omitted — defaults to `UNCLASSIFIED`. TL classifies severity after reviewing all findings.

Every finding **must** include `--suggested-fix`.

Optional: `--severity <CRITICAL|HIGH|MEDIUM|LOW>` (only used by TL or when re-adding a known-severity risk), `--plan-ref F3 --connected-area S2 --connected-area M4`

### Architect responds

```bash
node flow-log/flow-log.mjs respond-risk \
  --feature my-feature \
  --id 1 \
  --status ADDRESSED \
  --note "Switched to sequential with justification in plan section 6" \
  --by JavaArchitect
```

Status: `ADDRESSED` (fixed) or `INVALIDATED` (not a valid concern).

### Reviewer adjudicates

```bash
# Accept the response
node flow-log/flow-log.mjs resolve-risk \
  --feature my-feature --id 1 --by ArchitectureReviewer

# Reject the response
node flow-log/flow-log.mjs reopen-risk \
  --feature my-feature --id 1 \
  --reason "Sequential still wrong — bidirectional dependency" \
  --by ArchitectureReviewer
```

### TL severity classification

Architecture Reviewer records all findings as UNCLASSIFIED (no severity). TL reads each finding's violation and consequence, then assigns severity based on project needs and delivery cost:

```bash
node flow-log/flow-log.mjs reclassify-risk \
  --feature my-feature \
  --id 1 \
  --severity HIGH \
  --reason "Coder will guess wrong on composition — genuine runtime failure risk" \
  --by TL
```

The original severity is preserved as `previousSeverity` on the risk object. All risks must be classified before running `architecture-gate` — UNCLASSIFIED risks block the gate.

### Round management

```bash
# TL increments before each review round
node flow-log/flow-log.mjs increment-round --feature my-feature

# Check architecture gate (computed from risk state)
node flow-log/flow-log.mjs architecture-gate --feature my-feature
# Returns: PASS | FAIL | ESCALATE (3+ rounds)
```

### Escalation handling

When `architecture-gate` returns `ESCALATE`, it includes `unresolvedRisks`. TL logs a decision:

```bash
node flow-log/flow-log.mjs add-event \
  --feature my-feature \
  --type archEscalationDecision \
  --decision PROCEED_TO_CODING \
  --reason "Risk #1 is artifact naming only, not a correctness blocker" \
  --by TL
```

Decisions: `PROCEED_TO_CODING` (non-blocking), `FINAL_ADJUSTMENT` (targeted fix, skip re-review), `ESCALATE_TO_USER` (real blocker).

## Code Findings

Used during the code review loop (between Code Reviewer and Java Coder).

### Record a finding

```bash
node flow-log/flow-log.mjs add-finding \
  --feature my-feature \
  --severity HIGH \
  --description "Null check missing on mapper input" \
  --file "src/main/java/Mapper.java" \
  --by CodeReviewer
```

### Coder responds

```bash
node flow-log/flow-log.mjs respond-finding \
  --feature my-feature \
  --id 1 \
  --status FIXED \
  --note "Added Objects.requireNonNull with descriptive message" \
  --by JavaCoder
```

Status: `FIXED` or `DISPUTED`.

### Reviewer adjudicates

```bash
# Accept
node flow-log/flow-log.mjs resolve-finding \
  --feature my-feature --id 1 --by CodeReviewer

# Reject
node flow-log/flow-log.mjs reopen-finding \
  --feature my-feature --id 1 \
  --reason "Only fixed one of three call sites" \
  --by CodeReviewer
```

### Round management

```bash
node flow-log/flow-log.mjs increment-code-review-round --feature my-feature
node flow-log/flow-log.mjs code-review-gate --feature my-feature
# Returns: PASS | FAIL | ESCALATE (3+ rounds)
```
