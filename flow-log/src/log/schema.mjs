import { timestamp } from "./common.mjs";

export const ARTIFACT_TYPES = ["story", "e2e", "plan"];
export const REVIEW_NAMES = ["architectureReview", "codeReview"];
export const REVIEW_STATUSES = ["PENDING", "PASS", "FAIL", "BLOCKED"];
export const CHECK_NAMES = ["verifyQuick", "finalCheck", "karate"];
export const CHECK_STATUSES = ["NOT_RUN", "PASS", "FAIL", "BLOCKED"];
export const SLICE_RUN_TYPES = ["intermediate", "final"];
export const E2E_MODES = ["UNDECIDED", "REUSE_EXISTING", "SCENARIOS_REQUIRED"];
export const EVENT_TYPES = ["redCard", "rejection", "reroute", "note", "sliceRunStart", "sliceRunEnd", "archEscalationDecision"];
export const ARCH_ESCALATION_DECISIONS = ["PROCEED_TO_CODING", "FINAL_ADJUSTMENT", "ESCALATE_TO_USER"];
export const RISK_SEVERITIES = ["CRITICAL", "HIGH", "MEDIUM", "LOW", "UNCLASSIFIED"];
export const RISK_STATUSES = ["OPEN", "ADDRESSED", "INVALIDATED", "RESOLVED", "REOPENED", "ACCEPTED", "DEFERRED"];
export const FINDING_SEVERITIES = ["CRITICAL", "HIGH", "MEDIUM", "LOW"];
export const FINDING_STATUSES = ["OPEN", "FIXED", "DISPUTED", "RESOLVED", "REOPENED", "ACCEPTED", "DEFERRED"];
export const MAX_ARCHITECTURE_REVIEW_ROUNDS = 3;
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
    e2e: createE2EState(),
    artifacts: {
      story: createArtifactEntry(),
      e2e: createArtifactEntry(),
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
    sliceRuns: {
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

export function createE2EState() {
  return {
    mode: "UNDECIDED",
    decidedAt: null,
    decidedBy: null,
    reason: null
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
    reportPaths: [],
    logPath: null,
    sourceFingerprint: null
  };
}

export function validateStateShape(state, statePath) {
  const requiredKeys = [
    "schemaVersion",
    "feature",
    "requirements",
    "e2e",
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
