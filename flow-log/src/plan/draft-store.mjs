import fs from "node:fs";
import path from "node:path";
import { createDraftFromPlan } from "./schema.mjs";
import { readJsonFile, writeJsonAtomic } from "./store.mjs";

const DEFAULT_DRAFT_ROOT = "/tmp/flow-log-plan-drafts";

export function resolveDraftRoot() {
  return process.env.FLOW_LOG_PLAN_DRAFT_ROOT || DEFAULT_DRAFT_ROOT;
}

export function resolveDraftPath(feature) {
  return path.resolve(resolveDraftRoot(), `${feature}.draft.json`);
}

export function draftExists(feature) {
  return fs.existsSync(resolveDraftPath(feature));
}

export function loadDraft(feature) {
  const draftPath = resolveDraftPath(feature);

  if (!fs.existsSync(draftPath)) {
    throw new Error(`Draft file does not exist: ${draftPath}`);
  }

  return {
    draftPath,
    draft: readJsonFile(draftPath, "draft")
  };
}

export function loadDraftIfExists(feature) {
  const draftPath = resolveDraftPath(feature);
  if (!fs.existsSync(draftPath)) {
    return null;
  }

  return {
    draftPath,
    draft: readJsonFile(draftPath, "draft")
  };
}

export function saveDraft(feature, draft) {
  const draftPath = resolveDraftPath(feature);
  writeJsonAtomic(draftPath, draft);
  return draftPath;
}

export function createDraft(feature, canonicalPlan) {
  const draftPath = resolveDraftPath(feature);

  if (fs.existsSync(draftPath)) {
    return {
      created: false,
      draftPath,
      draft: readJsonFile(draftPath, "draft")
    };
  }

  const draft = createDraftFromPlan(canonicalPlan);
  writeJsonAtomic(draftPath, draft);

  return {
    created: true,
    draftPath,
    draft
  };
}

export function discardDraft(feature) {
  const draftPath = resolveDraftPath(feature);

  if (!fs.existsSync(draftPath)) {
    return { existed: false, draftPath };
  }

  fs.unlinkSync(draftPath);
  return { existed: true, draftPath };
}

export function getDraftStatus(feature) {
  const draftPath = resolveDraftPath(feature);

  if (!fs.existsSync(draftPath)) {
    return {
      exists: false,
      draftPath,
      lastModifiedAt: null
    };
  }

  const stats = fs.statSync(draftPath);
  return {
    exists: true,
    draftPath,
    lastModifiedAt: stats.mtime.toISOString()
  };
}
