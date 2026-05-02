import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { createPlanTempRoot, createTempRoot, runCli, runCliRaw } from "./test-helpers.mjs";

test("flow-log records minimal workflow facts and reports signoff readiness", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");
  const storyPath = path.join(tempRoot, "story.md");
  const e2ePath = path.join(tempRoot, "demo.e2e.md");
  const planPath = path.join(tempRoot, "plan.md");

  fs.writeFileSync(storyPath, "# Story\n");
  fs.writeFileSync(e2ePath, "# E2E Scenarios\n");
  fs.writeFileSync(planPath, "# Plan\n");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, ["lock-requirements", "--feature", "demo", "--state-path", statePath, "--by", "TL"]);
  runCli(tempRoot, [
    "set-e2e-mode",
    "--feature",
    "demo",
    "--state-path",
    statePath,
    "--mode",
    "SCENARIOS_REQUIRED",
    "--by",
    "TL"
  ]);
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
    "e2e",
    "--feature",
    "demo",
    "--state-path",
    statePath,
    "--path",
    e2ePath
  ]);
  runCli(tempRoot, [
    "approve-artifact",
    "e2e",
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

test("flow-log signoff allows reuse-existing smoke without an e2e artifact", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");
  const storyPath = path.join(tempRoot, "story.md");
  const planPath = path.join(tempRoot, "plan.md");

  fs.writeFileSync(storyPath, "# Story\n");
  fs.writeFileSync(planPath, "# Plan\n");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, ["lock-requirements", "--feature", "demo", "--state-path", statePath, "--by", "TL"]);
  runCli(tempRoot, [
    "set-e2e-mode",
    "--feature",
    "demo",
    "--state-path",
    statePath,
    "--mode",
    "REUSE_EXISTING",
    "--by",
    "TL",
    "--reason",
    "Internal refactor only; existing smoke coverage is reused."
  ]);
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

  const status = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  const readiness = runCli(tempRoot, ["readiness", "signoff", "--feature", "demo", "--state-path", statePath]);

  assert.equal(status.status.e2eMode, "REUSE_EXISTING");
  assert.equal(readiness.readiness.checks.e2eMode, "REUSE_EXISTING");
  assert.equal(readiness.readiness.ready, true);
});

test("flow-log summary reports missing signoff requirements", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  const summary = runCli(tempRoot, ["summary", "--feature", "demo", "--state-path", statePath]);

  assert.equal(summary.summary.readiness.ready, false);
  assert.match(summary.summary.nextActions[0], /Requirements must be locked/);
});

