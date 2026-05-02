import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { createTempRoot, runCli } from "./test-helpers.mjs";

const testDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(testDir, "..", "..");
const handoffScript = path.join(repoRoot, "scripts", "coder-handoff-check.sh");

test("coder handoff check reflects Team Lead smoke ownership for REUSE_EXISTING", () => {
  const { feature, statePath } = createHandoffState({
    type: "intermediate",
    e2eMode: "REUSE_EXISTING",
    currentFiles: ["flow-orchestrator/src/main/java/com/example/Demo.java"],
    checks: {
      verifyQuick: "PASS",
      finalCheck: "PASS"
    }
  });

  const result = runHandoff(feature, statePath);

  assert.equal(result.status, 0);
  assert.match(result.stdout, /active slice-run type is 'intermediate'/);
  assert.match(result.stdout, /post-handoff smoke owner is Team Lead for REUSE_EXISTING/);
});

test("coder handoff check reflects E2E Tester smoke ownership for SCENARIOS_REQUIRED", () => {
  const { feature, statePath } = createHandoffState({
    type: "final",
    e2eMode: "SCENARIOS_REQUIRED",
    currentFiles: ["flow-orchestrator/src/main/java/com/example/Demo.java"],
    checks: {
      verifyQuick: "PASS",
      finalCheck: "PASS"
    }
  });

  const result = runHandoff(feature, statePath);

  assert.equal(result.status, 0);
  assert.match(result.stdout, /post-handoff smoke owner is E2E Tester for SCENARIOS_REQUIRED/);
});

test("coder handoff check requires active slice-run changed files", () => {
  const { feature, statePath } = createHandoffState({
    type: "intermediate",
    currentFiles: [],
    globalFiles: ["flow-orchestrator/src/main/java/com/example/PreviousSlice.java"],
    checks: {
      verifyQuick: "PASS",
      finalCheck: "PASS"
    }
  });

  const result = runHandoff(feature, statePath);

  assert.notEqual(result.status, 0);
  assert.match(result.stdout, /No changed files recorded for active slice-run/);
});

function createHandoffState({ type, e2eMode = "REUSE_EXISTING", currentFiles, globalFiles = currentFiles, checks }) {
  const tempRoot = createTempRoot("handoff-check-");
  const feature = `handoff-${process.pid}-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
  const statePath = path.join(tempRoot, `${feature}.json`);

  runCli(repoRoot, ["create", "--feature", feature, "--state-path", statePath]);
  for (const [name, status] of Object.entries(checks)) {
    runCli(repoRoot, [
      "set-check",
      "--feature",
      feature,
      "--state-path",
      statePath,
      "--name",
      name,
      "--status",
      status,
      "--by",
      "Test"
    ]);
  }

  const state = JSON.parse(fs.readFileSync(statePath, "utf8"));
  state.e2e = {
    mode: e2eMode,
    decidedAt: state.createdAt,
    decidedBy: "TL",
    reason: null
  };
  state.sliceRuns.current = {
    run: 1,
    slice: "S1",
    type,
    changedFiles: currentFiles,
    startedAt: state.createdAt,
    completedAt: null,
    status: "in-progress",
    by: "TL"
  };
  state.sliceRuns.total = 1;
  state.changes.files = globalFiles;
  fs.writeFileSync(statePath, `${JSON.stringify(state, null, 2)}\n`);

  return { feature, statePath };
}

function runHandoff(feature, statePath) {
  const safeEnv = {
    PATH: process.env.PATH,
    HOME: process.env.HOME,
    TMPDIR: process.env.TMPDIR,
    TMP: process.env.TMP,
    TEMP: process.env.TEMP,
    SHELL: process.env.SHELL,
    USER: process.env.USER,
    LANG: process.env.LANG,
    TERM: process.env.TERM
  };

  return spawnSync("bash", [handoffScript, feature, "--state-path", statePath], {
    cwd: repoRoot,
    encoding: "utf8",
    env: safeEnv
  });
}
