import { timestamp } from "./common.mjs";
import { createCheckEntry } from "./schema.mjs";

export function setCheck(state, name, status, options = {}) {
  state.checks[name] = {
    status,
    updatedAt: timestamp(),
    updatedBy: options.by ?? null,
    command: options.command ?? null,
    details: options.details ?? null,
    reportPaths: options.reportPaths ?? []
  };

  return state.checks[name];
}

export function appendChangedFiles(state, files) {
  const next = new Set(state.changes.files);

  for (const file of files) {
    next.add(file);
  }

  state.changes.files = Array.from(next).sort();
}

export function resetChecksForRedCard(state) {
  state.checks.verifyQuick = createCheckEntry();
  state.checks.finalCheck = createCheckEntry();
  state.checks.karate = createCheckEntry();
}

export function summarizeChecks(checks) {
  return {
    verifyQuick: checks.verifyQuick.status,
    finalCheck: checks.finalCheck.status,
    karate: checks.karate.status
  };
}