test("flow-log status is compact and signoff-oriented", () => {
  const tempRoot = createTempRoot();
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
  const tempRoot = createTempRoot();
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

test("flow-log tracks slice-run lifecycle (start, complete, multiple runs)", () => {
  const tempRoot = createPlanTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  registerApprovedPlan(tempRoot, statePath, "demo", ["slice-1", "slice-2", "slice-3"]);

  const firstSliceRun = runCli(tempRoot, [
    "start-slice-run",
    "--feature", "demo",
    "--state-path", statePath,
    "--slice", "slice-1",
    "--type", "intermediate",
    "--by", "TL"
  ]);
  assert.equal(firstSliceRun.ok, true);
  assert.equal(firstSliceRun.sliceRun.run, 1);
  assert.equal(firstSliceRun.sliceRun.slice, "slice-1");
  assert.equal(firstSliceRun.sliceRun.type, "intermediate");
  assert.equal(firstSliceRun.sliceRun.status, "in-progress");

  const midStatus = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(midStatus.status.currentSliceRun, 1);
  assert.equal(midStatus.status.currentSlice, "slice-1");
  assert.equal(midStatus.status.currentSliceRunType, "intermediate");
  assert.equal(midStatus.status.totalSliceRuns, 1);

  const firstComplete = runCli(tempRoot, [
    "complete-slice-run",
    "--feature", "demo",
    "--state-path", statePath,
    "--status", "complete"
  ]);
  assert.equal(firstComplete.ok, true);
  assert.equal(firstComplete.completedSliceRun.run, 1);
  assert.equal(firstComplete.completedSliceRun.status, "complete");
  assert.equal(firstComplete.totalSliceRuns, 1);

  runCli(tempRoot, [
    "start-slice-run",
    "--feature", "demo",
    "--state-path", statePath,
    "--slice", "slice-3",
    "--type", "final",
    "--by", "TL"
  ]);
  runCli(tempRoot, [
    "complete-slice-run",
    "--feature", "demo",
    "--state-path", statePath
  ]);

  const summary = runCli(tempRoot, ["summary", "--feature", "demo", "--state-path", statePath]);
  assert.equal(summary.summary.sliceRuns.completed, 2);
  assert.equal(summary.summary.sliceRuns.total, 2);
  assert.equal(summary.summary.sliceRuns.current, null);
  assert.equal(summary.summary.events.counts.sliceRunStart, 2);
  assert.equal(summary.summary.events.counts.sliceRunEnd, 2);

  const history = runCli(tempRoot, ["history", "--feature", "demo", "--state-path", statePath]);
  assert.equal(history.events[0].type, "sliceRunStart");
  assert.equal(history.events[0].slice, "slice-1");
  assert.equal(history.events[0].sliceRunType, "intermediate");
  assert.equal(history.events[1].type, "sliceRunEnd");
  assert.equal(history.events[1].sliceRunStatus, "complete");
  assert.equal(history.events[1].sliceRunType, "intermediate");
});

test("flow-log summary exposes the active slice-run", () => {
  const tempRoot = createPlanTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  registerApprovedPlan(tempRoot, statePath, "demo", ["S1", "S2"]);
  runCli(tempRoot, [
    "start-slice-run",
    "--feature", "demo",
    "--state-path", statePath,
    "--slice", "S1",
    "--type", "intermediate",
    "--by", "TL"
  ]);

  const summary = runCli(tempRoot, ["summary", "--feature", "demo", "--state-path", statePath]);
  assert.equal(summary.summary.sliceRuns.current.run, 1);
  assert.equal(summary.summary.sliceRuns.current.slice, "S1");
  assert.equal(summary.summary.sliceRuns.current.type, "intermediate");
  assert.equal(summary.summary.sliceRuns.current.changedFileCount, 0);
  assert.deepEqual(summary.summary.sliceRuns.current.changedFiles, []);
});

test("flow-log start-slice-run requires one explicit approved slice id and handoff type", () => {
  const tempRoot = createPlanTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  registerApprovedPlan(tempRoot, statePath, "demo", ["S1", "S2"]);

  const missingSlice = runCliRaw(tempRoot, [
    "start-slice-run", "--feature", "demo", "--state-path", statePath
  ]);
  assert.notEqual(missingSlice.status, 0);
  assert.match(missingSlice.stderr, /Missing required flag: --slice/i);

  const missingType = runCliRaw(tempRoot, [
    "start-slice-run", "--feature", "demo", "--state-path", statePath, "--slice", "S1"
  ]);
  assert.notEqual(missingType.status, 0);
  assert.match(missingType.stderr, /Missing required flag: --type/i);

  const unknownType = runCliRaw(tempRoot, [
    "start-slice-run", "--feature", "demo", "--state-path", statePath, "--slice", "S1", "--type", "review"
  ]);
  assert.notEqual(unknownType.status, 0);
  assert.match(unknownType.stderr, /slice-run type must be one of/i);

  const unknownSlice = runCliRaw(tempRoot, [
    "start-slice-run", "--feature", "demo", "--state-path", statePath, "--slice", "S404", "--type", "intermediate"
  ]);
  assert.notEqual(unknownSlice.status, 0);
  assert.match(unknownSlice.stderr, /Unknown approved slice id: S404/);
});

test("flow-log start-slice-run rejects a second in-progress slice-run", () => {
  const tempRoot = createPlanTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  registerApprovedPlan(tempRoot, statePath, "demo", ["S1", "S2"]);
  runCli(tempRoot, [
    "start-slice-run", "--feature", "demo", "--state-path", statePath, "--slice", "S1", "--type", "intermediate", "--by", "TL"
  ]);

  const secondStart = runCliRaw(tempRoot, [
    "start-slice-run", "--feature", "demo", "--state-path", statePath, "--slice", "S2", "--type", "intermediate", "--by", "TL"
  ]);
  assert.notEqual(secondStart.status, 0);
  assert.match(secondStart.stderr, /already in progress/i);
});

test("flow-log start-slice-run rejects stale approved plan after direct file edits even when stored hash is unchanged", () => {
  const tempRoot = createPlanTempRoot();
  const statePath = path.join(tempRoot, "feature.json");
  const feature = "demo";
  const planPath = path.join(tempRoot, "artifacts", "implementation-plans", `${feature}.plan.json`);

  runCli(tempRoot, ["create", "--feature", feature, "--state-path", statePath]);

  const plan = buildValidV4Plan(feature, ["S1"]);
  plan.hash = "persisted-hash";
  fs.writeFileSync(planPath, `${JSON.stringify(plan, null, 2)}\n`);

  runCli(tempRoot, [
    "register-artifact",
    "plan",
    "--feature",
    feature,
    "--state-path",
    statePath,
    "--path",
    planPath
  ]);
  runCli(tempRoot, [
    "approve-artifact",
    "plan",
    "--feature",
    feature,
    "--state-path",
    statePath,
    "--by",
    "TL"
  ]);

  plan.slices[0].title = "Changed directly after approval";
  fs.writeFileSync(planPath, `${JSON.stringify(plan, null, 2)}\n`);

  const start = runCliRaw(tempRoot, [
    "start-slice-run",
    "--feature",
    feature,
    "--state-path",
    statePath,
    "--slice",
    "S1",
    "--type",
    "intermediate",
    "--by",
    "TL"
  ]);
  assert.notEqual(start.status, 0);
  assert.match(start.stderr, /Plan approval is stale/i);
});

test("flow-log summary exposes changed files for review intake", () => {
  const tempRoot = createPlanTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  registerApprovedPlan(tempRoot, statePath, "demo", ["S1"]);
  runCli(tempRoot, [
    "start-slice-run",
    "--feature", "demo",
    "--state-path", statePath,
    "--slice", "S1",
    "--type", "intermediate",
    "--by", "TL"
  ]);
  runCli(tempRoot, [
    "add-change",
    "--feature", "demo",
    "--state-path", statePath,
    "--file", "flow-orchestrator/src/main/java/com/example/Demo.java",
    "--file", "flow-orchestrator/src/test/java/com/example/DemoTest.java"
  ]);

  const summary = runCli(tempRoot, ["summary", "--feature", "demo", "--state-path", statePath]);
  assert.equal(summary.summary.changedFileCount, 2);
  assert.deepEqual(summary.summary.changedFiles, [
    "flow-orchestrator/src/main/java/com/example/Demo.java",
    "flow-orchestrator/src/test/java/com/example/DemoTest.java"
  ]);
  assert.equal(summary.summary.sliceRuns.current.changedFileCount, 2);
  assert.deepEqual(summary.summary.sliceRuns.current.changedFiles, [
    "flow-orchestrator/src/main/java/com/example/Demo.java",
    "flow-orchestrator/src/test/java/com/example/DemoTest.java"
  ]);
});

test("flow-log complete-slice-run preserves changed file ownership in history", () => {
  const tempRoot = createPlanTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  registerApprovedPlan(tempRoot, statePath, "demo", ["S1"]);
  runCli(tempRoot, [
    "start-slice-run",
    "--feature", "demo",
    "--state-path", statePath,
    "--slice", "S1",
    "--type", "intermediate",
    "--by", "TL"
  ]);
  runCli(tempRoot, [
    "add-change",
    "--feature", "demo",
    "--state-path", statePath,
    "--file", "flow-orchestrator/src/main/java/com/example/Demo.java"
  ]);
  runCli(tempRoot, [
    "complete-slice-run",
    "--feature", "demo",
    "--state-path", statePath,
    "--status", "complete"
  ]);

  const state = runCli(tempRoot, ["get", "--feature", "demo", "--state-path", statePath]);
  assert.equal(state.state.sliceRuns.history.length, 1);
  assert.deepEqual(state.state.sliceRuns.history[0].changedFiles, [
    "flow-orchestrator/src/main/java/com/example/Demo.java"
  ]);
});

test("flow-log story-get returns the External Contracts section from the approved story", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");
  const storyPath = path.join(tempRoot, "artifacts", "user-stories", "demo.story.md");

  fs.mkdirSync(path.dirname(storyPath), { recursive: true });
  fs.writeFileSync(storyPath, `# Story\n\n## Business Goal\n\nGoal.\n\n## External Contracts\n\n### Client Contract\n\n- Operation or endpoint: POST /api/milestones/search\n- Required request shape notes: body may be omitted\n\n### Upstream / GitLab Contract\n\n- Operation or endpoint: GET /projects/:id/milestones\n\n## Scope\n\n### In Scope\n\n- milestone search\n`);

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
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

  const storySection = runCli(tempRoot, [
    "story-get",
    "--feature",
    "demo",
    "--state-path",
    statePath,
    "--section",
    "external-contracts"
  ]);

  assert.equal(storySection.command, "story-get");
  assert.equal(storySection.section, "external-contracts");
  assert.match(storySection.content, /POST \/api\/milestones\/search/);
  assert.match(storySection.content, /GET \/projects\/:id\/milestones/);
});

test("flow-log story-get rejects stale approved stories", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");
  const storyPath = path.join(tempRoot, "artifacts", "user-stories", "demo.story.md");

  fs.mkdirSync(path.dirname(storyPath), { recursive: true });
  fs.writeFileSync(storyPath, "# Story\n\n## External Contracts\n\n- initial\n");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
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

  fs.writeFileSync(storyPath, "# Story\n\n## External Contracts\n\n- changed after approval\n");

  const storyGet = runCliRaw(tempRoot, [
    "story-get",
    "--feature",
    "demo",
    "--state-path",
    statePath,
    "--section",
    "external-contracts"
  ]);
  assert.notEqual(storyGet.status, 0);
  assert.match(storyGet.stderr, /stale/i);

  const status = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(status.status.storyStale, true);
});

