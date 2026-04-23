import fs from "node:fs";
import {
  assertPlan,
  PLAN_SCHEMA_VERSION,
  createDraft,
  createInitialPlan,
  discardDraft,
  getDraftStatus,
  loadDraft,
  loadDraftIfExists,
  loadPlan,
  planExists,
  resolveDraftPath,
  resolvePlanPath,
  runFullDraftValidation,
  savePlan,
  acceptDraft,
  computePlanHash,
  plansEquivalentForAcceptance
} from "../plan/index.mjs";
import { optionalFlag, requiredFlag } from "../cli-helpers.mjs";
import { ensureFeatureMatches, loadState, resolveStatePath } from "../log/store.mjs";

export const DRAFT_PLAN_COMMAND_HELP = [
  "plan-create-draft --feature <name>",
  "plan-init-draft --feature <name> [--force]",
  "plan-validate-draft --feature <name> [--state-path <path>]",
  "plan-accept-draft --feature <name> [--state-path <path>]",
  "plan-discard-draft --feature <name>",
  "plan-draft-status --feature <name> [--state-path <path>]"
];

export function dispatchDraftPlanCommand(command, parsed, cwd) {
  switch (command) {
    case "plan-create-draft":
      return handlePlanCreateDraft(parsed, cwd);
    case "plan-init-draft":
      return handlePlanInitDraft(parsed, cwd);
    case "plan-validate-draft":
      return handlePlanValidateDraft(parsed, cwd);
    case "plan-accept-draft":
      return handlePlanAcceptDraft(parsed, cwd);
    case "plan-discard-draft":
      return handlePlanDiscardDraft(parsed, cwd);
    case "plan-draft-status":
      return handlePlanDraftStatus(parsed, cwd);
    default:
      return undefined;
  }
}

function handlePlanInitDraft(parsed, cwd) {
  const feature = requiredFlag(parsed, "feature");
  const planPath = resolvePlanPath(cwd, feature);
  const force = optionalFlag(parsed, "force") === true;

  if (!force && planExists(planPath)) {
    throw new Error(`Plan already exists: ${planPath}. Use --force to overwrite.`);
  }

  const plan = createInitialPlan(feature);
  plan.hash = computePlanHash(plan);
  savePlan(planPath, plan);

  const created = createDraft(feature, plan);

  return {
    ok: true,
    command: "plan-init-draft",
    feature,
    planPath,
    draftPath: created.draftPath,
    draftCreated: created.created,
    schemaVersion: plan.schemaVersion,
    revision: plan.revision,
    status: plan.status
  };
}

function handlePlanCreateDraft(parsed, cwd) {
  const feature = requiredFlag(parsed, "feature");
  const planPath = resolvePlanPath(cwd, feature);

  if (!planExists(planPath)) {
    throw new Error(`Cannot create draft: canonical plan does not exist at ${planPath}. Run plan-init-draft first.`);
  }

  const canonicalPlan = loadPlan(planPath);
  assertPlan(canonicalPlan, "canonical plan");

  const existingDraft = loadDraftIfExists(feature);
  if (existingDraft) {
    const hasChanges = !plansEquivalentForAcceptance(canonicalPlan, existingDraft.draft);
    if (hasChanges) {
      throw new Error(
        `Draft already exists with unapplied changes: ${existingDraft.draftPath}. ` +
        "Review it with plan-draft-status or discard it with plan-discard-draft before creating a fresh draft."
      );
    }

    return {
      ok: true,
      command: "plan-create-draft",
      feature,
      planPath,
      draftPath: existingDraft.draftPath,
      created: false,
      schemaVersion: existingDraft.draft.schemaVersion,
      revision: existingDraft.draft.revision,
      note: "Draft already exists and matches canonical plan."
    };
  }

  const created = createDraft(feature, canonicalPlan);

  return {
    ok: true,
    command: "plan-create-draft",
    feature,
    planPath,
    draftPath: created.draftPath,
    created: created.created,
    schemaVersion: created.draft.schemaVersion,
    revision: created.draft.revision,
    note: created.created ? "Draft created from canonical plan." : "Draft already exists; returning current draft."
  };
}

