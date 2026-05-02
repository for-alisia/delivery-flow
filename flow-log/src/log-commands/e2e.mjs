import {
  E2E_MODES,
  saveState,
  setE2EMode,
  validateValue
} from "../log/index.mjs";
import { optionalFlag, requiredFlag } from "../cli-helpers.mjs";
import { openState } from "./shared.mjs";

export function handleSetE2EMode(parsed, cwd) {
  const mode = requiredFlag(parsed, "mode");
  validateValue(mode, E2E_MODES, "e2e mode");

  const { feature, state, statePath } = openState(parsed, cwd);
  const e2e = setE2EMode(
    state,
    mode,
    optionalFlag(parsed, "by"),
    optionalFlag(parsed, "reason")
  );
  saveState(statePath, state);

  return {
    ok: true,
    command: "set-e2e-mode",
    feature,
    statePath,
    e2e
  };
}