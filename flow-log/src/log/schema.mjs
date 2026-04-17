import { timestamp } from "./common.mjs";

export const ARTIFACT_TYPES = ["story", "plan"];
export const REVIEW_NAMES = ["architectureReview", "codeReview"];
export const REVIEW_STATUSES = ["PENDING", "PASS", "FAIL", "BLOCKED"];
export const CHECK_NAMES = ["verifyQuick", "finalCheck", "karate"];
export const CHECK_STATUSES = ["NOT_RUN", "PASS", "FAIL", "BLOCKED"];
export const EVENT_TYPES = ["redCard", "rejection", "reroute", "note", "batchStart", "batchEnd", "archEscalationDecision"];
export const ARCH_ESCALATION_DECISIONS = ["PROCEED_TO_CODING", "FINAL_ADJUSTMENT", "ESCALATE_TO_USER"];
export const RISK_SEVERITIES = ["CRITICAL", "HIGH", "MEDIUM", "LOW"];
export const RISK_STATUSES = ["OPEN", "ADDRESSED", "INVALIDATED", "RESOLVED", "REOPENED"];
export const FINDING_SEVERITIES = ["CRITICAL", "HIGH", "MEDIUM", "LOW"];
export const FINDING_STATUSES = ["OPEN", "FIXED", "DISPUTED", "RESOLVED", "REOPENED"];
export const MAX_ARCHITECTURE_REVIEW_ROUNDS = 5;
export const MAX_CODE_REVIEW_ROUNDS = 3;

export function createInitialState(feature) {
  const now = timestamp();

  return {
    schemaVersion: "1.0",
    feature,
    createdAt: now,
    updatedAt: now,
    requirements: {
      locked: false,
      lockedAt: null,
      lockedBy: null,
      requestSource: null
    },
    artifacts: {
      story: createArtifactEntry(),
      plan: createArtifactEntry()
    },
    reviews: {
      architectureReview: createReviewEntry(),
      codeReview: createReviewEntry()
    },
    checks: {
      verifyQuick: createCheckEntry(),
      finalCheck: createCheckEntry(),
      karate: createCheckEntry()
    },
    timing: {
      startedAt: now,
      completedAt: null,
      durationMinutes: null
    },
    batches: {
      current: null,
      total: 0,
      history: []
    },
    changes: {
      files: []
    },
    architecturalRisks: {
      round: 0,
      risks: []
    },
    codeFindings: {
      round: 0,
      findings: []
    },
    events: []
  };
}

export function createArtifactEntry() {
  return {
    path: null,
    approved: false,
    approvedAt: null,
    approvedBy: null,
    approvedRevision: null,
    approvedHash: null,
    lastVerifiedExistsAt: null
  };
}

export function createReviewEntry() {
  return {
    status: "PENDING",
    reason: null,
    updatedAt: null,
    updatedBy: null
  };
}

export function createCheckEntry() {
  return {
    status: "NOT_RUN",
    updatedAt: null,
    updatedBy: null,
    command: null,
    details: null,
    reportPaths: []
  };
}

export function validateStateShape(state, statePath) {
  const requiredKeys = [
    "schemaVersion",
    "feature",
    "requirements",
    "artifacts",
    "reviews",
    "checks",
    "changes",
    "events"
  ];

  for (const key of requiredKeys) {
    if (!(key in state)) {
      throw new Error(`Invalid state file ${statePath}: missing '${key}'`);
    }
  }
}
