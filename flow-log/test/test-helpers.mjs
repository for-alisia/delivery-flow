import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";

const repoRoot = path.resolve(path.dirname(new URL(import.meta.url).pathname), "..", "..");
const cliPath = path.join(repoRoot, "flow-log", "flow-log.mjs");

export function createTempRoot(prefix = "flow-log-test-") {
  return fs.mkdtempSync(path.join(os.tmpdir(), prefix));
}

export function createPlanTempRoot(prefix = "flow-log-plan-") {
  const tempRoot = createTempRoot(prefix);
  fs.mkdirSync(path.join(tempRoot, "artifacts", "implementation-plans"), { recursive: true });
  return tempRoot;
}

export function runCliRaw(cwd, args) {
  return spawnSync(process.execPath, [cliPath, ...args], {
    cwd,
    encoding: "utf8"
  });
}

export function runCli(cwd, args) {
  const result = runCliRaw(cwd, args);

  if (result.status !== 0) {
    throw new Error(`CLI failed: ${result.stderr}`);
  }

  return JSON.parse(result.stdout);
}
