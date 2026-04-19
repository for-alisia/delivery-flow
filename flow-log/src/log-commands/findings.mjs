import {
  addFinding,
  buildCodeReviewGate,
  incrementCodeReviewRound,
  reopenFinding,
  resolveFinding,
  respondToFinding,
  saveState
} from "../log/index.mjs";
import {
  optionalFlag,
  parsePositiveInteger,
  requiredFlag
} from "../cli-helpers.mjs";
import { openState } from "./shared.mjs";

export function handleAddFinding(parsed, cwd) {
  const severity = requiredFlag(parsed, "severity");
  const description = requiredFlag(parsed, "description");
  const file = optionalFlag(parsed, "file");
  const by = optionalFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const finding = addFinding(state, severity, description, file, by);
  saveState(statePath, state);

  return {
    ok: true,
    command: "add-finding",
    feature,
    statePath,
    finding
  };
}

export function handleRespondFinding(parsed, cwd) {
  const findingId = parsePositiveInteger(requiredFlag(parsed, "id"), "finding id");
  const status = requiredFlag(parsed, "status");
  const note = requiredFlag(parsed, "note");
  const by = optionalFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const finding = respondToFinding(state, findingId, status, note, by);
  saveState(statePath, state);

  return {
    ok: true,
    command: "respond-finding",
    feature,
    statePath,
    finding
  };
}

export function handleResolveFinding(parsed, cwd) {
  const findingId = parsePositiveInteger(requiredFlag(parsed, "id"), "finding id");
  const by = optionalFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const finding = resolveFinding(state, findingId, by);
  saveState(statePath, state);

  return {
    ok: true,
    command: "resolve-finding",
    feature,
    statePath,
    finding
  };
}

export function handleReopenFinding(parsed, cwd) {
  const findingId = parsePositiveInteger(requiredFlag(parsed, "id"), "finding id");
  const reason = optionalFlag(parsed, "reason");
  const by = optionalFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const finding = reopenFinding(state, findingId, reason, by);
  saveState(statePath, state);

  return {
    ok: true,
    command: "reopen-finding",
    feature,
    statePath,
    finding
  };
}

export function handleIncrementCodeReviewRound(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  const round = incrementCodeReviewRound(state);
  saveState(statePath, state);

  return {
    ok: true,
    command: "increment-code-review-round",
    feature,
    statePath,
    round
  };
}

export function handleCodeReviewGate(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);

  return {
    ok: true,
    command: "code-review-gate",
    feature,
    statePath,
    ...buildCodeReviewGate(state)
  };
}
