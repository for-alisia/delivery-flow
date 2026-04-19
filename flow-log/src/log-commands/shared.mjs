import {
  ensureFeatureMatches,
  loadState,
  resolveStatePath
} from "../log/index.mjs";
import { optionalFlag, requiredFlag } from "../cli-helpers.mjs";

export function assertStateDoesNotExist(statePath) {
  try {
    loadState(statePath);
  } catch (error) {
    if (error.message.startsWith("State file does not exist")) {
      return;
    }
    throw error;
  }

  throw new Error(`State file already exists: ${statePath}`);
}

export function openState(parsed, cwd) {
  const feature = requiredFlag(parsed, "feature");
  const statePath = resolveStatePath(cwd, feature, optionalFlag(parsed, "state-path"));
  const state = loadState(statePath);
  ensureFeatureMatches(state, feature, statePath);

  return { feature, state, statePath };
}
