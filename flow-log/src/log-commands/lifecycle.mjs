import {
  completeFlow,
  createInitialState,
  resolveStatePath,
  saveState
} from "../log/index.mjs";
import { optionalFlag, requiredFlag } from "../cli-helpers.mjs";
import { assertStateDoesNotExist, openState } from "./shared.mjs";

export function handleCreate(parsed, cwd) {
  const feature = requiredFlag(parsed, "feature");
  const statePath = resolveStatePath(cwd, feature, optionalFlag(parsed, "state-path"));

  if (optionalFlag(parsed, "force") !== true) {
    assertStateDoesNotExist(statePath);
  }

  const state = createInitialState(feature);
  saveState(statePath, state);

  return {
    ok: true,
    command: "create",
    feature,
    statePath
  };
}

export function handleLockRequirements(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  state.requirements.locked = true;
  state.requirements.lockedAt = new Date().toISOString();
  state.requirements.lockedBy = optionalFlag(parsed, "by");
  const requestSource = optionalFlag(parsed, "request-source");
  if (requestSource) {
    state.requirements.requestSource = requestSource;
  }
  saveState(statePath, state);

  return {
    ok: true,
    command: "lock-requirements",
    feature,
    statePath,
    requirements: state.requirements
  };
}

export function handleComplete(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  completeFlow(state);
  saveState(statePath, state);

  return {
    ok: true,
    command: "complete",
    feature,
    statePath,
    timing: state.timing
  };
}