function handlePlanValidateDraft(parsed, cwd) {
  const feature = requiredFlag(parsed, "feature");
  const explicitStatePath = optionalFlag(parsed, "state-path");
  const planPath = resolvePlanPath(cwd, feature);

  if (!planExists(planPath)) {
    throw new Error(`Canonical plan does not exist: ${planPath}`);
  }

  const canonicalPlan = loadPlan(planPath);
  assertPlan(canonicalPlan, "canonical plan");

  const { draftPath, draft } = loadDraft(feature);
  assertPlan(draft, "draft");

  const validation = runFullDraftValidation(draft, cwd, feature, explicitStatePath);

  return {
    ok: true,
    command: "plan-validate-draft",
    feature,
    planPath,
    draftPath,
    valid: validation.valid,
    issues: validation.issues,
    shape: validation.shape,
    readiness: validation.readiness,
    riskLinks: validation.riskLinks
  };
}

function handlePlanAcceptDraft(parsed, cwd) {
  const feature = requiredFlag(parsed, "feature");
  const explicitStatePath = optionalFlag(parsed, "state-path");
  const planPath = resolvePlanPath(cwd, feature);

  if (!planExists(planPath)) {
    throw new Error(`Canonical plan does not exist: ${planPath}`);
  }

  const canonicalPlan = loadPlan(planPath);
  assertPlan(canonicalPlan, "canonical plan");

  const { draftPath, draft } = loadDraft(feature);
  assertPlan(draft, "draft");

  const result = acceptDraft({
    cwd,
    feature,
    planPath,
    canonicalPlan,
    draft,
    explicitStatePath
  });

  if (!result.accepted) {
    const issues = result.validation?.issues ?? ["Draft acceptance failed."];
    throw new Error(`Draft validation failed: ${issues.join(" | ")}`);
  }

  return {
    ok: true,
    command: "plan-accept-draft",
    feature,
    planPath,
    draftPath,
    accepted: result.accepted,
    changed: result.changed,
    revision: result.revision ?? canonicalPlan.revision,
    hash: result.hash ?? computePlanHash(canonicalPlan),
    validation: result.validation,
    approvalInvalidation: result.approvalInvalidation
  };
}

function handlePlanDiscardDraft(parsed, _cwd) {
  const feature = requiredFlag(parsed, "feature");
  const result = discardDraft(feature);

  return {
    ok: true,
    command: "plan-discard-draft",
    feature,
    draftPath: result.draftPath,
    discarded: result.existed
  };
}

function handlePlanDraftStatus(parsed, cwd) {
  const feature = requiredFlag(parsed, "feature");
  const explicitStatePath = optionalFlag(parsed, "state-path");
  const draftStatus = getDraftStatus(feature);
  const planPath = resolvePlanPath(cwd, feature);

  let hasChanges = false;
  let wouldInvalidateApproval = false;
  let currentRevision = null;
  let currentHash = null;

  if (draftStatus.exists && planExists(planPath)) {
    const canonicalPlan = loadPlan(planPath);
    if (canonicalPlan?.schemaVersion === PLAN_SCHEMA_VERSION) {
      currentRevision = canonicalPlan.revision;
      currentHash = computePlanHash(canonicalPlan);

      const loadedDraft = loadDraftIfExists(feature);
      if (loadedDraft) {
        hasChanges = !plansEquivalentForAcceptance(canonicalPlan, loadedDraft.draft);
      }

      const approval = readPlanApprovalState(cwd, feature, explicitStatePath);
      wouldInvalidateApproval = hasChanges && approval.approved;
    }
  }

  return {
    ok: true,
    command: "plan-draft-status",
    feature,
    planPath,
    draftPath: draftStatus.draftPath,
    exists: draftStatus.exists,
    lastModifiedAt: draftStatus.lastModifiedAt,
    hasChanges,
    currentRevision,
    currentHash,
    wouldInvalidateApproval
  };
}

function readPlanApprovalState(cwd, feature, explicitStatePath) {
  const statePath = resolveStatePath(cwd, feature, explicitStatePath);

  if (!fs.existsSync(statePath)) {
    return { approved: false, stateFound: false };
  }

  const state = loadState(statePath);
  ensureFeatureMatches(state, feature, statePath);

  return {
    approved: state.artifacts?.plan?.approved === true,
    stateFound: true,
    statePath
  };
}
