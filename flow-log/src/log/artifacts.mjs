import fs from "node:fs";
import path from "node:path";
import { timestamp } from "./common.mjs";

export function assertFileExists(cwd, targetPath, label) {
  const absolutePath = path.resolve(cwd, targetPath);
  if (!fs.existsSync(absolutePath)) {
    throw new Error(`${label} does not exist: ${targetPath}`);
  }

  return absolutePath;
}

export function artifactExists(cwd, artifactPath) {
  if (!artifactPath) {
    return false;
  }

  return fs.existsSync(path.resolve(cwd, artifactPath));
}

export function recordArtifactVerification(entry) {
  entry.lastVerifiedExistsAt = timestamp();
}

export function registerArtifactPath(state, artifactType, artifactPath) {
  const entry = state.artifacts[artifactType];
  entry.path = artifactPath;
  entry.approved = false;
  entry.approvedAt = null;
  entry.approvedBy = null;
  entry.approvedRevision = null;
  entry.approvedHash = null;
  recordArtifactVerification(entry);
  return entry;
}

export function approveArtifact(state, artifactType, approver) {
  const entry = state.artifacts[artifactType];
  entry.approved = true;
  entry.approvedAt = timestamp();
  entry.approvedBy = approver;
  recordArtifactVerification(entry);
  return entry;
}
