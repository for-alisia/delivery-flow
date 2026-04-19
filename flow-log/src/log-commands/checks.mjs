import {
  CHECK_NAMES,
  CHECK_STATUSES,
  appendChangedFiles,
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
    changedFiles: state.changes.files
  };
}
