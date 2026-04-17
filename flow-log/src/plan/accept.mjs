import fs from "node:fs";
import { saveDraft } from "./draft-store.mjs";
import { computePlanHash, plansEquivalentForAcceptance } from "./hash.mjs";
import { PLAN_V3_SCHEMA_VERSION } from "./schema.mjs";
import { savePlan } from "./store.mjs";
import { validatePlanReadiness } from "./validate-readiness.mjs";
import { validateRiskLinks } from "./validate-risk-links.mjs";
import { validatePlanShape } from "./validate-shape.mjs";
import { ensureFeatureMatches, loadState, resolveStatePath, saveState } from "../log/store.mjs";

export function runFullDraftValidation(draft, cwd, feature, explicitStatePath) {
  const shape = validatePlanShape(draft);
  const readiness = validatePlanReadiness(draft);
  const riskLinks = validateRiskLinks(draft, cwd, feature, explicitStatePath);

  const issues = [
    ...shape.issues.map((issue) => `[shape] ${issue}`),
    ...readiness.issues.map((issue) => `[readiness] ${issue}`),
    ...riskLinks.issues.map((issue) => `[risk-links] ${issue}`)
  ];

  return {
    valid: issues.length === 0,
    issues,
    shape,
    readiness,
    riskLinks
  };
}

export function acceptDraft(options) {
  const {
    cwd,
    feature,
    planPath,
    canonicalPlan,
    draft,
    explicitStatePath
  } = options;

  const validation = runFullDraftValidation(draft, cwd, feature, explicitStatePath);

  if (!validation.valid) {
    return {
      accepted: false,
      changed: false,
      validation
    };
  }

  if (draft.schemaVersion !== PLAN_V3_SCHEMA_VERSION) {
    return {
      accepted: false,
      changed: false,
      validation: {
        ...validation,
        valid: false,
        issues: [...validation.issues, `[shape] schemaVersion must be '${PLAN_V3_SCHEMA_VERSION}'.`]
      }
    };
  }

  const changed = !plansEquivalentForAcceptance(canonicalPlan, draft);
  const canonicalHash = typeof canonicalPlan.hash === "string"
    ? canonicalPlan.hash
    : computePlanHash(canonicalPlan);

  if (!changed) {
    return {
      accepted: true,
      changed: false,
      revision: canonicalPlan.revision,
      hash: canonicalHash,
      validation,
      approvalInvalidation: {
        stateFound: false,
        invalidated: false,
        statePath: null
      }
    };
  }

  const nextPlan = structuredClone(draft);
  nextPlan.feature = feature;
  nextPlan.schemaVersion = PLAN_V3_SCHEMA_VERSION;
  nextPlan.revision = canonicalPlan.revision + 1;
  nextPlan.hash = computePlanHash(nextPlan);

  savePlan(planPath, nextPlan);
  saveDraft(feature, nextPlan);

  const approvalInvalidation = invalidatePlanApproval(cwd, feature, explicitStatePath);

  return {
    accepted: true,
    changed: true,
    revision: nextPlan.revision,
    hash: nextPlan.hash,
    validation,
    approvalInvalidation,
    plan: nextPlan
  };
}

function invalidatePlanApproval(cwd, feature, explicitStatePath) {
  const statePath = resolveStatePath(cwd, feature, explicitStatePath);

  if (!fs.existsSync(statePath)) {
    return {
      stateFound: false,
      invalidated: false,
      statePath: null
    };
  }

  const state = loadState(statePath);
  ensureFeatureMatches(state, feature, statePath);

  const artifact = state.artifacts?.plan;
  if (!artifact) {
    return {
      stateFound: true,
      invalidated: false,
      statePath
    };
  }

  const invalidated = artifact.approved === true || artifact.approvedRevision !== null || artifact.approvedHash !== null;

  artifact.approved = false;
  artifact.approvedAt = null;
  artifact.approvedBy = null;
  artifact.approvedRevision = null;
  artifact.approvedHash = null;

  saveState(statePath, state);

  return {
    stateFound: true,
    invalidated,
    statePath
  };
}
