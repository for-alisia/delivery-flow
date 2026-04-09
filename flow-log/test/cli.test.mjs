import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";

const repoRoot = path.resolve(path.dirname(new URL(import.meta.url).pathname), "..", "..");
const cliPath = path.join(repoRoot, "flow-log", "flow-log.mjs");

test("flow-log records minimal workflow facts and reports signoff readiness", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-test-"));
  const statePath = path.join(tempRoot, "feature.json");
  const storyPath = path.join(tempRoot, "story.md");
  const planPath = path.join(tempRoot, "plan.md");

  fs.writeFileSync(storyPath, "# Story\n");
  fs.writeFileSync(planPath, "# Plan\n");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, ["lock-requirements", "--feature", "demo", "--state-path", statePath, "--by", "TL"]);
  runCli(tempRoot, [
    "register-artifact",
    "story",
    "--feature",
    "demo",
    "--state-path",
    statePath,
    "--path",
    storyPath
  ]);
  runCli(tempRoot, [
    "approve-artifact",
    "story",
    "--feature",
    "demo",
    "--state-path",
    statePath,
    "--by",
    "TL"
  ]);
  runCli(tempRoot, [
    "register-artifact",
    "plan",
    "--feature",
    "demo",
    "--state-path",
    statePath,
    "--path",
    planPath
  ]);
  runCli(tempRoot, [
    "approve-artifact",
    "plan",
    "--feature",
    "demo",
    "--state-path",
    statePath,
    "--by",
    "TL"
  ]);
  runCli(tempRoot, [
    "set-review",
    "--feature",
    "demo",
    "--state-path",
    statePath,
    "--name",
    "architectureReview",
    "--status",
    "PASS",
    "--by",
    "Reviewer"
  ]);
  runCli(tempRoot, [
    "set-review",
    "--feature",
    "demo",
    "--state-path",
    statePath,
    "--name",
    "codeReview",
    "--status",
    "PASS",
    "--by",
    "Reviewer"
  ]);
  runCli(tempRoot, [
    "set-check",
    "--feature",
    "demo",
    "--state-path",
    statePath,
    "--name",
    "finalCheck",
    "--status",
    "PASS",
    "--by",
    "TL",
    "--command",
    "scripts/final-check.sh"
  ]);
  runCli(tempRoot, [
    "set-check",
    "--feature",
    "demo",
    "--state-path",
    statePath,
    "--name",
    "karate",
    "--status",
    "PASS",
    "--by",
    "TL",
    "--command",
    "scripts/karate-test.sh"
  ]);

  const readiness = runCli(tempRoot, [
    "readiness",
    "signoff",
    "--feature",
    "demo",
    "--state-path",
    statePath
  ]);

  assert.equal(readiness.readiness.ready, true);
  assert.deepEqual(readiness.readiness.reasons, []);
});

test("flow-log summary reports missing signoff requirements", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-test-"));
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  const summary = runCli(tempRoot, ["summary", "--feature", "demo", "--state-path", statePath]);

  assert.equal(summary.summary.readiness.ready, false);
  assert.match(summary.summary.nextActions[0], /Requirements must be locked/);
});

test("flow-log status is compact and signoff-oriented", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-test-"));
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  const status = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);

  assert.equal(status.status.feature, "demo");
  assert.equal(status.status.phase, "requirement-lock");
  assert.equal(status.status.readyForSignoff, false);
  assert.equal(status.status.architectureReview, "PENDING");
  assert.equal(status.status.codeReview, "PENDING");
  assert.ok(Array.isArray(status.status.missing));
  assert.equal("artifacts" in status.status, false);
  assert.equal("events" in status.status, false);
});

test("flow-log tracks red cards and rejections in event history", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-test-"));
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, [
    "add-event",
    "--feature",
    "demo",
    "--state-path",
    statePath,
    "--type",
    "redCard",
    "--by",
    "TL",
    "--target",
    "JavaCoder",
    "--related-check",
    "finalCheck",
    "--reason",
    "Coder claimed ready but final-check failed"
  ]);
  runCli(tempRoot, [
    "add-event",
    "--feature",
    "demo",
    "--state-path",
    statePath,
    "--type",
    "rejection",
    "--by",
    "CodeReviewer",
    "--target",
    "JavaCoder",
    "--related-review",
    "codeReview",
    "--reason",
    "Implementation does not match approved plan"
  ]);

  const summary = runCli(tempRoot, ["summary", "--feature", "demo", "--state-path", statePath]);
  const history = runCli(tempRoot, ["history", "--feature", "demo", "--state-path", statePath]);

  assert.equal(summary.summary.events.total, 2);
  assert.equal(summary.summary.events.counts.redCard, 1);
  assert.equal(summary.summary.events.counts.rejection, 1);
  assert.equal(history.eventCount, 2);
  assert.equal(history.events[0].type, "redCard");
  assert.equal(history.events[1].relatedReview, "codeReview");
});

