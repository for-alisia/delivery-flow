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
  completeSliceRun,
  createInitialState,
  decideFinding,
  decideRisk,
  ensureFeatureMatches,
  incrementCodeReviewRound,
  incrementReviewRound,
  loadState,
  saveState,
  startSliceRun,
  summarizeSliceRuns,
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

  assert.equal(summary.counts.sliceRunStart, 0);
  assert.equal(summary.counts.sliceRunEnd, 0);
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

test("review gates surface explicit non-blocking debt counts", () => {
  const architectureState = createInitialState("demo-arch-debt");
  incrementReviewRound(architectureState);
  addRisk(architectureState, "MEDIUM", "Naming cleanup", "ArchitectureReviewer", null, ["S1"]);

  let architectureGate = buildArchitectureGate(architectureState);
  assert.equal(architectureGate.gate, "PASS");
  assert.equal(architectureGate.undecidedNonBlocking, 1);
  assert.equal(architectureGate.accepted, 0);
  assert.equal(architectureGate.deferred, 0);

  decideRisk(architectureState, 1, "ACCEPTED", "Accept naming debt for this delivery.", "TL", null);
  architectureGate = buildArchitectureGate(architectureState);
  assert.equal(architectureGate.gate, "PASS");
  assert.equal(architectureGate.undecidedNonBlocking, 0);
  assert.equal(architectureGate.accepted, 1);

  const codeState = createInitialState("demo-code-debt");
  incrementCodeReviewRound(codeState);
  addFinding(codeState, "LOW", "Extract constant", "src/App.java", "CodeReviewer");

  let codeGate = buildCodeReviewGate(codeState);
  assert.equal(codeGate.gate, "PASS");
  assert.equal(codeGate.undecidedNonBlocking, 1);
  assert.equal(codeGate.accepted, 0);
  assert.equal(codeGate.deferred, 0);

  decideFinding(codeState, 1, "DEFERRED", "Follow up in cleanup story.", "TL", "cleanup-story");
  codeGate = buildCodeReviewGate(codeState);
  assert.equal(codeGate.gate, "PASS");
  assert.equal(codeGate.undecidedNonBlocking, 0);
  assert.equal(codeGate.deferred, 1);
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

test("slice-run lifecycle guards complete-without-start and preserves history totals", () => {
  const state = createInitialState("demo");

  assert.throws(
    () => completeSliceRun(state),
    /No slice-run is currently in progress/
  );

  startSliceRun(state, "slice-1", "intermediate", "TL");
  completeSliceRun(state, "complete");

  startSliceRun(state, "slice-2", "final", "TL");
  completeSliceRun(state);

  const summary = summarizeSliceRuns(state);
  assert.equal(summary.current, null);
  assert.equal(summary.completed, 2);
  assert.equal(summary.total, 2);
});