test("flow-log add-risk requires a plan-ref", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, ["increment-round", "--feature", "demo", "--state-path", statePath]);

  const result = runCliRaw(tempRoot, [
    "add-risk",
    "--feature",
    "demo",
    "--state-path",
    statePath,
    "--description",
    "Missing slice reference",
    "--suggested-fix",
    "Target the slice explicitly",
    "--by",
    "ArchitectureReviewer"
  ]);

  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /plan-ref/i);
});

test("flow-log reset-checks clears all checks and records red card event", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
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

  let status = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(status.status.finalCheck, "PASS");
  assert.equal(status.status.karate, "PASS");

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

  status = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(status.status.finalCheck, "NOT_RUN");
  assert.equal(status.status.karate, "NOT_RUN");
});

test("flow-log lock-requirements stores request source in state and summary", () => {
  const tempRoot = createTempRoot();
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
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  const raw = JSON.parse(fs.readFileSync(statePath, "utf8"));
  assert.ok(raw.timing.startedAt);
  assert.equal(raw.timing.completedAt, null);
  assert.equal(raw.timing.durationMinutes, null);

  const result = runCli(tempRoot, ["complete", "--feature", "demo", "--state-path", statePath]);
  assert.equal(result.ok, true);
  assert.ok(result.timing.completedAt);
  assert.equal(typeof result.timing.durationMinutes, "number");

  const summary = runCli(tempRoot, ["summary", "--feature", "demo", "--state-path", statePath]);
  assert.ok(summary.summary.timing.startedAt);
  assert.ok(summary.summary.timing.completedAt);
});

test("flow-log architectural risks full lifecycle: add, respond, resolve/reopen, gate", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, ["increment-round", "--feature", "demo", "--state-path", statePath]);

  const firstRisk = runCli(tempRoot, [
    "add-risk", "--feature", "demo", "--state-path", statePath,
    "--severity", "CRITICAL", "--description", "Missing interface contract for port",
    "--plan-ref", "S1",
    "--by", "ArchitectureReviewer"
  ]);
  assert.equal(firstRisk.risk.id, 1);
  assert.equal(firstRisk.risk.severity, "CRITICAL");
  assert.equal(firstRisk.risk.status, "OPEN");
  assert.equal(firstRisk.risk.round, 1);

  runCli(tempRoot, [
    "add-risk", "--feature", "demo", "--state-path", statePath,
    "--severity", "MEDIUM", "--description", "Naming could be clearer",
    "--plan-ref", "S1",
    "--by", "ArchitectureReviewer"
  ]);

  let gate = runCli(tempRoot, ["architecture-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "FAIL");
  assert.equal(gate.unresolvedBlocking, 1);

  const response = runCli(tempRoot, [
    "respond-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "ADDRESSED",
    "--note", "Added PortInterface with full contract in plan section 5",
    "--by", "JavaArchitect"
  ]);
  assert.equal(response.risk.status, "ADDRESSED");
  assert.equal(response.risk.responseNote, "Added PortInterface with full contract in plan section 5");

  const resolved = runCli(tempRoot, [
    "resolve-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--by", "ArchitectureReviewer"
  ]);
  assert.equal(resolved.risk.status, "RESOLVED");

  gate = runCli(tempRoot, ["architecture-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "PASS");
  assert.equal(gate.unresolvedBlocking, 0);

  const summary = runCli(tempRoot, ["summary", "--feature", "demo", "--state-path", statePath]);
  assert.equal(summary.summary.architecturalRisks.total, 2);
  assert.equal(summary.summary.architecturalRisks.round, 1);
  assert.equal(summary.summary.architecturalRisks.byStatus.RESOLVED, 1);
  assert.equal(summary.summary.architecturalRisks.byStatus.OPEN, 1);
  assert.equal(summary.summary.architecturalRisks.risks[0].responseNote, "Added PortInterface with full contract in plan section 5");

  const status = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(status.status.architectureGate, "PASS");
  assert.equal(status.status.architectureReviewRound, 1);
});

test("flow-log add-risk stores suggestedFix and surfaces it in summary", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, ["increment-round", "--feature", "demo", "--state-path", statePath]);

  const highRisk = runCli(tempRoot, [
    "add-risk", "--feature", "demo", "--state-path", statePath,
    "--severity", "HIGH",
    "--description", "Missing fail-fast on rejected execution",
    "--suggested-fix", "Add CallerRunsPolicy or throw ServiceUnavailableException from a RejectedExecutionHandler",
    "--plan-ref", "S1",
    "--by", "ArchitectureReviewer"
  ]);
  assert.equal(highRisk.risk.suggestedFix, "Add CallerRunsPolicy or throw ServiceUnavailableException from a RejectedExecutionHandler");
  assert.equal(highRisk.risk.status, "OPEN");

  const mediumRisk = runCli(tempRoot, [
    "add-risk", "--feature", "demo", "--state-path", statePath,
    "--severity", "MEDIUM",
    "--description", "Naming could be clearer",
    "--plan-ref", "S1",
    "--by", "ArchitectureReviewer"
  ]);
  assert.equal(mediumRisk.risk.suggestedFix, null);

  const summary = runCli(tempRoot, ["summary", "--feature", "demo", "--state-path", statePath]);
  assert.equal(summary.summary.architecturalRisks.risks[0].suggestedFix, "Add CallerRunsPolicy or throw ServiceUnavailableException from a RejectedExecutionHandler");
  assert.equal(summary.summary.architecturalRisks.risks[1].suggestedFix, null);
});

