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

test("flow-log architectural risks reopen and escalation after 3 rounds", () => {
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

  // Round 2
  runCli(tempRoot, ["increment-round", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, [
    "respond-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "ADDRESSED",
    "--note", "Split into two independent calls", "--by", "JavaArchitect"
  ]);
  runCli(tempRoot, [
    "reopen-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--reason", "Still coupled via shared state",
    "--by", "ArchitectureReviewer"
  ]);

  // Round 3
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

  // Gate should ESCALATE (3 rounds, still unresolved HIGH)
  const gate = runCli(tempRoot, ["architecture-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "ESCALATE");
  assert.equal(gate.round, 3);
  assert.equal(gate.unresolvedBlocking, 1);
  assert.match(gate.message, /Escalate to user/);
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
