import fs from "node:fs";
import path from "node:path";
import { timestamp } from "./common.mjs";
import { validateStateShape } from "./schema.mjs";

export function resolveStatePath(cwd, feature, explicitStatePath) {
  if (explicitStatePath) {
    return path.resolve(cwd, explicitStatePath);
  }

  const artifactsDir = path.resolve(cwd, "artifacts");
  if (!fs.existsSync(artifactsDir)) {
    const suggested = findRepoRoot(cwd);
    const hint = suggested ? ` Try: cd ${suggested}` : "";
    throw new Error(
      `flow-log must be run from the repository root directory. ` +
      `Current directory: ${cwd} does not contain artifacts/.${hint}`
    );
  }

  return path.resolve(cwd, "artifacts", "flow-logs", `${feature}.json`);
}

function findRepoRoot(startDir, maxLevels = 3) {
  let dir = startDir;
  for (let i = 0; i <= maxLevels; i++) {
    if (fs.existsSync(path.join(dir, "artifacts")) && fs.existsSync(path.join(dir, "flow-log"))) {
      return dir;
    }
    const parent = path.dirname(dir);
    if (parent === dir) break;
    dir = parent;
  }
  return null;
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