test("flow-log architectural risks reopen and escalation after 3 rounds", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  runCli(tempRoot, ["increment-round", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, [
    "add-risk", "--feature", "demo", "--state-path", statePath,
    "--severity", "HIGH", "--description", "Wrong composition strategy",
    "--plan-ref", "S1",
    "--by", "ArchitectureReviewer"
  ]);

  runCli(tempRoot, [
    "respond-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "ADDRESSED",
    "--note", "Switched to sequential", "--by", "JavaArchitect"
  ]);

  const reopened = runCli(tempRoot, [
    "reopen-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--reason", "Sequential still wrong — data dependency is bidirectional",
    "--by", "ArchitectureReviewer"
  ]);
  assert.equal(reopened.risk.status, "REOPENED");

  for (let round = 2; round <= 2; round += 1) {
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

  const gate = runCli(tempRoot, ["architecture-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "ESCALATE");
  assert.equal(gate.round, 3);
  assert.equal(gate.unresolvedBlocking, 1);
  assert.ok(gate.unresolvedRisks.length === 1);
  assert.equal(gate.unresolvedRisks[0].severity, "HIGH");
  assert.match(gate.message, /TL must decide/);

  const decision = runCli(tempRoot, [
    "add-event", "--feature", "demo", "--state-path", statePath,
    "--type", "archEscalationDecision",
    "--decision", "PROCEED_TO_CODING",
    "--reason", "Risk #1 is artifact naming only, not a correctness blocker",
    "--by", "TL"
  ]);
  assert.equal(decision.event.type, "archEscalationDecision");
  assert.equal(decision.event.decision, "PROCEED_TO_CODING");

  assert.throws(
    () => runCli(tempRoot, ["increment-round", "--feature", "demo", "--state-path", statePath]),
    /hard cap reached/i
  );
});

test("flow-log architectural risks invalidation path", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, ["increment-round", "--feature", "demo", "--state-path", statePath]);

  runCli(tempRoot, [
    "add-risk", "--feature", "demo", "--state-path", statePath,
    "--severity", "HIGH", "--description", "Shared infra duplicated",
    "--plan-ref", "S1",
    "--by", "ArchitectureReviewer"
  ]);

  runCli(tempRoot, [
    "respond-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "INVALIDATED",
    "--note", "Uses SharedMapper which already exists in shared infra — not duplication",
    "--by", "JavaArchitect"
  ]);

  runCli(tempRoot, [
    "resolve-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--by", "ArchitectureReviewer"
  ]);

  const gate = runCli(tempRoot, ["architecture-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "PASS");
});

test("flow-log reclassify-risk changes severity and preserves previous", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, ["increment-round", "--feature", "demo", "--state-path", statePath]);

  // Reviewer adds risk without severity (defaults to UNCLASSIFIED)
  const added = runCli(tempRoot, [
    "add-risk", "--feature", "demo", "--state-path", statePath,
    "--description", "Shared infra duplicated — will cause runtime class conflicts in production",
    "--plan-ref", "S1",
    "--by", "ArchitectureReviewer"
  ]);
  assert.equal(added.risk.severity, "UNCLASSIFIED");

  // Gate blocks on UNCLASSIFIED risk
  let gate = runCli(tempRoot, ["architecture-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "FAIL");
  assert.equal(gate.unresolvedBlocking, 1);

  // TL classifies the risk
  const result = runCli(tempRoot, [
    "reclassify-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--severity", "HIGH",
    "--reason", "Genuine runtime failure risk", "--by", "TL"
  ]);

  assert.equal(result.risk.severity, "HIGH");
  assert.equal(result.risk.previousSeverity, "UNCLASSIFIED");
  assert.equal(result.risk.reclassifiedBy, "TL");
  assert.equal(result.risk.reclassificationReason, "Genuine runtime failure risk");
  assert.ok(result.risk.reclassifiedAt);

  // Gate should still FAIL because of unresolved HIGH risk
  gate = runCli(tempRoot, ["architecture-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "FAIL");
  assert.equal(gate.unresolvedBlocking, 1);
});

test("flow-log reclassify-risk to LOW makes risk non-blocking", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, ["increment-round", "--feature", "demo", "--state-path", statePath]);

  runCli(tempRoot, [
    "add-risk", "--feature", "demo", "--state-path", statePath,
    "--description", "Naming convention mismatch — advisory only",
    "--plan-ref", "S1",
    "--by", "ArchitectureReviewer"
  ]);

  // TL classifies as LOW — not blocking
  runCli(tempRoot, [
    "reclassify-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--severity", "LOW",
    "--reason", "Naming preference, not a correctness issue", "--by", "TL"
  ]);

  const gate = runCli(tempRoot, ["architecture-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "PASS");
  assert.equal(gate.unresolvedBlocking, 0);
  assert.equal(gate.undecidedNonBlocking, 1);
});

test("flow-log decide-risk records explicit non-blocking architecture debt", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, ["increment-round", "--feature", "demo", "--state-path", statePath]);

  runCli(tempRoot, [
    "add-risk", "--feature", "demo", "--state-path", statePath,
    "--severity", "MEDIUM", "--description", "Logging context could be richer",
    "--plan-ref", "S1", "--by", "ArchitectureReviewer"
  ]);

  const decision = runCli(tempRoot, [
    "decide-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "ACCEPTED",
    "--reason", "Acceptable for this release; not worth another plan loop.",
    "--follow-up", "cleanup-story", "--by", "TL"
  ]);

  assert.equal(decision.risk.status, "ACCEPTED");
  assert.equal(decision.risk.decisionReason, "Acceptable for this release; not worth another plan loop.");
  assert.equal(decision.risk.followUpRef, "cleanup-story");

  const gate = runCli(tempRoot, ["architecture-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "PASS");
  assert.equal(gate.accepted, 1);
  assert.equal(gate.undecidedNonBlocking, 0);

  const summary = runCli(tempRoot, ["summary", "--feature", "demo", "--state-path", statePath]);
  assert.equal(summary.summary.architecturalRisks.byStatus.ACCEPTED, 1);
  assert.equal(summary.summary.architecturalRisks.debt.accepted, 1);
  assert.equal(summary.summary.architecturalRisks.risks[0].decisionReason, "Acceptable for this release; not worth another plan loop.");

  const invalid = runCliRaw(tempRoot, [
    "add-risk", "--feature", "demo", "--state-path", statePath,
    "--severity", "HIGH", "--description", "Blocking defect",
    "--plan-ref", "S1", "--by", "ArchitectureReviewer"
  ]);
  assert.equal(invalid.status, 0);

  const invalidDecision = runCliRaw(tempRoot, [
    "decide-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "2", "--status", "DEFERRED",
    "--reason", "Should not be allowed", "--by", "TL"
  ]);
  assert.notEqual(invalidDecision.status, 0);
  assert.match(invalidDecision.stderr, /only MEDIUM or LOW risks can be ACCEPTED or DEFERRED/i);
});

test("flow-log code findings full lifecycle: add, respond, resolve/reopen, gate", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, ["increment-code-review-round", "--feature", "demo", "--state-path", statePath]);

  const firstFinding = runCli(tempRoot, [
    "add-finding", "--feature", "demo", "--state-path", statePath,
    "--severity", "HIGH", "--description", "Null check missing on mapper input",
    "--file", "src/main/java/Mapper.java",
    "--by", "CodeReviewer"
  ]);
  assert.equal(firstFinding.finding.id, 1);
  assert.equal(firstFinding.finding.severity, "HIGH");
  assert.equal(firstFinding.finding.status, "OPEN");
  assert.equal(firstFinding.finding.file, "src/main/java/Mapper.java");
  assert.equal(firstFinding.finding.round, 1);

  runCli(tempRoot, [
    "add-finding", "--feature", "demo", "--state-path", statePath,
    "--severity", "LOW", "--description", "Consider extracting constant",
    "--by", "CodeReviewer"
  ]);

  let gate = runCli(tempRoot, ["code-review-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "FAIL");
  assert.equal(gate.unresolvedBlocking, 1);

  const response = runCli(tempRoot, [
    "respond-finding", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "FIXED",
    "--note", "Added Objects.requireNonNull with descriptive message",
    "--by", "JavaCoder"
  ]);
  assert.equal(response.finding.status, "FIXED");
  assert.equal(response.finding.responseNote, "Added Objects.requireNonNull with descriptive message");

  const resolved = runCli(tempRoot, [
    "resolve-finding", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--by", "CodeReviewer"
  ]);
  assert.equal(resolved.finding.status, "RESOLVED");

  gate = runCli(tempRoot, ["code-review-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "PASS");
  assert.equal(gate.unresolvedBlocking, 0);

  const summary = runCli(tempRoot, ["summary", "--feature", "demo", "--state-path", statePath]);
  assert.equal(summary.summary.codeFindings.total, 2);
  assert.equal(summary.summary.codeFindings.round, 1);
  assert.equal(summary.summary.codeFindings.byStatus.RESOLVED, 1);
  assert.equal(summary.summary.codeFindings.byStatus.OPEN, 1);
  assert.equal(summary.summary.codeFindings.findings[0].file, "src/main/java/Mapper.java");

  const status = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(status.status.codeReviewGate, "PASS");
  assert.equal(status.status.codeReviewRound, 1);
});

test("flow-log readiness signoff requires explicit decisions for non-blocking review debt", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");
  const storyPath = path.join(tempRoot, "story.md");
  const planPath = path.join(tempRoot, "plan.md");

  fs.writeFileSync(storyPath, "# Story\n");
  fs.writeFileSync(planPath, "# Plan\n");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, ["lock-requirements", "--feature", "demo", "--state-path", statePath, "--by", "TL"]);
  runCli(tempRoot, [
    "set-e2e-mode", "--feature", "demo", "--state-path", statePath,
    "--mode", "REUSE_EXISTING", "--by", "TL"
  ]);
  runCli(tempRoot, [
    "register-artifact", "story", "--feature", "demo", "--state-path", statePath,
    "--path", storyPath
  ]);
  runCli(tempRoot, [
    "approve-artifact", "story", "--feature", "demo", "--state-path", statePath,
    "--by", "TL"
  ]);
  runCli(tempRoot, [
    "register-artifact", "plan", "--feature", "demo", "--state-path", statePath,
    "--path", planPath
  ]);
  runCli(tempRoot, [
    "approve-artifact", "plan", "--feature", "demo", "--state-path", statePath,
    "--by", "TL"
  ]);
  runCli(tempRoot, [
    "set-review", "--feature", "demo", "--state-path", statePath,
    "--name", "architectureReview", "--status", "PASS", "--by", "Reviewer"
  ]);
  runCli(tempRoot, [
    "set-review", "--feature", "demo", "--state-path", statePath,
    "--name", "codeReview", "--status", "PASS", "--by", "Reviewer"
  ]);
  runCli(tempRoot, [
    "set-check", "--feature", "demo", "--state-path", statePath,
    "--name", "finalCheck", "--status", "PASS", "--by", "TL"
  ]);
  runCli(tempRoot, [
    "set-check", "--feature", "demo", "--state-path", statePath,
    "--name", "karate", "--status", "PASS", "--by", "TL"
  ]);
  runCli(tempRoot, ["increment-round", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, [
    "add-risk", "--feature", "demo", "--state-path", statePath,
    "--severity", "MEDIUM", "--description", "Minor naming debt",
    "--plan-ref", "S1", "--by", "ArchitectureReviewer"
  ]);
  runCli(tempRoot, ["increment-code-review-round", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, [
    "add-finding", "--feature", "demo", "--state-path", statePath,
    "--severity", "LOW", "--description", "Extract constant", "--by", "CodeReviewer"
  ]);

  let readiness = runCli(tempRoot, [
    "readiness", "signoff", "--feature", "demo", "--state-path", statePath
  ]);
  assert.equal(readiness.readiness.ready, false);
  assert.ok(readiness.readiness.reasons.some((reason) => reason.includes("Non-blocking architecture risks still require TL decision")));
  assert.ok(readiness.readiness.reasons.some((reason) => reason.includes("Non-blocking code findings still require TL decision")));

  const statusBefore = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(statusBefore.status.phase, "review-debt");

  runCli(tempRoot, [
    "decide-risk", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "ACCEPTED",
    "--reason", "Accept naming debt for this release.", "--by", "TL"
  ]);
  runCli(tempRoot, [
    "decide-finding", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "DEFERRED",
    "--reason", "Track in cleanup story.", "--follow-up", "cleanup-story", "--by", "TL"
  ]);

  readiness = runCli(tempRoot, [
    "readiness", "signoff", "--feature", "demo", "--state-path", statePath
  ]);
  assert.equal(readiness.readiness.ready, true);

  const summary = runCli(tempRoot, ["summary", "--feature", "demo", "--state-path", statePath]);
  assert.equal(summary.summary.architecturalRisks.byStatus.ACCEPTED, 1);
  assert.equal(summary.summary.codeFindings.byStatus.DEFERRED, 1);
  assert.equal(summary.summary.architecturalRisks.debt.accepted, 1);
  assert.equal(summary.summary.codeFindings.debt.deferred, 1);
  assert.equal(summary.summary.codeFindings.findings[0].followUpRef, "cleanup-story");
});

test("flow-log code findings reopen and escalation after 3 rounds", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  runCli(tempRoot, ["increment-code-review-round", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, [
    "add-finding", "--feature", "demo", "--state-path", statePath,
    "--severity", "CRITICAL", "--description", "SQL injection via string concatenation",
    "--file", "src/main/java/Repository.java",
    "--by", "CodeReviewer"
  ]);

  runCli(tempRoot, [
    "respond-finding", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "FIXED",
    "--note", "Switched to parameterized query", "--by", "JavaCoder"
  ]);

  const reopened = runCli(tempRoot, [
    "reopen-finding", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--reason", "Only fixed one of three query methods",
    "--by", "CodeReviewer"
  ]);
  assert.equal(reopened.finding.status, "REOPENED");

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

  const gate = runCli(tempRoot, ["code-review-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "ESCALATE");
  assert.equal(gate.round, 3);
  assert.equal(gate.unresolvedBlocking, 1);
  assert.match(gate.message, /Escalate to user/);
});

test("flow-log code findings disputed path", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, ["increment-code-review-round", "--feature", "demo", "--state-path", statePath]);

  runCli(tempRoot, [
    "add-finding", "--feature", "demo", "--state-path", statePath,
    "--severity", "HIGH", "--description", "Missing error handling on HTTP call",
    "--by", "CodeReviewer"
  ]);

  runCli(tempRoot, [
    "respond-finding", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--status", "DISPUTED",
    "--note", "FeignErrorDecoder in SharedInfra already handles all 4xx/5xx — test in ErrorDecoderTest",
    "--by", "JavaCoder"
  ]);

  runCli(tempRoot, [
    "resolve-finding", "--feature", "demo", "--state-path", statePath,
    "--id", "1", "--by", "CodeReviewer"
  ]);

  const gate = runCli(tempRoot, ["code-review-gate", "--feature", "demo", "--state-path", statePath]);
  assert.equal(gate.gate, "PASS");
});

test("flow-log path resilience: refuses to run from wrong directory without --state-path", () => {
  const wrongDir = createTempRoot("flow-log-wrong-dir-");
  const result = runCliRaw(wrongDir, ["summary", "--feature", "demo"]);
  assert.notEqual(result.status, 0);
  const stderr = JSON.parse(result.stderr);
  assert.match(stderr.error, /repository root/);
  assert.match(stderr.hint, /repository root/);
});

test("flow-log friendly errors: state file not found includes hint", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "nonexistent.json");
  const result = runCliRaw(tempRoot, [
    "summary", "--feature", "demo", "--state-path", statePath
  ]);
  assert.notEqual(result.status, 0);
  const stderr = JSON.parse(result.stderr);
  assert.match(stderr.error, /State file does not exist/);
  assert.match(stderr.hint, /create --feature/);
});

test("flow-log friendly errors: unknown command includes hint", () => {
  const tempRoot = createTempRoot();
  const result = runCliRaw(tempRoot, ["nonexistent-command"]);
  assert.notEqual(result.status, 0);
  const stderr = JSON.parse(result.stderr);
  assert.match(stderr.error, /Unknown command/);
  assert.match(stderr.hint, /help/);
});

test("flow-log friendly errors: missing required flag includes hint", () => {
  const tempRoot = createTempRoot();
  const result = runCliRaw(tempRoot, ["create"]);
  assert.notEqual(result.status, 0);
  const stderr = JSON.parse(result.stderr);
  assert.match(stderr.error, /Missing required flag/);
  assert.match(stderr.hint, /help/);
});

test("flow-log run-check executes script and records PASS on exit 0", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");
  const scriptPath = path.join(tempRoot, "pass-script.sh");

  fs.writeFileSync(scriptPath, "#!/usr/bin/env bash\necho 'All checks passed'\nexit 0\n", { mode: 0o755 });
  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  const result = runCli(tempRoot, [
    "run-check", "--feature", "demo", "--state-path", statePath,
    "--name", "finalCheck", "--command", scriptPath, "--by", "JavaCoder"
  ]);

  assert.equal(result.ok, true);
  assert.equal(result.status, "PASS");
  assert.equal(result.exitCode, 0);
  assert.equal(result.check, "finalCheck");
  assert.ok(result.durationMs >= 0);
  assert.ok(Array.isArray(result.outputTail));
  assert.ok(result.outputTail.some(line => line.includes("All checks passed")));

  const status = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(status.status.finalCheck, "PASS");
});

test("flow-log run-check records FAIL on non-zero exit", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");
  const scriptPath = path.join(tempRoot, "fail-script.sh");

  fs.writeFileSync(scriptPath, "#!/usr/bin/env bash\necho 'Authorization: Bearer secret-token-value'\necho 'TOKEN=secret-value'\necho 'bash: warning: setlocale: LC_ALL: cannot change locale (en_US.UTF-8)'\necho 'Test failed: 3 errors'\nexit 1\n", { mode: 0o755 });
  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  const result = runCli(tempRoot, [
    "run-check", "--feature", "demo", "--state-path", statePath,
    "--name", "verifyQuick", "--command", scriptPath
  ]);

  assert.equal(result.ok, false);
  assert.equal(result.status, "FAIL");
  assert.equal(result.exitCode, 1);
  assert.ok(result.outputTail.some(line => line.includes("3 errors")));
  assert.ok(result.outputTail.some(line => line.includes("[REDACTED]")));
  assert.ok(!result.outputTail.some(line => line.includes("secret-token-value")));
  assert.ok(result.logPath);
  assert.ok(fs.existsSync(result.logPath));

  const persisted = fs.readFileSync(result.logPath, "utf8");
  assert.match(persisted, /Test failed: 3 errors/);
  assert.match(persisted, /\[REDACTED\]/);
  assert.doesNotMatch(persisted, /secret-token-value/);

  const logResult = runCli(tempRoot, [
    "check-log", "--feature", "demo", "--state-path", statePath,
    "--name", "verifyQuick", "--lines", "2"
  ]);
  assert.equal(logResult.check, "verifyQuick");
  assert.equal(logResult.status, "FAIL");
  assert.equal(logResult.logPath, result.logPath);
  assert.ok(logResult.lines.some(line => line.includes("3 errors")));
  assert.ok(logResult.lines.some(line => line.includes("[REDACTED]")));
  assert.ok(!logResult.lines.some(line => line.includes("setlocale")));

  const status = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(status.status.verifyQuick, "FAIL");

  const raw = JSON.parse(fs.readFileSync(statePath, "utf8"));
  assert.equal(raw.checks.verifyQuick.logPath, result.logPath);
});

test("flow-log run-check records FAIL on timeout", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");
  const scriptPath = path.join(tempRoot, "slow-script.sh");

  fs.writeFileSync(scriptPath, "#!/usr/bin/env bash\necho 'starting'\nsleep 30\n", { mode: 0o755 });
  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  const result = runCli(tempRoot, [
    "run-check", "--feature", "demo", "--state-path", statePath,
    "--name", "karate", "--command", scriptPath, "--timeout", "500"
  ]);

  assert.equal(result.ok, false);
  assert.equal(result.status, "FAIL");
  assert.equal(result.timedOut, true);
  assert.ok(result.logPath);
  assert.ok(fs.existsSync(result.logPath));

  const status = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(status.status.karate, "FAIL");
});

test("flow-log run-check uses default script when --command not provided", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  // Default script won't exist in temp dir, so it should fail — but the command path should be correct
  const result = runCli(tempRoot, [
    "run-check", "--feature", "demo", "--state-path", statePath,
    "--name", "finalCheck"
  ]);

  assert.equal(result.ok, false);
  assert.equal(result.status, "FAIL");
  assert.equal(result.command, "scripts/final-check.sh");
});

// --- Source fingerprint and staleness tests ---

test("flow-log run-check stores sourceFingerprint on PASS", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");
  const scriptPath = path.join(tempRoot, "pass.sh");

  // Create a fake flow-orchestrator/src tree so fingerprint is non-null
  const srcDir = path.join(tempRoot, "flow-orchestrator", "src");
  fs.mkdirSync(srcDir, { recursive: true });
  fs.writeFileSync(path.join(srcDir, "Main.java"), "class Main {}");

  fs.writeFileSync(scriptPath, "#!/usr/bin/env bash\nexit 0\n", { mode: 0o755 });
  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  const result = runCli(tempRoot, [
    "run-check", "--feature", "demo", "--state-path", statePath,
    "--name", "verifyQuick", "--command", scriptPath
  ]);

  assert.equal(result.status, "PASS");
  assert.ok(typeof result.sourceFingerprint === "string");
  assert.equal(result.sourceFingerprint.length, 16);

  // Fingerprint persisted in state
  const raw = JSON.parse(fs.readFileSync(statePath, "utf8"));
  assert.equal(raw.checks.verifyQuick.sourceFingerprint, result.sourceFingerprint);
});

