import {
  appendEvent,
  completeBatch,
  resetChecksForRedCard,
  saveState,
  startBatch
} from "../log/index.mjs";
import {
  normalizeArray,
  optionalFlag
} from "../cli-helpers.mjs";
import { openState } from "./shared.mjs";

export function handleStartBatch(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  const slices = normalizeArray(optionalFlag(parsed, "slice"));
  const by = optionalFlag(parsed, "by");
  startBatch(state, slices, by);
  saveState(statePath, state);

  return {
    ok: true,
    command: "start-batch",
    feature,
    statePath,
    batch: state.batches.current
  };
}

export function handleCompleteBatch(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  const status = optionalFlag(parsed, "status") ?? "complete";
  completeBatch(state, status);
  saveState(statePath, state);

  const last = state.batches.history[state.batches.history.length - 1];

  return {
    ok: true,
    command: "complete-batch",
    feature,
    statePath,
    completedBatch: last,
    totalBatches: state.batches.total
  };
}

export function handleResetChecks(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  const reason = optionalFlag(parsed, "reason") ?? "Red card — checks reset for coder retry";
  resetChecksForRedCard(state);
  appendEvent(state, {
    type: "redCard",
    by: optionalFlag(parsed, "by") ?? "TL",
    reason,
    target: optionalFlag(parsed, "target") ?? "JavaCoder"
  });
  saveState(statePath, state);

  return {
    ok: true,
    command: "reset-checks",
    feature,
    statePath,
    checks: {
      verifyQuick: state.checks.verifyQuick.status,
      finalCheck: state.checks.finalCheck.status,
      karate: state.checks.karate.status
    },
    eventRecorded: state.events[state.events.length - 1]
  };
}
