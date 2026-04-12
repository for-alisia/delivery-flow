import {
  buildSignoffReadiness,
  buildStatus,
  buildSummary
} from "../log/index.mjs";
import {
  optionalFlag,
  parsePositiveInteger
} from "../cli-helpers.mjs";
import { openState } from "./shared.mjs";

export function handleGet(parsed, cwd) {
  const { state, statePath } = openState(parsed, cwd);
  return {
    ok: true,
    statePath,
    state
  };
}

export function handleHistory(parsed, cwd) {
  const { state, statePath } = openState(parsed, cwd);
  const limitValue = optionalFlag(parsed, "limit");
  const limit = limitValue === undefined ? undefined : parsePositiveInteger(limitValue, "limit");
  const events = limit ? state.events.slice(-limit) : state.events;

  return {
    ok: true,
    statePath,
    eventCount: state.events.length,
    events
  };
}

export function handleStatus(parsed, cwd) {
  const { state, statePath } = openState(parsed, cwd);
  return {
    ok: true,
    status: buildStatus(state, cwd, statePath)
  };
}

export function handleSummary(parsed, cwd) {
  const { state, statePath } = openState(parsed, cwd);
  return {
    ok: true,
    summary: buildSummary(state, cwd, statePath)
  };
}

export function handleReadiness(parsed, cwd, target) {
  if (target !== "signoff") {
    throw new Error("Only 'signoff' readiness is supported in v1.");
  }

  const { state, statePath } = openState(parsed, cwd);

  return {
    ok: true,
    statePath,
    readiness: buildSignoffReadiness(state, cwd)
  };
}