test("flow-log run-check does not store sourceFingerprint on FAIL", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");
  const scriptPath = path.join(tempRoot, "fail.sh");

  const srcDir = path.join(tempRoot, "flow-orchestrator", "src");
  fs.mkdirSync(srcDir, { recursive: true });
  fs.writeFileSync(path.join(srcDir, "Main.java"), "class Main {}");

  fs.writeFileSync(scriptPath, "#!/usr/bin/env bash\nexit 1\n", { mode: 0o755 });
  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  const result = runCli(tempRoot, [
    "run-check", "--feature", "demo", "--state-path", statePath,
    "--name", "verifyQuick", "--command", scriptPath
  ]);

  assert.equal(result.status, "FAIL");
  assert.equal(result.sourceFingerprint, null);
});

test("flow-log status detects stale checks after source file changes", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");
  const scriptPath = path.join(tempRoot, "pass.sh");

  const srcDir = path.join(tempRoot, "flow-orchestrator", "src");
  fs.mkdirSync(srcDir, { recursive: true });
  const javaFile = path.join(srcDir, "Main.java");
  fs.writeFileSync(javaFile, "class Main {}");

  fs.writeFileSync(scriptPath, "#!/usr/bin/env bash\nexit 0\n", { mode: 0o755 });
  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  runCli(tempRoot, [
    "run-check", "--feature", "demo", "--state-path", statePath,
    "--name", "finalCheck", "--command", scriptPath
  ]);

  // Status right after PASS — should NOT be stale
  const freshStatus = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(freshStatus.status.finalCheck, "PASS");
  assert.equal(freshStatus.status.finalCheckStale, false);

  // Modify a source file to change the fingerprint
  // Need a small delay to ensure mtime changes (filesystem resolution)
  const originalMtime = fs.statSync(javaFile).mtimeMs;
  fs.writeFileSync(javaFile, "class Main { void changed() {} }");
  // Force different mtime if filesystem has low resolution
  const newMtime = fs.statSync(javaFile).mtimeMs;
  if (newMtime === originalMtime) {
    const futureTime = originalMtime + 1000;
    fs.utimesSync(javaFile, futureTime / 1000, futureTime / 1000);
  }

  // Status after change — should be stale
  const staleStatus = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(staleStatus.status.finalCheck, "PASS");
  assert.equal(staleStatus.status.finalCheckStale, true);
});

