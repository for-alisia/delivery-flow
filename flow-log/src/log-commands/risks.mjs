import {
  addRisk,
  buildArchitectureGate,
  incrementReviewRound,
  reclassifyRisk,
  reopenRisk,
  resolveRisk,
  respondToRisk,
  saveState
} from "../log/index.mjs";
import {
  normalizeArray,
  optionalFlag,
  parsePositiveInteger,
  requiredFlag
} from "../cli-helpers.mjs";
import { openState } from "./shared.mjs";

export function handleAddRisk(parsed, cwd) {
  const severity = optionalFlag(parsed, "severity") ?? null;
  const description = requiredFlag(parsed, "description");
  const suggestedFix = optionalFlag(parsed, "suggested-fix");
  const planRefs = parseMultiValueFlags(optionalFlag(parsed, "plan-ref"));
  const connectedAreas = parseMultiValueFlags(optionalFlag(parsed, "connected-area"));
  const by = optionalFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const risk = addRisk(state, severity, description, by, suggestedFix, planRefs, connectedAreas);
  saveState(statePath, state);

  return {
    ok: true,
    command: "add-risk",
    feature,
    statePath,
    risk
  };
}

function parseMultiValueFlags(rawValue) {
  return normalizeArray(rawValue)
    .flatMap((value) => String(value).split(","))
    .map((value) => value.trim())
    .filter((value) => value.length > 0);
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

export function handleReclassifyRisk(parsed, cwd) {
  const riskId = parsePositiveInteger(requiredFlag(parsed, "id"), "risk id");
  const severity = requiredFlag(parsed, "severity");
  const reason = requiredFlag(parsed, "reason");
  const by = optionalFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const risk = reclassifyRisk(state, riskId, severity, reason, by);
  saveState(statePath, state);

  return {
    ok: true,
    command: "reclassify-risk",
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
