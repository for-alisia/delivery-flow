import fs from "node:fs";
import path from "node:path";
import { computePlanHash } from "./hash.mjs";
import { ensureFeatureMatches, loadState, resolveStatePath } from "../log/store.mjs";

export function buildPlanSummary(plan, cwd, feature, explicitStatePath, canonicalPlanPath) {
  const currentHash = typeof plan.hash === "string" ? plan.hash : computePlanHash(plan);
  const approval = buildApprovalSummary(plan, cwd, feature, currentHash, explicitStatePath, canonicalPlanPath);

  return {
    feature: plan.feature,
    revision: plan.revision,
    status: plan.status,
    schemaVersion: plan.schemaVersion,
    hash: currentHash,
    sectionCounts: {
      implementationFlow: (plan.implementationFlow ?? []).length,
      slices: plan.slices.length,
      models: plan.models.length,
      classes: plan.classes.length,
      contractExamples: (plan.contractExamples ?? []).length,
      validationRules: plan.validationRules.length,
      designDecisions: plan.designDecisions.length,
      verificationSlices: (plan.verification?.slices ?? []).length,
      verificationFinalGates: (plan.verification?.finalGates ?? []).length
    },
    approval
  };
}

function buildApprovalSummary(plan, cwd, feature, currentHash, explicitStatePath, canonicalPlanPath) {
  const statePath = resolveStatePath(cwd, feature, explicitStatePath);

  if (!fs.existsSync(statePath)) {
    return {
      stateFound: false,
      statePath,
      approved: false,
      approvedRevision: null,
      approvedHash: null,
      currentRevision: plan.revision,
      currentHash,
      stale: false
    };
  }

  const state = loadState(statePath);
  ensureFeatureMatches(state, feature, statePath);

  const artifact = state.artifacts?.plan ?? null;
  const approvedRevision = artifact?.approvedRevision ?? null;
  const approvedHash = artifact?.approvedHash ?? null;
  const approved = artifact?.approved === true;

  const stale = approved && (
    approvedRevision !== plan.revision ||
    (typeof approvedHash === "string" && approvedHash !== currentHash)
  );

  const registeredPath = artifact?.path ?? null;
  const resolvedRegisteredPath = registeredPath ? path.resolve(cwd, registeredPath) : null;
  const resolvedCanonicalPath = canonicalPlanPath ? path.resolve(canonicalPlanPath) : null;

  return {
    stateFound: true,
    statePath,
    approved,
    approvedRevision,
    approvedHash,
    currentRevision: plan.revision,
    currentHash,
    stale,
    registeredPlanPath: registeredPath,
    registeredPlanPathMatchesCurrent: resolvedRegisteredPath === null || resolvedCanonicalPath === null
      ? null
      : resolvedRegisteredPath === resolvedCanonicalPath
  };
}