test("flow-log readiness signoff rejects stale verification evidence", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");
  const storyPath = path.join(tempRoot, "story.md");
  const planPath = path.join(tempRoot, "plan.md");
  const scriptPath = path.join(tempRoot, "pass.sh");

  const srcDir = path.join(tempRoot, "flow-orchestrator", "src");
  fs.mkdirSync(srcDir, { recursive: true });
  const javaFile = path.join(srcDir, "Main.java");
  fs.writeFileSync(javaFile, "class Main {}");

  fs.writeFileSync(storyPath, "# Story\n");
  fs.writeFileSync(planPath, "# Plan\n");
  fs.writeFileSync(scriptPath, "#!/usr/bin/env bash\nexit 0\n", { mode: 0o755 });

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);
  runCli(tempRoot, ["lock-requirements", "--feature", "demo", "--state-path", statePath, "--by", "TL"]);
  runCli(tempRoot, [
    "register-artifact", "story", "--feature", "demo", "--state-path", statePath, "--path", storyPath
  ]);
  runCli(tempRoot, [
    "approve-artifact", "story", "--feature", "demo", "--state-path", statePath, "--by", "TL"
  ]);
  runCli(tempRoot, [
    "register-artifact", "plan", "--feature", "demo", "--state-path", statePath, "--path", planPath
  ]);
  runCli(tempRoot, [
    "approve-artifact", "plan", "--feature", "demo", "--state-path", statePath, "--by", "TL"
  ]);
  runCli(tempRoot, [
    "set-review", "--feature", "demo", "--state-path", statePath,
    "--name", "architectureReview", "--status", "PASS", "--by", "Reviewer"
  ]);
  runCli(tempRoot, [
    "set-review", "--feature", "demo", "--state-path", statePath,
    "--name", "codeReview", "--status", "PASS", "--by", "Reviewer"
  ]);
  runCli(tempRoot, [
    "run-check", "--feature", "demo", "--state-path", statePath,
    "--name", "finalCheck", "--command", scriptPath
  ]);
  runCli(tempRoot, [
    "run-check", "--feature", "demo", "--state-path", statePath,
    "--name", "karate", "--command", scriptPath
  ]);

  const originalMtime = fs.statSync(javaFile).mtimeMs;
  fs.writeFileSync(javaFile, "class Main { void changed() {} }");
  const newMtime = fs.statSync(javaFile).mtimeMs;
  if (newMtime === originalMtime) {
    const futureTime = originalMtime + 1000;
    fs.utimesSync(javaFile, futureTime / 1000, futureTime / 1000);
  }

  const readiness = runCli(tempRoot, [
    "readiness", "signoff", "--feature", "demo", "--state-path", statePath
  ]);
  assert.equal(readiness.readiness.ready, false);
  assert.ok(readiness.readiness.reasons.some((reason) => reason.includes("finalCheck must be re-run")));
  assert.ok(readiness.readiness.reasons.some((reason) => reason.includes("karate must be re-run")));
});