test("flow-log tracks batch lifecycle (start, complete, multiple batches)", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-test-"));
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  // Start batch 1 with slices
  const b1Start = runCli(tempRoot, [
    "start-batch",
    "--feature", "demo",
    "--state-path", statePath,
    "--slice", "slice-1",
    "--slice", "slice-2",
    "--by", "TL"
  ]);
  assert.equal(b1Start.ok, true);
  assert.equal(b1Start.batch.batch, 1);
  assert.deepEqual(b1Start.batch.slices, ["slice-1", "slice-2"]);
  assert.equal(b1Start.batch.status, "in-progress");

  // Status should show current batch
  const statusMid = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(statusMid.status.currentBatch, 1);
  assert.equal(statusMid.status.totalBatches, 1);

  // Complete batch 1
  const b1End = runCli(tempRoot, [
    "complete-batch",
    "--feature", "demo",
    "--state-path", statePath,
    "--status", "complete"
  ]);
  assert.equal(b1End.ok, true);
  assert.equal(b1End.completedBatch.batch, 1);
  assert.equal(b1End.completedBatch.status, "complete");
  assert.equal(b1End.totalBatches, 1);

  // Start and complete batch 2
  runCli(tempRoot, [
    "start-batch",
    "--feature", "demo",
    "--state-path", statePath,
    "--slice", "slice-3",
    "--by", "TL"
  ]);
  runCli(tempRoot, [
    "complete-batch",
    "--feature", "demo",
    "--state-path", statePath
  ]);

  // Summary should show batch counts
  const summary = runCli(tempRoot, ["summary", "--feature", "demo", "--state-path", statePath]);
  assert.equal(summary.summary.batches.completed, 2);
  assert.equal(summary.summary.batches.total, 2);
  assert.equal(summary.summary.batches.current, null);
});

test("flow-log reset-checks clears all checks and records red card event", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-test-"));
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  // Set some checks to PASS
  runCli(tempRoot, [
    "set-check", "--feature", "demo", "--state-path", statePath,
    "--name", "verifyQuick", "--status", "PASS", "--by", "Coder"
  ]);
  runCli(tempRoot, [
    "set-check", "--feature", "demo", "--state-path", statePath,
    "--name", "finalCheck", "--status", "PASS", "--by", "TL"
  ]);
  runCli(tempRoot, [
    "set-check", "--feature", "demo", "--state-path", statePath,
    "--name", "karate", "--status", "PASS", "--by", "TL"
  ]);

  // Verify they are PASS
  let status = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(status.status.finalCheck, "PASS");
  assert.equal(status.status.karate, "PASS");

  // Reset checks (red card scenario)
  const reset = runCli(tempRoot, [
    "reset-checks", "--feature", "demo", "--state-path", statePath,
    "--reason", "Component test failure found by TL",
    "--by", "TL",
    "--target", "JavaCoder"
  ]);
  assert.equal(reset.ok, true);
  assert.equal(reset.checks.verifyQuick, "NOT_RUN");
  assert.equal(reset.checks.finalCheck, "NOT_RUN");
  assert.equal(reset.checks.karate, "NOT_RUN");
  assert.equal(reset.eventRecorded.type, "redCard");
  assert.equal(reset.eventRecorded.reason, "Component test failure found by TL");

  // Status should reflect reset
  status = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(status.status.finalCheck, "NOT_RUN");
  assert.equal(status.status.karate, "NOT_RUN");
});

test("flow-log lock-requirements stores request source in state and summary", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-test-"));
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  const lockResult = runCli(tempRoot, [
    "lock-requirements", "--feature", "demo", "--state-path", statePath,
    "--by", "TL",
    "--request-source", "artifacts/user-prompts/demo.md"
  ]);

  assert.equal(lockResult.ok, true);
  assert.equal(lockResult.requirements.requestSource, "artifacts/user-prompts/demo.md");

  const summary = runCli(tempRoot, ["summary", "--feature", "demo", "--state-path", statePath]);
  assert.equal(summary.summary.requestSource, "artifacts/user-prompts/demo.md");
});

