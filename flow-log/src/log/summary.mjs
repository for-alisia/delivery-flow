import { artifactExists } from "./artifacts.mjs";
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
    currentPhase: deriveCurrentPhase(state),
    requirementsLocked: state.requirements.locked,
    requestSource: state.requirements.requestSource ?? null,
    artifacts: {
      story: summarizeArtifactCompact(state.artifacts.story, cwd),
      plan: summarizeArtifactCompact(state.artifacts.plan, cwd)
    },
    reviews: summarizeReviews(state.reviews),
    checks: summarizeChecks(state.checks),
    architecturalRisks: summarizeRisks(state),
    codeFindings: summarizeFindings(state),
    batches: summarizeBatches(state),
    changedFileCount: state.changes.files.length,
    events: summarizeEventCounts(state.events),
    readiness,
    nextActions: readiness.ready ? [] : readiness.reasons
  };
}

export function buildStatus(state, cwd, statePath) {
  const readiness = buildSignoffReadiness(state, cwd);
  const gate = buildArchitectureGate(state);
  const currentFingerprint = computeSourceFingerprint(cwd);

  return {
    feature: state.feature,
    statePath,
    updatedAt: state.updatedAt,
    phase: deriveCurrentPhase(state),
    requirementsLocked: state.requirements.locked,
    storyApproved: state.artifacts.story.approved && artifactExists(cwd, state.artifacts.story.path),
    planApproved: state.artifacts.plan.approved && artifactExists(cwd, state.artifacts.plan.path),
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

function summarizeArtifactCompact(entry, cwd) {
  return {
    path: entry.path,
    exists: artifactExists(cwd, entry.path),
    approved: entry.approved,
    approvedRevision: entry.approvedRevision ?? null,
    approvedHash: entry.approvedHash ?? null
  };
}
