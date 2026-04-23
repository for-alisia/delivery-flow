import fs from "node:fs";
import { createHash } from "node:crypto";
import path from "node:path";
import { timestamp } from "./common.mjs";
import { computePlanHash, isPlan } from "../plan/index.mjs";

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

export function readArtifactApprovalMetadata(artifactType, absolutePath) {
  const raw = fs.readFileSync(absolutePath, "utf8");

  if (artifactType === "plan") {
    const parsed = tryParseJson(raw);
    if (parsed && isPlan(parsed)) {
      return {
        revision: Number.isInteger(parsed.revision) ? parsed.revision : null,
        // Recompute from content so direct file edits cannot hide behind a stale stored hash field.
        hash: computePlanHash(parsed)
      };
    }
  }

  return {
    revision: null,
    hash: computeContentHash(raw)
  };
}

export function buildArtifactApprovalState(cwd, artifactType, entry) {
  const exists = artifactExists(cwd, entry?.path);
  const approvedHash = entry?.approvedHash ?? null;
  const approvalTracked = typeof approvedHash === "string" && approvedHash.length > 0;

  if (!exists) {
    return {
      path: entry?.path ?? null,
      exists: false,
      approved: entry?.approved === true,
      approvedRevision: entry?.approvedRevision ?? null,
      approvedHash,
      currentRevision: null,
      currentHash: null,
      approvalTracked,
      stale: false
    };
  }

  const absolutePath = path.resolve(cwd, entry.path);
  const current = readArtifactApprovalMetadata(artifactType, absolutePath);
  const stale = entry?.approved === true && (!approvalTracked || current.hash !== approvedHash);

  return {
    path: entry.path,
    exists: true,
    approved: entry?.approved === true,
    approvedRevision: entry?.approvedRevision ?? null,
    approvedHash,
    currentRevision: current.revision,
    currentHash: current.hash,
    approvalTracked,
    stale
  };
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

function tryParseJson(raw) {
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

function computeContentHash(content) {
  return createHash("sha256").update(content).digest("hex");
}
