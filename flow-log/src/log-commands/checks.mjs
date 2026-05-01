import {
  CHECK_NAMES,
  CHECK_STATUSES,
  appendChangedFiles,
  runAndRecordCheck,
  saveState,
  setCheck,
  validateValue
} from "../log/index.mjs";
import {
  normalizeArray,
  optionalFlag,
  requiredFlag
} from "../cli-helpers.mjs";
import { openState } from "./shared.mjs";

export function handleSetCheck(parsed, cwd) {
  const name = requiredFlag(parsed, "name");
  const status = requiredFlag(parsed, "status");
  validateValue(name, CHECK_NAMES, "check name");
  validateValue(status, CHECK_STATUSES, "check status");

  const { feature, state, statePath } = openState(parsed, cwd);
  const reportPaths = normalizeArray(optionalFlag(parsed, "report-path"));

  const check = setCheck(state, name, status, {
    by: optionalFlag(parsed, "by"),
    command: optionalFlag(parsed, "command"),
    details: optionalFlag(parsed, "details"),
    reportPaths
  });
  saveState(statePath, state);

  return {
    ok: true,
    command: "set-check",
    feature,
    name,
    statePath,
    check
  };
}

export function handleAddChange(parsed, cwd) {
  const files = normalizeArray(requiredFlag(parsed, "file"));
  const { feature, state, statePath } = openState(parsed, cwd);
  appendChangedFiles(state, files);
  saveState(statePath, state);

  return {
    ok: true,
    command: "add-change",
    feature,
    statePath,
    changedFiles: state.changes.files,
    currentBatchChangedFiles: state.batches?.current?.changedFiles ?? []
  };
}

export function handleRunCheck(parsed, cwd) {
  const name = requiredFlag(parsed, "name");
  validateValue(name, CHECK_NAMES, "check name");

  const { feature, state, statePath } = openState(parsed, cwd);

  const timeoutRaw = optionalFlag(parsed, "timeout");
  const timeout = timeoutRaw ? Number(timeoutRaw) : undefined;

  const result = runAndRecordCheck(state, name, cwd, {
    command: optionalFlag(parsed, "command"),
    timeout,
    by: optionalFlag(parsed, "by")
  });
  saveState(statePath, state);

  return {
    ok: result.status === "PASS",
    command: "run-check",
    feature,
    statePath,
    ...result
  };
}

export function handleVerify(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  const profile = optionalFlag(parsed, "profile") ?? "full";
  const by = optionalFlag(parsed, "by") ?? "flow-log";
  const timeoutRaw = optionalFlag(parsed, "timeout");
  const timeout = timeoutRaw ? Number(timeoutRaw) : undefined;
  const sequence = resolveVerifySequence(profile);
  const results = [];

  for (const name of sequence) {
    const result = runAndRecordCheck(state, name, cwd, { timeout, by });
    saveState(statePath, state);
    results.push({ check: name, status: result.status, durationMs: result.durationMs });

    if (result.status !== "PASS") {
      return {
        ok: false,
        command: "verify",
        feature,
        profile,
        statePath,
        stoppedAt: name,
        results,
        failedCheck: result
      };
    }
  }

  return {
    ok: true,
    command: "verify",
    feature,
    profile,
    statePath,
    results
  };
}

function resolveVerifySequence(profile) {
  if (profile === "full") {
    return ["verifyQuick", "finalCheck", "karate"];
  }
  if (profile === "batch") {
    return ["verifyQuick", "finalCheck"];
  }

  throw new Error(`Unknown verify profile: ${profile}. Available: full, batch`);
}
