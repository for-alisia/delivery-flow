import { buildArtifactApprovalState } from "./artifacts.mjs";
import { summarizeBatches } from "./batches.mjs";
import { computeSourceFingerprint, isCheckStale, summarizeChecks } from "./checks.mjs";
import { summarizeEventCounts } from "./events.mjs";
import { buildCodeReviewGate, summarizeFindings } from "./findings.mjs";
import {
  buildSignoffReadiness,
  deriveCurrentPhase
} from "./gates.mjs";
import { buildArchitectureGate, summarizeRisks } from "./risks.mjs";
import { summarizeReviews } from "./reviews.mjs";

export function buildSummary(state, cwd, statePath) {
  const readiness = buildSignoffReadiness(state, cwd);

  return {
    feature: state.feature,
    statePath,
    updatedAt: state.updatedAt,
    timing: state.timing ?? null,
    currentPhase: deriveCurrentPhase(state, cwd),
    requirementsLocked: state.requirements.locked,
    requestSource: state.requirements.requestSource ?? null,
    artifacts: {
      story: summarizeArtifactCompact(cwd, "story", state.artifacts.story),
      plan: summarizeArtifactCompact(cwd, "plan", state.artifacts.plan)
    },
    reviews: summarizeReviews(state.reviews),
    checks: summarizeChecks(state.checks),
    architecturalRisks: summarizeRisks(state),
    codeFindings: summarizeFindings(state),
    batches: summarizeBatches(state),
    changedFileCount: state.changes.files.length,
    changedFiles: state.changes.files,
    events: summarizeEventCounts(state.events),
    readiness,
    nextActions: readiness.ready ? [] : readiness.reasons
  };
}

export function buildStatus(state, cwd, statePath) {
  const readiness = buildSignoffReadiness(state, cwd);
  const gate = buildArchitectureGate(state);
  const currentFingerprint = computeSourceFingerprint(cwd);
  const story = buildArtifactApprovalState(cwd, "story", state.artifacts.story);
  const plan = buildArtifactApprovalState(cwd, "plan", state.artifacts.plan);

  return {
    feature: state.feature,
    statePath,
    updatedAt: state.updatedAt,
    phase: deriveCurrentPhase(state, cwd),
    requirementsLocked: state.requirements.locked,
    storyApproved: story.approved && story.exists,
    storyStale: story.stale,
    planApproved: plan.approved && plan.exists,
    planStale: plan.stale,
    architectureReview: state.reviews.architectureReview.status,
    architectureGate: gate.gate,
    architectureReviewRound: state.architecturalRisks?.round ?? 0,
    codeReview: state.reviews.codeReview.status,
    codeReviewGate: buildCodeReviewGate(state).gate,
    codeReviewRound: state.codeFindings?.round ?? 0,
    verifyQuick: state.checks.verifyQuick.status,
    verifyQuickStale: isCheckStale(state.checks.verifyQuick, currentFingerprint),
    finalCheck: state.checks.finalCheck.status,
    finalCheckStale: isCheckStale(state.checks.finalCheck, currentFingerprint),
    karate: state.checks.karate.status,
    karateStale: isCheckStale(state.checks.karate, currentFingerprint),
    currentBatch: state.batches?.current?.batch ?? null,
    totalBatches: state.batches?.total ?? 0,
    readyForSignoff: readiness.ready,
    missing: readiness.reasons
  };
}

function summarizeArtifactCompact(cwd, artifactType, entry) {
  const approval = buildArtifactApprovalState(cwd, artifactType, entry);

  return {
    path: approval.path,
    exists: approval.exists,
    approved: approval.approved,
    approvedRevision: approval.approvedRevision,
    approvedHash: approval.approvedHash,
    approvalTracked: approval.approvalTracked,
    stale: approval.stale
  };
}