test("flow-log status shows stale=false for NOT_RUN and FAIL checks", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  const srcDir = path.join(tempRoot, "flow-orchestrator", "src");
  fs.mkdirSync(srcDir, { recursive: true });
  fs.writeFileSync(path.join(srcDir, "Main.java"), "class Main {}");

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  // NOT_RUN checks should never be stale
  const status = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(status.status.verifyQuickStale, false);
  assert.equal(status.status.finalCheckStale, false);
  assert.equal(status.status.karateStale, false);
});

// --- verify tests ---

test("flow-log verify --profile full runs all checks and reports combined PASS", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  // Create pass scripts for all 3 checks
  const scriptsDir = path.join(tempRoot, "scripts");
  fs.mkdirSync(scriptsDir, { recursive: true });
  for (const name of ["verify-quick.sh", "final-check.sh", "karate-test.sh"]) {
    fs.writeFileSync(path.join(scriptsDir, name), "#!/usr/bin/env bash\nexit 0\n", { mode: 0o755 });
  }

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  const result = runCli(tempRoot, [
    "verify", "--feature", "demo", "--state-path", statePath, "--profile", "full", "--by", "JavaCoder"
  ]);

  assert.equal(result.ok, true);
  assert.equal(result.command, "verify");
  assert.equal(result.profile, "full");
  assert.equal(result.results.length, 3);
  assert.equal(result.results[0].check, "verifyQuick");
  assert.equal(result.results[0].status, "PASS");
  assert.equal(result.results[1].check, "finalCheck");
  assert.equal(result.results[1].status, "PASS");
  assert.equal(result.results[2].check, "karate");
  assert.equal(result.results[2].status, "PASS");

  // All checks should be PASS in state
  const status = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(status.status.verifyQuick, "PASS");
  assert.equal(status.status.finalCheck, "PASS");
  assert.equal(status.status.karate, "PASS");
});

