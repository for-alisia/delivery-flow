import {
  addRisk,
  buildArchitectureGate,
  incrementReviewRound,
  reopenRisk,
  resolveRisk,
  respondToRisk,
  saveState
} from "../log/index.mjs";
import {
  optionalFlag,
  parsePositiveInteger,
  requiredFlag
} from "../cli-helpers.mjs";
import { openState } from "./shared.mjs";

export function handleAddRisk(parsed, cwd) {
  const severity = requiredFlag(parsed, "severity");
  const description = requiredFlag(parsed, "description");
  const suggestedFix = optionalFlag(parsed, "suggested-fix");
  const by = optionalFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const risk = addRisk(state, severity, description, by, suggestedFix);
  saveState(statePath, state);

  return {
    ok: true,
    command: "add-risk",
    feature,
    statePath,
    risk
  };
}

export function handleRespondRisk(parsed, cwd) {
  const riskId = parsePositiveInteger(requiredFlag(parsed, "id"), "risk id");
  const status = requiredFlag(parsed, "status");
  const note = requiredFlag(parsed, "note");
  const by = optionalFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const risk = respondToRisk(state, riskId, status, note, by);
  saveState(statePath, state);

  return {
    ok: true,
    command: "respond-risk",
    feature,
    statePath,
    risk
  };
}

export function handleResolveRisk(parsed, cwd) {
  const riskId = parsePositiveInteger(requiredFlag(parsed, "id"), "risk id");
  const by = optionalFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const risk = resolveRisk(state, riskId, by);
  saveState(statePath, state);

  return {
    ok: true,
    command: "resolve-risk",
    feature,
    statePath,
    risk
  };
}

export function handleReopenRisk(parsed, cwd) {
  const riskId = parsePositiveInteger(requiredFlag(parsed, "id"), "risk id");
  const reason = optionalFlag(parsed, "reason");
  const by = optionalFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const risk = reopenRisk(state, riskId, reason, by);
  saveState(statePath, state);

  return {
    ok: true,
    command: "reopen-risk",
    feature,
    statePath,
    risk
  };
}

export function handleIncrementRound(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  const round = incrementReviewRound(state);
  saveState(statePath, state);

  return {
    ok: true,
    command: "increment-round",
    feature,
    statePath,
    round
  };
}

export function handleArchitectureGate(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);

  return {
    ok: true,
    command: "architecture-gate",
    feature,
    statePath,
    ...buildArchitectureGate(state)
  };
}
