import fs from "node:fs";
import path from "node:path";
import { timestamp } from "./common.mjs";
import { validateStateShape } from "./schema.mjs";

export function resolveStatePath(cwd, feature, explicitStatePath) {
  if (explicitStatePath) {
    return path.resolve(cwd, explicitStatePath);
  }

  return path.resolve(cwd, "artifacts", "flow-logs", `${feature}.json`);
}

export function loadState(statePath) {
  if (!fs.existsSync(statePath)) {
    throw new Error(`State file does not exist: ${statePath}`);
  }

  const raw = fs.readFileSync(statePath, "utf8");
  const state = JSON.parse(raw);
  validateStateShape(state, statePath);
  return state;
}

export function saveState(statePath, state) {
  state.updatedAt = timestamp();
  fs.mkdirSync(path.dirname(statePath), { recursive: true });
  fs.writeFileSync(statePath, `${JSON.stringify(state, null, 2)}\n`);
}

export function ensureFeatureMatches(state, feature, statePath) {
  if (state.feature !== feature) {
    throw new Error(
      `Feature mismatch for state file ${statePath}: expected '${feature}', found '${state.feature}'`
    );
  }
}
