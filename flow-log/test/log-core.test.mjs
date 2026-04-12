import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import {
  EVENT_TYPES,
  MAX_ARCHITECTURE_REVIEW_ROUNDS,
  MAX_CODE_REVIEW_ROUNDS,
  addFinding,
  addRisk,
  appendEvent,
  buildArchitectureGate,
  buildCodeReviewGate,
  completeBatch,
  createInitialState,
  ensureFeatureMatches,
  incrementCodeReviewRound,
  incrementReviewRound,
  loadState,
  saveState,
  startBatch,
  summarizeBatches,
  summarizeEventCounts
} from "../src/log/index.mjs";
import { createTempRoot } from "./test-helpers.mjs";

test("event summary counts all supported event types and never returns NaN", () => {
  const state = createInitialState("demo");

  appendEvent(state, {
    type: "redCard",
    reason: "Reset checks"
  });
  appendEvent(state, {
    type: "archEscalationDecision",
    reason: "Escalation decision recorded",
    decision: "PROCEED_TO_CODING"
  });

  const summary = summarizeEventCounts(state.events);

  assert.equal(summary.total, 2);
  assert.equal(summary.counts.redCard, 1);
  assert.equal(summary.counts.archEscalationDecision, 1);

  for (const type of EVENT_TYPES) {
    assert.equal(typeof summary.counts[type], "number");
    assert.equal(Number.isNaN(summary.counts[type]), false);
  }

  assert.equal(summary.counts.batchStart, 0);
  assert.equal(summary.counts.batchEnd, 0);
});

test("architecture gate returns PASS, FAIL, and ESCALATE for expected conditions", () => {
  const passState = createInitialState("demo-pass");
  const passGate = buildArchitectureGate(passState);
  assert.equal(passGate.gate, "PASS");

  const failState = createInitialState("demo-fail");
  incrementReviewRound(failState);
  addRisk(failState, "HIGH", "Missing rollback strategy", "Reviewer", null);
  const failGate = buildArchitectureGate(failState);
  assert.equal(failGate.gate, "FAIL");
  assert.equal(failGate.unresolvedBlocking, 1);

  const escalateState = createInitialState("demo-escalate");
  for (let i = 0; i < MAX_ARCHITECTURE_REVIEW_ROUNDS; i += 1) {
    incrementReviewRound(escalateState);
  }
  addRisk(escalateState, "CRITICAL", "Data loss risk not addressed", "Reviewer", null);
  const escalateGate = buildArchitectureGate(escalateState);
  assert.equal(escalateGate.gate, "ESCALATE");
  assert.equal(escalateGate.round, MAX_ARCHITECTURE_REVIEW_ROUNDS);
  assert.equal(escalateGate.unresolvedBlocking, 1);
});

test("code review gate returns PASS, FAIL, and ESCALATE for expected conditions", () => {
  const passState = createInitialState("demo-pass");
  const passGate = buildCodeReviewGate(passState);
  assert.equal(passGate.gate, "PASS");

  const failState = createInitialState("demo-fail");
  incrementCodeReviewRound(failState);
  addFinding(failState, "HIGH", "Null handling missing", "src/App.java", "Reviewer");
  const failGate = buildCodeReviewGate(failState);
  assert.equal(failGate.gate, "FAIL");
  assert.equal(failGate.unresolvedBlocking, 1);

  const escalateState = createInitialState("demo-escalate");
  for (let i = 0; i < MAX_CODE_REVIEW_ROUNDS; i += 1) {
    incrementCodeReviewRound(escalateState);
  }
  addFinding(escalateState, "CRITICAL", "SQL injection path", "src/Repo.java", "Reviewer");
  const escalateGate = buildCodeReviewGate(escalateState);
  assert.equal(escalateGate.gate, "ESCALATE");
  assert.equal(escalateGate.round, MAX_CODE_REVIEW_ROUNDS);
  assert.equal(escalateGate.unresolvedBlocking, 1);
});

test("store layer updates updatedAt, validates shape, and rejects feature mismatches", () => {
  const tempRoot = createTempRoot();
  const statePath = path.join(tempRoot, "feature.json");
  const invalidPath = path.join(tempRoot, "invalid.json");

  const state = createInitialState("demo");
  state.updatedAt = "2000-01-01T00:00:00.000Z";
  saveState(statePath, state);

  const loaded = loadState(statePath);
  assert.notEqual(loaded.updatedAt, "2000-01-01T00:00:00.000Z");

  ensureFeatureMatches(loaded, "demo", statePath);
  assert.throws(
    () => ensureFeatureMatches(loaded, "other-feature", statePath),
    /Feature mismatch/
  );

  fs.writeFileSync(invalidPath, `${JSON.stringify({ feature: "broken" })}\n`);
  assert.throws(
    () => loadState(invalidPath),
    /Invalid state file/
  );
});

test("batch lifecycle guards complete-without-start and preserves history totals", () => {
  const state = createInitialState("demo");

  assert.throws(
    () => completeBatch(state),
    /No batch is currently in progress/
  );

  startBatch(state, ["slice-1"], "TL");
  completeBatch(state, "complete");

  startBatch(state, ["slice-2"], "TL");
  completeBatch(state);

  const summary = summarizeBatches(state);
  assert.equal(summary.current, null);
  assert.equal(summary.completed, 2);
  assert.equal(summary.total, 2);
});