test("flow-log complete records end time and calculates duration", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-test-"));
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  // State should have timing.startedAt set
  const raw = JSON.parse(fs.readFileSync(statePath, "utf8"));
  assert.ok(raw.timing.startedAt);
  assert.equal(raw.timing.completedAt, null);
  assert.equal(raw.timing.durationMinutes, null);

  // Complete the flow
  const result = runCli(tempRoot, ["complete", "--feature", "demo", "--state-path", statePath]);
  assert.equal(result.ok, true);
  assert.ok(result.timing.completedAt);
  assert.equal(typeof result.timing.durationMinutes, "number");

  // Summary should include timing
  const summary = runCli(tempRoot, ["summary", "--feature", "demo", "--state-path", statePath]);
  assert.ok(summary.summary.timing.startedAt);
  assert.ok(summary.summary.timing.completedAt);
});

test("flow-log architectural risks full lifecycle: add, respond, resolve/reopen, gate", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-test-"));
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  // Round 1: Architecture Reviewer adds risks
  runCli(tempRoot, ["increment-round", "--feature", "demo", "--state-path", statePath]);

  const r1 = runCli(tempRoot, [
    "add-risk", "--feature", "demo", "--state-path", statePath,
    "--severity", "CRITICAL", "--description", "Missing interface contract for port",
    "--by", "ArchitectureReviewer"
  ]);
  assert.equal(r1.risk.id, 1);
  assert.equal(r1.risk.severity, "CRITICAL");
  assert.equal(r1.risk.status, "OPEN");
  assert.equal(r1.risk.round, 1);

  runCli(tempRoot, [
    "add-risk", "--feature", "demo", "--state-path", statePath,
    "--severity", "MEDIUM", "--description", "Naming could be clearer",
    "--by", "ArchitectureReviewer"
  ]);

  // Gate should FAIL (unresolved CRITICAL)
  let gate = runCli(tempRoot, ["architecture-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "FAIL");
  assert.equal(gate.unresolvedBlocking, 1);

  // Architect responds: addresses the critical, invalidates nothing
  const resp = runCli(tempRoot, [
    "respond-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "ADDRESSED",
    "--note", "Added PortInterface with full contract in plan section 5",
    "--by", "JavaArchitect"
  ]);
  assert.equal(resp.risk.status, "ADDRESSED");
  assert.equal(resp.risk.responseNote, "Added PortInterface with full contract in plan section 5");

  // Architecture Reviewer accepts: resolves the critical
  const resolved = runCli(tempRoot, [
    "resolve-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--by", "ArchitectureReviewer"
  ]);
  assert.equal(resolved.risk.status, "RESOLVED");

  // Gate should now PASS (no unresolved CRITICAL/HIGH)
  gate = runCli(tempRoot, ["architecture-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "PASS");
  assert.equal(gate.unresolvedBlocking, 0);

  // Summary should show risks
  const summary = runCli(tempRoot, ["summary", "--feature", "demo", "--state-path", statePath]);
  assert.equal(summary.summary.architecturalRisks.total, 2);
  assert.equal(summary.summary.architecturalRisks.round, 1);
  assert.equal(summary.summary.architecturalRisks.byStatus.RESOLVED, 1);
  assert.equal(summary.summary.architecturalRisks.byStatus.OPEN, 1); // MEDIUM still open
  assert.equal(summary.summary.architecturalRisks.risks[0].responseNote, "Added PortInterface with full contract in plan section 5");

  // Status should show architecture gate
  const status = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(status.status.architectureGate, "PASS");
  assert.equal(status.status.architectureReviewRound, 1);
});

test("flow-log add-risk stores suggestedFix and surfaces it in summary", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-test-"));
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, ["increment-round", "--feature", "demo", "--state-path", statePath]);

  // Add risk with suggestedFix
  const r1 = runCli(tempRoot, [
    "add-risk", "--feature", "demo", "--state-path", statePath,
    "--severity", "HIGH",
    "--description", "Missing fail-fast on rejected execution",
    "--suggested-fix", "Add CallerRunsPolicy or throw ServiceUnavailableException from a RejectedExecutionHandler",
    "--by", "ArchitectureReviewer"
  ]);
  assert.equal(r1.risk.suggestedFix, "Add CallerRunsPolicy or throw ServiceUnavailableException from a RejectedExecutionHandler");
  assert.equal(r1.risk.status, "OPEN");

  // Add risk without suggestedFix
  const r2 = runCli(tempRoot, [
    "add-risk", "--feature", "demo", "--state-path", statePath,
    "--severity", "MEDIUM",
    "--description", "Naming could be clearer",
    "--by", "ArchitectureReviewer"
  ]);
  assert.equal(r2.risk.suggestedFix, null);

  // Summary should include suggestedFix
  const summary = runCli(tempRoot, ["summary", "--feature", "demo", "--state-path", statePath]);
  assert.equal(summary.summary.architecturalRisks.risks[0].suggestedFix, "Add CallerRunsPolicy or throw ServiceUnavailableException from a RejectedExecutionHandler");
  assert.equal(summary.summary.architecturalRisks.risks[1].suggestedFix, null);
});

test("flow-log architectural risks reopen and escalation after 5 rounds", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-test-"));
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  // Round 1: add HIGH risk
  runCli(tempRoot, ["increment-round", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, [
    "add-risk", "--feature", "demo", "--state-path", statePath,
    "--severity", "HIGH", "--description", "Wrong composition strategy",
    "--by", "ArchitectureReviewer"
  ]);

  // Architect addresses it
  runCli(tempRoot, [
    "respond-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "ADDRESSED",
    "--note", "Switched to sequential", "--by", "JavaArchitect"
  ]);

  // Reviewer reopens: not satisfied
  const reopened = runCli(tempRoot, [
    "reopen-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--reason", "Sequential still wrong — data dependency is bidirectional",
    "--by", "ArchitectureReviewer"
  ]);
  assert.equal(reopened.risk.status, "REOPENED");

  // Rounds 2-4: same pattern
  for (let round = 2; round <= 4; round++) {
    runCli(tempRoot, ["increment-round", "--feature", "demo", "--state-path", statePath]);
    runCli(tempRoot, [
      "respond-risk", "--feature", "demo", "--state-path", statePath,
      "--id", "1", "--status", "ADDRESSED",
      "--note", `Attempt ${round}`, "--by", "JavaArchitect"
    ]);
    runCli(tempRoot, [
      "reopen-risk", "--feature", "demo", "--state-path", statePath,
      "--id", "1", "--reason", `Still not fixed in round ${round}`,
      "--by", "ArchitectureReviewer"
    ]);
  }

  // Round 5
  runCli(tempRoot, ["increment-round", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, [
    "respond-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "ADDRESSED",
    "--note", "Introduced mediator", "--by", "JavaArchitect"
  ]);
  runCli(tempRoot, [
    "reopen-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--reason", "Mediator adds complexity without solving root cause",
    "--by", "ArchitectureReviewer"
  ]);

  // Gate should ESCALATE (5 rounds, still unresolved HIGH)
  const gate = runCli(tempRoot, ["architecture-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "ESCALATE");
  assert.equal(gate.round, 5);
  assert.equal(gate.unresolvedBlocking, 1);
  assert.ok(gate.unresolvedRisks.length === 1);
  assert.equal(gate.unresolvedRisks[0].severity, "HIGH");
  assert.match(gate.message, /TL must decide/);

  // TL logs escalation decision
  const decision = runCli(tempRoot, [
    "add-event", "--feature", "demo", "--state-path", statePath,
    "--type", "archEscalationDecision",
    "--decision", "PROCEED_TO_CODING",
    "--reason", "Risk #1 is artifact naming only, not a correctness blocker",
    "--by", "TL"
  ]);
  assert.equal(decision.event.type, "archEscalationDecision");
  assert.equal(decision.event.decision, "PROCEED_TO_CODING");

  // Hard cap: increment-round should refuse round 6
  assert.throws(
    () => runCli(tempRoot, ["increment-round", "--feature", "demo", "--state-path", statePath]),
    /hard cap reached/i
  );
});

test("flow-log architectural risks invalidation path", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-test-"));
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, ["increment-round", "--feature", "demo", "--state-path", statePath]);

  // Add a HIGH risk
  runCli(tempRoot, [
    "add-risk", "--feature", "demo", "--state-path", statePath,
    "--severity", "HIGH", "--description", "Shared infra duplicated",
    "--by", "ArchitectureReviewer"
  ]);

  // Architect invalidates: argues it's not actually duplicated
  runCli(tempRoot, [
    "respond-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "INVALIDATED",
    "--note", "Uses SharedMapper which already exists in shared infra — not duplication",
    "--by", "JavaArchitect"
  ]);

  // Reviewer accepts the invalidation
  runCli(tempRoot, [
    "resolve-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--by", "ArchitectureReviewer"
  ]);

  const gate = runCli(tempRoot, ["architecture-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "PASS");
});

// --- Code Findings tests ---

test("flow-log code findings full lifecycle: add, respond, resolve/reopen, gate", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-test-"));
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  // Round 1: Code Reviewer adds findings
  runCli(tempRoot, ["increment-code-review-round", "--feature", "demo", "--state-path", statePath]);

  const f1 = runCli(tempRoot, [
    "add-finding", "--feature", "demo", "--state-path", statePath,
    "--severity", "HIGH", "--description", "Null check missing on mapper input",
    "--file", "src/main/java/Mapper.java",
    "--by", "CodeReviewer"
  ]);
  assert.equal(f1.finding.id, 1);
  assert.equal(f1.finding.severity, "HIGH");
  assert.equal(f1.finding.status, "OPEN");
  assert.equal(f1.finding.file, "src/main/java/Mapper.java");
  assert.equal(f1.finding.round, 1);

  runCli(tempRoot, [
    "add-finding", "--feature", "demo", "--state-path", statePath,
    "--severity", "LOW", "--description", "Consider extracting constant",
    "--by", "CodeReviewer"
  ]);

  // Gate should FAIL (unresolved HIGH)
  let gate = runCli(tempRoot, ["code-review-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "FAIL");
  assert.equal(gate.unresolvedBlocking, 1);

  // Coder responds: fixes the HIGH finding
  const resp = runCli(tempRoot, [
    "respond-finding", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "FIXED",
    "--note", "Added Objects.requireNonNull with descriptive message",
    "--by", "JavaCoder"
  ]);
  assert.equal(resp.finding.status, "FIXED");
  assert.equal(resp.finding.responseNote, "Added Objects.requireNonNull with descriptive message");

  // Code Reviewer accepts: resolves the finding
  const resolved = runCli(tempRoot, [
    "resolve-finding", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--by", "CodeReviewer"
  ]);
  assert.equal(resolved.finding.status, "RESOLVED");

  // Gate should now PASS (no unresolved HIGH/CRITICAL)
  gate = runCli(tempRoot, ["code-review-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "PASS");
  assert.equal(gate.unresolvedBlocking, 0);

  // Summary should show findings
  const summary = runCli(tempRoot, ["summary", "--feature", "demo", "--state-path", statePath]);
  assert.equal(summary.summary.codeFindings.total, 2);
  assert.equal(summary.summary.codeFindings.round, 1);
  assert.equal(summary.summary.codeFindings.byStatus.RESOLVED, 1);
  assert.equal(summary.summary.codeFindings.byStatus.OPEN, 1); // LOW still open
  assert.equal(summary.summary.codeFindings.findings[0].file, "src/main/java/Mapper.java");

  // Status should show code review gate
  const status = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(status.status.codeReviewGate, "PASS");
  assert.equal(status.status.codeReviewRound, 1);
});

test("flow-log code findings reopen and escalation after 3 rounds", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-test-"));
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  // Round 1: add CRITICAL finding
  runCli(tempRoot, ["increment-code-review-round", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, [
    "add-finding", "--feature", "demo", "--state-path", statePath,
    "--severity", "CRITICAL", "--description", "SQL injection via string concatenation",
    "--file", "src/main/java/Repository.java",
    "--by", "CodeReviewer"
  ]);

  // Coder fixes it
  runCli(tempRoot, [
    "respond-finding", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "FIXED",
    "--note", "Switched to parameterized query", "--by", "JavaCoder"
  ]);

  // Reviewer reopens: fix is incomplete
  const reopened = runCli(tempRoot, [
    "reopen-finding", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--reason", "Only fixed one of three query methods",
    "--by", "CodeReviewer"
  ]);
  assert.equal(reopened.finding.status, "REOPENED");

  // Round 2
  runCli(tempRoot, ["increment-code-review-round", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, [
    "respond-finding", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "FIXED",
    "--note", "Fixed all three methods", "--by", "JavaCoder"
  ]);
  runCli(tempRoot, [
    "reopen-finding", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--reason", "Third method still uses string concat for ORDER BY",
    "--by", "CodeReviewer"
  ]);

  // Round 3
  runCli(tempRoot, ["increment-code-review-round", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, [
    "respond-finding", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "FIXED",
    "--note", "Replaced ORDER BY with enum-based approach", "--by", "JavaCoder"
  ]);
  runCli(tempRoot, [
    "reopen-finding", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--reason", "Enum approach allows arbitrary column names",
    "--by", "CodeReviewer"
  ]);

  // Gate should ESCALATE (3 rounds, still unresolved CRITICAL)
  const gate = runCli(tempRoot, ["code-review-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "ESCALATE");
  assert.equal(gate.round, 3);
  assert.equal(gate.unresolvedBlocking, 1);
  assert.match(gate.message, /Escalate to user/);
});

test("flow-log code findings disputed path", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-test-"));
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, ["increment-code-review-round", "--feature", "demo", "--state-path", statePath]);

  // Add a HIGH finding
  runCli(tempRoot, [
    "add-finding", "--feature", "demo", "--state-path", statePath,
    "--severity", "HIGH", "--description", "Missing error handling on HTTP call",
    "--by", "CodeReviewer"
  ]);

  // Coder disputes: argues Feign decoder already handles it
  runCli(tempRoot, [
    "respond-finding", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "DISPUTED",
    "--note", "FeignErrorDecoder in SharedInfra already handles all 4xx/5xx — test in ErrorDecoderTest",
    "--by", "JavaCoder"
  ]);

  // Reviewer accepts the dispute
  runCli(tempRoot, [
    "resolve-finding", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--by", "CodeReviewer"
  ]);

  const gate = runCli(tempRoot, ["code-review-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "PASS");
});

test("flow-log plan: full lifecycle — all sections populated, validate, summary, plan-get", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-plan-full-"));
  fs.mkdirSync(path.join(tempRoot, "artifacts", "implementation-plans"), { recursive: true });

  // init
  const init = runCli(tempRoot, ["init-plan", "--feature", "demo"]);
  assert.equal(init.ok, true);
  assert.equal(init.revision, 1);

  // payload example with parsed JSON body
  const ex = runCli(tempRoot, [
    "add-plan-example", "--feature", "demo",
    "--label", "Search request",
    "--type", "request",
    "--body", '{"pagination":{"page":1},"filters":{"audit":["label"]}}'
  ]);
  assert.equal(ex.ok, true);
  assert.equal(ex.total, 1);
  assert.deepEqual(ex.entry.body, { pagination: { page: 1 }, filters: { audit: ["label"] } });

  // validation boundary
  const vb = runCli(tempRoot, [
    "add-plan-validation", "--feature", "demo",
    "--rule", "perPage <= 40",
    "--boundary", "IssuesService",
    "--reason", "Existing runtime guard"
  ]);
  assert.equal(vb.ok, true);
  assert.equal(vb.total, 1);

  // model with inline fields
  const model = runCli(tempRoot, [
    "add-plan-model", "--feature", "demo",
    "--qualified-name", "com.example.Issue",
    "--type", "record",
    "--status", "modified",
    "--justification", "Orchestration entity per constitution Principle 2",
    "--annotations", "@Builder",
    "--fields", '[{"name":"id","type":"long"},{"name":"labels","type":"List<String>","nullable":false,"defensiveCopy":true}]',
    "--notes", "Defensive copy on labels"
  ]);
  assert.equal(model.ok, true);
  assert.equal(model.totalModels, 1);
  assert.equal(model.entry.fields.length, 2);
  assert.equal(model.entry.justification, "Orchestration entity per constitution Principle 2");

  // add field to existing model
  const field = runCli(tempRoot, [
    "add-plan-model-field", "--feature", "demo",
    "--model", "com.example.Issue",
    "--name", "changeSets",
    "--type", "List<ChangeSet>",
    "--nullable",
    "--defensive-copy"
  ]);
  assert.equal(field.ok, true);
  assert.equal(field.field.nullable, true);
  assert.equal(field.field.defensiveCopy, true);

  // enum model
  runCli(tempRoot, [
    "add-plan-model", "--feature", "demo",
    "--qualified-name", "com.example.AuditType",
    "--type", "enum",
    "--status", "new",
    "--justification", "Domain concept in orchestration",
    "--values", "LABEL",
    "--methods", "String value(),static AuditType fromValue(String raw)"
  ]);

  // classes
  runCli(tempRoot, [
    "add-plan-class", "--feature", "demo",
    "--path", "src/main/java/IssuesService.java",
    "--status", "modified",
    "--role", "Search with audit"
  ]);
  runCli(tempRoot, [
    "add-plan-class", "--feature", "demo",
    "--path", "src/main/java/IssuesController.java",
    "--status", "modified",
    "--role", "Delegate search input"
  ]);

  // composition
  const comp = runCli(tempRoot, [
    "set-plan-composition", "--feature", "demo",
    "--approach", "dependent-then-parallel",
    "--description", "Search first, then parallel enrichment"
  ]);
  assert.equal(comp.compositionStrategy.approach, "dependent-then-parallel");

  // shared infra
  const infra = runCli(tempRoot, [
    "set-plan-infra", "--feature", "demo",
    "--reused", "AsyncComposer,GitLabExceptionMapper"
  ]);
  assert.deepEqual(infra.sharedInfra.reused, ["AsyncComposer", "GitLabExceptionMapper"]);

  // slice with inline tests and logging
  const slice = runCli(tempRoot, [
    "add-plan-slice", "--feature", "demo",
    "--id", "1",
    "--title", "Orchestration models",
    "--goal", "Add SearchIssuesInput and wire service",
    "--files", "SearchIssuesInput.java,IssuesService.java",
    "--unit-test", "IssuesServiceTest: search with audit",
    "--unit-test", "IssuesServiceTest: search without audit",
    "--component-test", "IssuesApiComponentTest: audit returns changeSets",
    "--info-log", "IssuesService logs audit types",
    "--warn-log", "None",
    "--error-log", "None"
  ]);
  assert.equal(slice.ok, true);
  assert.equal(slice.entry.tests.unit.length, 2);
  assert.equal(slice.entry.tests.component.length, 1);
  assert.equal(slice.entry.logging.info, "IssuesService logs audit types");

  // add test to slice incrementally
  runCli(tempRoot, [
    "add-plan-slice-test", "--feature", "demo",
    "--slice", "1",
    "--level", "unit",
    "--test", "IssuesServiceTest: failure propagation"
  ]);

  // set logging on slice
  runCli(tempRoot, [
    "set-plan-slice-logging", "--feature", "demo",
    "--slice", "1",
    "--error", "IssuesService logs enrichment failure"
  ]);

  // testing matrix
  runCli(tempRoot, [
    "add-plan-test", "--feature", "demo",
    "--level", "Unit", "--required", "--coverage", "IssuesServiceTest"
  ]);
  runCli(tempRoot, [
    "add-plan-test", "--feature", "demo",
    "--level", "Component", "--required", "--coverage", "IssuesApiComponentTest"
  ]);

  // karate
  const karate = runCli(tempRoot, [
    "set-plan-karate", "--feature", "demo",
    "--feature-file", "src/test/karate/resources/issues/search-audit.feature",
    "--scenario", "Search with label audit",
    "--scenario", "Search without audit",
    "--smoke-tagged"
  ]);
  assert.equal(karate.karate.smokeTagged, true);
  assert.equal(karate.karate.scenarios.length, 2);

  // archunit
  const arch = runCli(tempRoot, [
    "set-plan-archunit", "--feature", "demo",
    "--existing-reviewed"
  ]);
  assert.equal(arch.archUnit.existingRulesReviewed, true);
  assert.deepEqual(arch.archUnit.newRules, []);

  // validate — should pass
  const valid = runCli(tempRoot, ["validate-plan", "--feature", "demo"]);
  assert.equal(valid.valid, true);
  assert.deepEqual(valid.issues, []);

  // plan-summary
  const summary = runCli(tempRoot, ["plan-summary", "--feature", "demo"]);
  assert.equal(summary.modelCount, 2);
  assert.equal(summary.classCount, 2);
  assert.equal(summary.sliceCount, 1);
  assert.equal(summary.exampleCount, 1);
  assert.equal(summary.hasComposition, true);
  assert.equal(summary.hasKarate, true);

  // plan-get full
  const full = runCli(tempRoot, ["plan-get", "--feature", "demo"]);
  assert.ok(full.plan);
  assert.equal(full.plan.schemaVersion, "2.0");
  assert.equal(full.plan.models[0].fields.length, 3); // 2 inline + 1 added

  // plan-get section
  const modelsSection = runCli(tempRoot, ["plan-get", "--feature", "demo", "--section", "models"]);
  assert.equal(modelsSection.section, "models");
  assert.equal(modelsSection.data.length, 2);

  const slicesSection = runCli(tempRoot, ["plan-get", "--feature", "demo", "--section", "slices"]);
  assert.equal(slicesSection.data[0].tests.unit.length, 3);
  assert.equal(slicesSection.data[0].logging.error, "IssuesService logs enrichment failure");
});

test("flow-log plan: revise-plan clears all sections and bumps revision", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-plan-revise-"));
  fs.mkdirSync(path.join(tempRoot, "artifacts", "implementation-plans"), { recursive: true });

  runCli(tempRoot, ["init-plan", "--feature", "rev"]);
  runCli(tempRoot, [
    "add-plan-model", "--feature", "rev",
    "--qualified-name", "com.example.Foo", "--type", "record", "--status", "new",
    "--justification", "Test model",
    "--fields", '[{"name":"id","type":"long"}]'
  ]);
  runCli(tempRoot, [
    "add-plan-class", "--feature", "rev",
    "--path", "Foo.java", "--status", "new", "--role", "Test"
  ]);
  runCli(tempRoot, [
    "add-plan-slice", "--feature", "rev",
    "--id", "1", "--title", "Slice 1", "--goal", "Do something"
  ]);

  const revised = runCli(tempRoot, ["revise-plan", "--feature", "rev"]);
  assert.equal(revised.previousRevision, 1);
  assert.equal(revised.newRevision, 2);

  const summary = runCli(tempRoot, ["plan-summary", "--feature", "rev"]);
  assert.equal(summary.revision, 2);
  assert.equal(summary.modelCount, 0);
  assert.equal(summary.classCount, 0);
  assert.equal(summary.sliceCount, 0);
  assert.equal(summary.hasComposition, false);

  // validate should fail — all sections empty
  const invalid = runCli(tempRoot, ["validate-plan", "--feature", "rev"]);
  assert.equal(invalid.valid, false);
  assert.ok(invalid.issues.length >= 3);
});

test("flow-log plan: validate catches missing justification and empty record fields", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-plan-val-"));
  fs.mkdirSync(path.join(tempRoot, "artifacts", "implementation-plans"), { recursive: true });

  runCli(tempRoot, ["init-plan", "--feature", "val"]);

  // Model with whitespace-only justification (semantically empty)
  runCli(tempRoot, [
    "add-plan-model", "--feature", "val",
    "--qualified-name", "com.example.Empty", "--type", "record", "--status", "new",
    "--justification", "   "
  ]);
  runCli(tempRoot, [
    "add-plan-class", "--feature", "val",
    "--path", "Empty.java", "--status", "new"
  ]);
  runCli(tempRoot, [
    "add-plan-slice", "--feature", "val",
    "--id", "1", "--title", "S1", "--goal", "Goal"
  ]);

  const result = runCli(tempRoot, ["validate-plan", "--feature", "val"]);
  assert.equal(result.valid, false);
  // Should flag: missing justification + record with no fields
  assert.ok(result.issues.some((i) => i.includes("justification")));
  assert.ok(result.issues.some((i) => i.includes("no fields")));
});

test("flow-log plan: duplicate class updates in place", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-plan-dup-"));
  fs.mkdirSync(path.join(tempRoot, "artifacts", "implementation-plans"), { recursive: true });

  runCli(tempRoot, ["init-plan", "--feature", "dup-test"]);
  runCli(tempRoot, [
    "add-plan-class", "--feature", "dup-test",
    "--path", "MyService.java", "--status", "new", "--role", "Original role"
  ]);

  const updated = runCli(tempRoot, [
    "add-plan-class", "--feature", "dup-test",
    "--path", "MyService.java", "--status", "modified", "--role", "Updated role"
  ]);
  assert.equal(updated.totalClasses, 1);
  assert.equal(updated.entry.status, "modified");
  assert.equal(updated.entry.role, "Updated role");
});

test("flow-log plan: init-plan fails without --force, succeeds with --force", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-plan-force-"));
  fs.mkdirSync(path.join(tempRoot, "artifacts", "implementation-plans"), { recursive: true });

  runCli(tempRoot, ["init-plan", "--feature", "force-test"]);

  const result = spawnSync(process.execPath, [cliPath, "init-plan", "--feature", "force-test"], {
    cwd: tempRoot,
    encoding: "utf8"
  });
  assert.notEqual(result.status, 0);
  assert.ok(result.stderr.includes("Plan already exists"));

  const forced = runCli(tempRoot, ["init-plan", "--feature", "force-test", "--force"]);
  assert.equal(forced.ok, true);
  assert.equal(forced.revision, 1);
});

test("flow-log plan: slice with Java files but no tests triggers validation warning", () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "flow-log-plan-slicetest-"));
  fs.mkdirSync(path.join(tempRoot, "artifacts", "implementation-plans"), { recursive: true });

  runCli(tempRoot, ["init-plan", "--feature", "st"]);
  runCli(tempRoot, [
    "add-plan-model", "--feature", "st",
    "--qualified-name", "com.example.M", "--type", "enum", "--status", "new",
    "--justification", "Test", "--values", "A"
  ]);
  runCli(tempRoot, [
    "add-plan-class", "--feature", "st",
    "--path", "M.java", "--status", "new"
  ]);
  runCli(tempRoot, [
    "add-plan-slice", "--feature", "st",
    "--id", "1", "--title", "S1", "--goal", "Goal",
    "--files", "M.java,Service.java"
  ]);

  const result = runCli(tempRoot, ["validate-plan", "--feature", "st"]);
  assert.equal(result.valid, false);
  assert.ok(result.issues.some((i) => i.includes("no tests")));
});

function runCli(cwd, args) {
  const result = spawnSync(process.execPath, [cliPath, ...args], {
    cwd,
    encoding: "utf8"
  });

  if (result.status !== 0) {
    throw new Error(`CLI failed: ${result.stderr}`);
  }

  return JSON.parse(result.stdout);
}
