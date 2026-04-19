import fs from "node:fs";
import path from "node:path";

export function resolvePlanPath(cwd, feature) {
  return path.resolve(cwd, "artifacts", "implementation-plans", `${feature}.plan.json`);
}

export function planExists(planPath) {
  return fs.existsSync(planPath);
}

export function loadPlan(planPath) {
  if (!fs.existsSync(planPath)) {
    throw new Error(`Plan file does not exist: ${planPath}`);
  }

  return readJsonFile(planPath, "plan");
}

export function loadPlanIfExists(planPath) {
  if (!fs.existsSync(planPath)) {
    return null;
  }

  return readJsonFile(planPath, "plan");
}

export function savePlan(planPath, plan) {
  writeJsonAtomic(planPath, plan);
}

export function readJsonFile(filePath, label) {
  const raw = fs.readFileSync(filePath, "utf8");
  try {
    return JSON.parse(raw);
  } catch (error) {
    throw new Error(`Invalid ${label} JSON at ${filePath}: ${error.message}`);
  }
}

export function writeJsonAtomic(filePath, value) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });

  const tempPath = `${filePath}.${process.pid}.${Date.now()}.tmp`;
  fs.writeFileSync(tempPath, `${JSON.stringify(value, null, 2)}\n`);
  fs.renameSync(tempPath, filePath);
}