test("flow-log verify --profile full stops on first failure and reports which check failed", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  // verify-quick passes, final-check fails
  const scriptsDir = path.join(tempRoot, "scripts");
  fs.mkdirSync(scriptsDir, { recursive: true });
  fs.writeFileSync(path.join(scriptsDir, "verify-quick.sh"), "#!/usr/bin/env bash\nexit 0\n", { mode: 0o755 });
  fs.writeFileSync(path.join(scriptsDir, "final-check.sh"), "#!/usr/bin/env bash\necho 'format error'\nexit 1\n", { mode: 0o755 });
  fs.writeFileSync(path.join(scriptsDir, "karate-test.sh"), "#!/usr/bin/env bash\nexit 0\n", { mode: 0o755 });

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  const result = runCli(tempRoot, [
    "verify", "--feature", "demo", "--state-path", statePath, "--profile", "full"
  ]);

  assert.equal(result.ok, false);
  assert.equal(result.stoppedAt, "finalCheck");
  assert.equal(result.results.length, 2);
  assert.equal(result.results[0].check, "verifyQuick");
  assert.equal(result.results[0].status, "PASS");
  assert.equal(result.results[1].check, "finalCheck");
  assert.equal(result.results[1].status, "FAIL");
  assert.ok(result.failedCheck.outputTail.some(line => line.includes("format error")));
  assert.ok(result.failedCheck.logPath);
  assert.ok(fs.existsSync(result.failedCheck.logPath));

  // karate should still be NOT_RUN since we stopped early
  const status = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(status.status.verifyQuick, "PASS");
  assert.equal(status.status.finalCheck, "FAIL");
  assert.equal(status.status.karate, "NOT_RUN");
});

// --- verify slice profile tests ---

test("flow-log verify --profile slice runs verifyQuick and finalCheck only", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  const scriptsDir = path.join(tempRoot, "scripts");
  fs.mkdirSync(scriptsDir, { recursive: true });
  for (const name of ["verify-quick.sh", "final-check.sh", "karate-test.sh"]) {
    fs.writeFileSync(path.join(scriptsDir, name), "#!/usr/bin/env bash\nexit 0\n", { mode: 0o755 });
  }

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  const result = runCli(tempRoot, [
    "verify", "--feature", "demo", "--state-path", statePath, "--profile", "slice", "--by", "JavaCoder"
  ]);

  assert.equal(result.ok, true);
  assert.equal(result.command, "verify");
  assert.equal(result.profile, "slice");
  assert.equal(result.results.length, 2);
  assert.equal(result.results[0].check, "verifyQuick");
  assert.equal(result.results[0].status, "PASS");
  assert.equal(result.results[1].check, "finalCheck");
  assert.equal(result.results[1].status, "PASS");

  // karate should remain NOT_RUN
  const status = runCli(tempRoot, ["status", "--feature", "demo", "--state-path", statePath]);
  assert.equal(status.status.verifyQuick, "PASS");
  assert.equal(status.status.finalCheck, "PASS");
  assert.equal(status.status.karate, "NOT_RUN");
});

test("flow-log verify --profile slice stops on first failure", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  const scriptsDir = path.join(tempRoot, "scripts");
  fs.mkdirSync(scriptsDir, { recursive: true });
  fs.writeFileSync(path.join(scriptsDir, "verify-quick.sh"), "#!/usr/bin/env bash\necho 'compile error'\nexit 1\n", { mode: 0o755 });
  fs.writeFileSync(path.join(scriptsDir, "final-check.sh"), "#!/usr/bin/env bash\nexit 0\n", { mode: 0o755 });

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  const raw = runCliRaw(tempRoot, [
    "verify", "--feature", "demo", "--state-path", statePath, "--profile", "slice"
  ]);
  const result = JSON.parse(raw.stdout);

  assert.equal(result.ok, false);
  assert.equal(result.stoppedAt, "verifyQuick");
  assert.equal(result.results.length, 1);
  assert.equal(result.results[0].status, "FAIL");
  assert.ok(result.failedCheck.outputTail.some(line => line.includes("compile error")));
  assert.ok(result.failedCheck.logPath);
  assert.ok(fs.existsSync(result.failedCheck.logPath));
});

// --- run-check PASS output trimming ---

test("flow-log run-check PASS trims outputTail to 5 lines max", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");

  const scriptsDir = path.join(tempRoot, "scripts");
  fs.mkdirSync(scriptsDir, { recursive: true });
  // Generate a script that outputs 20 lines, then exits 0
  const lines = Array.from({ length: 20 }, (_, i) => `echo "line ${i + 1}"`).join("\n");
  fs.writeFileSync(path.join(scriptsDir, "verify-quick.sh"), `#!/usr/bin/env bash\n${lines}\nexit 0\n`, { mode: 0o755 });

  runCli(tempRoot, ["create", "--feature", "demo", "--state-path", statePath]);

  const result = runCli(tempRoot, [
    "run-check", "--feature", "demo", "--state-path", statePath, "--name", "verifyQuick", "--by", "JavaCoder"
  ]);

  assert.equal(result.status, "PASS");
  assert.ok(result.outputTail.length <= 5, `Expected at most 5 lines, got ${result.outputTail.length}`);
});

function registerApprovedPlan(tempRoot, statePath, feature, sliceIds) {
  const planPath = path.join(tempRoot, "artifacts", "implementation-plans", `${feature}.plan.json`);
  fs.mkdirSync(path.dirname(planPath), { recursive: true });
  fs.writeFileSync(planPath, `${JSON.stringify(buildValidV4Plan(feature, sliceIds), null, 2)}\n`);

  runCli(tempRoot, [
    "register-artifact",
    "plan",
    "--feature",
    feature,
    "--state-path",
    statePath,
    "--path",
    planPath
  ]);
  runCli(tempRoot, [
    "approve-artifact",
    "plan",
    "--feature",
    feature,
    "--state-path",
    statePath,
    "--by",
    "TL"
  ]);

  return planPath;
}

function buildValidV4Plan(feature, sliceIds = ["S1"]) {
  return {
    schemaVersion: "4.0",
    feature,
    revision: 1,
    status: "approved",
    scope: {
      purpose: "Test plan",
      inScope: ["Slice validation"],
      outOfScope: ["None"],
      constraints: []
    },
    sharedRules: [],
    sharedDecisions: [],
    slices: sliceIds.map((sliceId, index) => ({
      id: sliceId,
      title: `Slice ${index + 1}`,
      goal: `Goal for ${sliceId}`,
      dependsOn: [],
      units: [
        {
          id: `${sliceId}-U1`,
          kind: "java-class",
          locationHint: `flow-orchestrator/src/main/java/com/example/${sliceId}Handler.java`,
          status: "new",
          purpose: `Unit for ${sliceId}`,
          change: "Implement the slice-owned unit.",
          tests: {
            levels: ["unit"],
            notes: "Exercise the owned unit."
          }
        }
      ],
      doneWhen: [
        `Slice ${sliceId} is implemented.`
      ]
    })),
    finalVerification: {
      requiredGates: ["verifyQuick", "finalCheck", "karate"],
      notes: []
    },
    hash: null
  };
}
