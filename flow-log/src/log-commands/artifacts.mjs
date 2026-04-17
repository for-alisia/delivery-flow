import fs from "node:fs";
import {
  ARTIFACT_TYPES,
  approveArtifact,
  assertFileExists,
  registerArtifactPath,
  saveState,
  validateValue
} from "../log/index.mjs";
import { requiredFlag } from "../cli-helpers.mjs";
import { computePlanHash, isV3Plan } from "../plan/index.mjs";
import { openState } from "./shared.mjs";

export function handleRegisterArtifact(parsed, cwd, artifactType) {
  validateValue(artifactType, ARTIFACT_TYPES, "artifact type");
  const artifactPath = requiredFlag(parsed, "path");
  assertFileExists(cwd, artifactPath, `${artifactType} artifact`);

  const { feature, state, statePath } = openState(parsed, cwd);
  const entry = registerArtifactPath(state, artifactType, artifactPath);
  saveState(statePath, state);

  return {
    ok: true,
    command: "register-artifact",
    feature,
    artifactType,
    statePath,
    artifact: entry
  };
}

export function handleApproveArtifact(parsed, cwd, artifactType) {
  validateValue(artifactType, ARTIFACT_TYPES, "artifact type");
  const approver = requiredFlag(parsed, "by");
  const { feature, state, statePath } = openState(parsed, cwd);
  const entry = state.artifacts[artifactType];

  if (!entry.path) {
    throw new Error(`${artifactType} artifact path is not registered.`);
  }

  const artifactAbsolutePath = assertFileExists(cwd, entry.path, `${artifactType} artifact`);
  approveArtifact(state, artifactType, approver);

  if (artifactType === "plan") {
    const parsed = tryReadJsonArtifact(artifactAbsolutePath);
    if (parsed) {
      entry.approvedRevision = Number.isInteger(parsed.revision) ? parsed.revision : null;
      entry.approvedHash = isV3Plan(parsed)
        ? (typeof parsed.hash === "string" ? parsed.hash : computePlanHash(parsed))
        : null;
    } else {
      entry.approvedRevision = null;
      entry.approvedHash = null;
    }
  } else {
    entry.approvedRevision = null;
    entry.approvedHash = null;
  }

  saveState(statePath, state);

  return {
    ok: true,
    command: "approve-artifact",
    feature,
    artifactType,
    statePath,
    artifact: entry
  };
}

function tryReadJsonArtifact(artifactPath) {
  const raw = fs.readFileSync(artifactPath, "utf8");

  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}
