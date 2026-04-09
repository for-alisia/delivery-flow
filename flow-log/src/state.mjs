import fs from "node:fs";
import path from "node:path";

export const ARTIFACT_TYPES = ["story", "plan"];
export const REVIEW_NAMES = ["architectureReview", "codeReview"];
export const REVIEW_STATUSES = ["PENDING", "PASS", "FAIL", "BLOCKED"];
export const CHECK_NAMES = ["verifyQuick", "finalCheck", "karate"];
export const CHECK_STATUSES = ["NOT_RUN", "PASS", "FAIL", "BLOCKED"];
export const EVENT_TYPES = ["redCard", "rejection", "reroute", "note", "batchStart", "batchEnd"];
export const RISK_SEVERITIES = ["CRITICAL", "HIGH", "MEDIUM", "LOW"];
export const RISK_STATUSES = ["OPEN", "ADDRESSED", "INVALIDATED", "RESOLVED", "REOPENED"];
export const FINDING_SEVERITIES = ["CRITICAL", "HIGH", "MEDIUM", "LOW"];
export const FINDING_STATUSES = ["OPEN", "FIXED", "DISPUTED", "RESOLVED", "REOPENED"];
export const MAX_ARCHITECTURE_REVIEW_ROUNDS = 3;
export const MAX_CODE_REVIEW_ROUNDS = 3;

export function resolveStatePath(cwd, feature, explicitStatePath) {
  if (explicitStatePath) {
    return path.resolve(cwd, explicitStatePath);
  }

  return path.resolve(cwd, "artifacts", "flow-logs", `${feature}.json`);
}

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

function createArtifactEntry() {
  return {
    path: null,
    approved: false,
    approvedAt: null,
    approvedBy: null,
    lastVerifiedExistsAt: null
  };
}

function createReviewEntry() {
  return {
    status: "PENDING",
    reason: null,
    updatedAt: null,
    updatedBy: null
  };
}

function createCheckEntry() {
  return {
    status: "NOT_RUN",
    updatedAt: null,
    updatedBy: null,
    command: null,
    details: null,
    reportPaths: []
  };
}

export function loadState(statePath) {
  if (!fs.existsSync(statePath)) {
    throw new Error(`State file does not exist: ${statePath}`);
  }

  const raw = fs.readFileSync(statePath, "utf8");
  const state = JSON.parse(raw);
  validateStateShape(state, statePath);
  return state;
}

export function saveState(statePath, state) {
  state.updatedAt = timestamp();
  fs.mkdirSync(path.dirname(statePath), { recursive: true });
  fs.writeFileSync(statePath, `${JSON.stringify(state, null, 2)}\n`);
}

export function ensureFeatureMatches(state, feature, statePath) {
  if (state.feature !== feature) {
    throw new Error(
      `Feature mismatch for state file ${statePath}: expected '${feature}', found '${state.feature}'`
    );
  }
}

export function validateValue(value, allowedValues, label) {
  if (!allowedValues.includes(value)) {
    throw new Error(`${label} must be one of: ${allowedValues.join(", ")}`);
  }
}

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

export function appendChangedFiles(state, files) {
  const next = new Set(state.changes.files);

  for (const file of files) {
    next.add(file);
  }

  state.changes.files = Array.from(next).sort();
}

export function appendEvent(state, event) {
  state.events.push({
    id: nextEventId(state.events),
    type: event.type,
    by: event.by ?? null,
    reason: event.reason,
    target: event.target ?? null,
    relatedCheck: event.relatedCheck ?? null,
    relatedReview: event.relatedReview ?? null,
    createdAt: timestamp()
  });
}

export function buildSignoffReadiness(state, cwd) {
  const checks = {
    requirementsLocked: state.requirements.locked,
    storyRegistered: Boolean(state.artifacts.story.path),
    storyExists: artifactExists(cwd, state.artifacts.story.path),
    storyApproved: state.artifacts.story.approved,
    planRegistered: Boolean(state.artifacts.plan.path),
    planExists: artifactExists(cwd, state.artifacts.plan.path),
    planApproved: state.artifacts.plan.approved,
    architectureReviewPassed: state.reviews.architectureReview.status === "PASS",
    codeReviewPassed: state.reviews.codeReview.status === "PASS",
    finalCheckPassed: state.checks.finalCheck.status === "PASS",
    karatePassed: state.checks.karate.status === "PASS"
  };

  const reasons = [];

  if (!checks.requirementsLocked) {
    reasons.push("Requirements must be locked.");
  }
  if (!checks.storyRegistered) {
    reasons.push("Story artifact path must be registered.");
  }
  if (checks.storyRegistered && !checks.storyExists) {
    reasons.push("Story artifact path does not exist on disk.");
  }
  if (!checks.storyApproved) {
    reasons.push("Story must be approved.");
  }
  if (!checks.planRegistered) {
    reasons.push("Plan artifact path must be registered.");
  }
  if (checks.planRegistered && !checks.planExists) {
    reasons.push("Plan artifact path does not exist on disk.");
  }
  if (!checks.planApproved) {
    reasons.push("Plan must be approved.");
  }
  if (!checks.architectureReviewPassed) {
    reasons.push("architectureReview must be PASS.");
  }
  if (!checks.codeReviewPassed) {
    reasons.push("codeReview must be PASS.");
  }
  if (!checks.finalCheckPassed) {
    reasons.push("finalCheck must be PASS.");
  }
  if (!checks.karatePassed) {
    reasons.push("karate must be PASS.");
  }

  return {
    ready: reasons.length === 0,
    target: "signoff",
    checks,
    reasons
  };
}

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
    finalCheck: state.checks.finalCheck.status,
    karate: state.checks.karate.status,
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
    approved: entry.approved
  };
}

function deriveCurrentPhase(state) {
  if (!state.requirements.locked) {
    return "requirement-lock";
  }
  if (!state.artifacts.story.approved) {
    return "story";
  }
  if (!state.artifacts.plan.approved) {
    return "plan";
  }
  if (state.reviews.architectureReview.status !== "PASS") {
    return "architecture-review";
  }
  if (state.reviews.codeReview.status !== "PASS") {
    return "implementation";
  }
  if (state.checks.finalCheck.status !== "PASS" || state.checks.karate.status !== "PASS") {
    return "verification";
  }

  return "signoff-ready";
}

function summarizeEventCounts(events) {
  const counts = {
    redCard: 0,
    rejection: 0,
    reroute: 0,
    note: 0
  };

  for (const event of events) {
    counts[event.type] += 1;
  }

  return {
    total: events.length,
    counts
  };
}

function summarizeReviews(reviews) {
  return {
    architectureReview: reviews.architectureReview.status,
    codeReview: reviews.codeReview.status
  };
}

function summarizeChecks(checks) {
  return {
    verifyQuick: checks.verifyQuick.status,
    finalCheck: checks.finalCheck.status,
    karate: checks.karate.status
  };
}

function summarizeBatches(state) {
  if (!state.batches) {
    return { current: null, completed: 0, total: 0 };
  }

  return {
    current: state.batches.current?.batch ?? null,
    completed: state.batches.history.length,
    total: state.batches.total
  };
}

function summarizeRisks(state) {
  const section = state.architecturalRisks ?? { round: 0, risks: [] };
  const risks = section.risks;

  const bySeverity = { CRITICAL: 0, HIGH: 0, MEDIUM: 0, LOW: 0 };
  const byStatus = { OPEN: 0, ADDRESSED: 0, INVALIDATED: 0, RESOLVED: 0, REOPENED: 0 };

  for (const risk of risks) {
    bySeverity[risk.severity] = (bySeverity[risk.severity] ?? 0) + 1;
    byStatus[risk.status] = (byStatus[risk.status] ?? 0) + 1;
  }

  return {
    round: section.round,
    total: risks.length,
    bySeverity,
    byStatus,
    risks: risks.map((r) => ({
      id: r.id,
      severity: r.severity,
      status: r.status,
      description: r.description,
      responseNote: r.responseNote
    }))
  };
}

function summarizeFindings(state) {
  const section = state.codeFindings ?? { round: 0, findings: [] };
  const findings = section.findings;

  const bySeverity = { CRITICAL: 0, HIGH: 0, MEDIUM: 0, LOW: 0 };
  const byStatus = { OPEN: 0, FIXED: 0, DISPUTED: 0, RESOLVED: 0, REOPENED: 0 };

  for (const finding of findings) {
    bySeverity[finding.severity] = (bySeverity[finding.severity] ?? 0) + 1;
    byStatus[finding.status] = (byStatus[finding.status] ?? 0) + 1;
  }

  return {
    round: section.round,
    total: findings.length,
    bySeverity,
    byStatus,
    findings: findings.map((f) => ({
      id: f.id,
      severity: f.severity,
      status: f.status,
      description: f.description,
      file: f.file,
      responseNote: f.responseNote
    }))
  };
}

export function startBatch(state, slices, by) {
  const now = timestamp();
  const batchNumber = state.batches.total + 1;

  state.batches.current = {
    batch: batchNumber,
    slices: slices ?? [],
    startedAt: now,
    completedAt: null,
    status: "in-progress",
    by: by ?? null
  };
  state.batches.total = batchNumber;
}

export function completeBatch(state, status) {
  if (!state.batches.current) {
    throw new Error("No batch is currently in progress.");
  }

  const now = timestamp();
  state.batches.current.completedAt = now;
  state.batches.current.status = status ?? "complete";
  state.batches.history.push(state.batches.current);
  state.batches.current = null;
}

export function resetChecksForRedCard(state) {
  state.checks.verifyQuick = createCheckEntry();
  state.checks.finalCheck = createCheckEntry();
  state.checks.karate = createCheckEntry();
}

export function completeFlow(state) {
  const now = timestamp();
  state.timing.completedAt = now;

  const start = new Date(state.timing.startedAt).getTime();
  const end = new Date(now).getTime();
  state.timing.durationMinutes = Math.round((end - start) / 60000);
}

export function addRisk(state, severity, description, by) {
  validateValue(severity, RISK_SEVERITIES, "risk severity");
  ensureRisksSection(state);

  const id = nextRiskId(state.architecturalRisks.risks);

  const risk = {
    id,
    severity,
    description,
    status: "OPEN",
    round: state.architecturalRisks.round,
    createdBy: by ?? null,
    createdAt: timestamp(),
    responseNote: null,
    respondedBy: null,
    respondedAt: null,
    resolvedBy: null,
    resolvedAt: null
  };

  state.architecturalRisks.risks.push(risk);
  return risk;
}

export function respondToRisk(state, riskId, newStatus, note, by) {
  ensureRisksSection(state);
  validateValue(newStatus, ["ADDRESSED", "INVALIDATED"], "response status");

  const risk = findRisk(state, riskId);

  if (risk.status !== "OPEN" && risk.status !== "REOPENED") {
    throw new Error(`Risk ${riskId} is '${risk.status}' — can only respond to OPEN or REOPENED risks.`);
  }

  risk.status = newStatus;
  risk.responseNote = note ?? null;
  risk.respondedBy = by ?? null;
  risk.respondedAt = timestamp();
  return risk;
}

export function resolveRisk(state, riskId, by) {
  ensureRisksSection(state);
  const risk = findRisk(state, riskId);

  if (risk.status !== "ADDRESSED" && risk.status !== "INVALIDATED") {
    throw new Error(`Risk ${riskId} is '${risk.status}' — can only resolve ADDRESSED or INVALIDATED risks.`);
  }

  risk.status = "RESOLVED";
  risk.resolvedBy = by ?? null;
  risk.resolvedAt = timestamp();
  return risk;
}

export function reopenRisk(state, riskId, reason, by) {
  ensureRisksSection(state);
  const risk = findRisk(state, riskId);

  if (risk.status !== "ADDRESSED" && risk.status !== "INVALIDATED") {
    throw new Error(`Risk ${riskId} is '${risk.status}' — can only reopen ADDRESSED or INVALIDATED risks.`);
  }

  risk.status = "REOPENED";
  risk.responseNote = reason ?? risk.responseNote;
  risk.respondedBy = null;
  risk.respondedAt = null;
  risk.resolvedBy = null;
  risk.resolvedAt = null;
  return risk;
}

export function incrementReviewRound(state) {
  ensureRisksSection(state);
  state.architecturalRisks.round += 1;
  return state.architecturalRisks.round;
}

export function buildArchitectureGate(state) {
  ensureRisksSection(state);

  const risks = state.architecturalRisks.risks;
  const round = state.architecturalRisks.round;

  const unresolvedBlocking = risks.filter(
    (r) => (r.severity === "CRITICAL" || r.severity === "HIGH") &&
           r.status !== "RESOLVED"
  );

  const allResponded = risks
    .filter((r) => r.status === "OPEN" || r.status === "REOPENED")
    .length === 0;

  if (unresolvedBlocking.length === 0) {
    return {
      gate: "PASS",
      round,
      unresolvedBlocking: 0,
      totalRisks: risks.length,
      message: "No unresolved Critical/High risks. Ready for implementation."
    };
  }

  if (round >= MAX_ARCHITECTURE_REVIEW_ROUNDS) {
    return {
      gate: "ESCALATE",
      round,
      unresolvedBlocking: unresolvedBlocking.length,
      totalRisks: risks.length,
      message: `${unresolvedBlocking.length} unresolved Critical/High risk(s) after ${round} round(s). Escalate to user.`
    };
  }

  return {
    gate: "FAIL",
    round,
    unresolvedBlocking: unresolvedBlocking.length,
    allResponded,
    totalRisks: risks.length,
    message: `${unresolvedBlocking.length} unresolved Critical/High risk(s) in round ${round}.`
  };
}

function findRisk(state, riskId) {
  const risk = state.architecturalRisks.risks.find((r) => r.id === riskId);
  if (!risk) {
    throw new Error(`Risk not found: ${riskId}`);
  }
  return risk;
}

function nextRiskId(risks) {
  if (risks.length === 0) {
    return 1;
  }
  return risks[risks.length - 1].id + 1;
}

function ensureRisksSection(state) {
  if (!state.architecturalRisks) {
    state.architecturalRisks = { round: 0, risks: [] };
  }
}

// --- Code Findings (Code Review) ---

export function addFinding(state, severity, description, file, by) {
  validateValue(severity, FINDING_SEVERITIES, "finding severity");
  ensureFindingsSection(state);

  const id = nextFindingId(state.codeFindings.findings);

  const finding = {
    id,
    severity,
    description,
    file: file ?? null,
    status: "OPEN",
    round: state.codeFindings.round,
    createdBy: by ?? null,
    createdAt: timestamp(),
    responseNote: null,
    respondedBy: null,
    respondedAt: null,
    resolvedBy: null,
    resolvedAt: null
  };

  state.codeFindings.findings.push(finding);
  return finding;
}

export function respondToFinding(state, findingId, newStatus, note, by) {
  ensureFindingsSection(state);
  validateValue(newStatus, ["FIXED", "DISPUTED"], "response status");

  const finding = findFinding(state, findingId);

  if (finding.status !== "OPEN" && finding.status !== "REOPENED") {
    throw new Error(`Finding ${findingId} is '${finding.status}' — can only respond to OPEN or REOPENED findings.`);
  }

  finding.status = newStatus;
  finding.responseNote = note ?? null;
  finding.respondedBy = by ?? null;
  finding.respondedAt = timestamp();
  return finding;
}

export function resolveFinding(state, findingId, by) {
  ensureFindingsSection(state);
  const finding = findFinding(state, findingId);

  if (finding.status !== "FIXED" && finding.status !== "DISPUTED") {
    throw new Error(`Finding ${findingId} is '${finding.status}' — can only resolve FIXED or DISPUTED findings.`);
  }

  finding.status = "RESOLVED";
  finding.resolvedBy = by ?? null;
  finding.resolvedAt = timestamp();
  return finding;
}

export function reopenFinding(state, findingId, reason, by) {
  ensureFindingsSection(state);
  const finding = findFinding(state, findingId);

  if (finding.status !== "FIXED" && finding.status !== "DISPUTED") {
    throw new Error(`Finding ${findingId} is '${finding.status}' — can only reopen FIXED or DISPUTED findings.`);
  }

  finding.status = "REOPENED";
  finding.responseNote = reason ?? finding.responseNote;
  finding.respondedBy = null;
  finding.respondedAt = null;
  finding.resolvedBy = null;
  finding.resolvedAt = null;
  return finding;
}

export function incrementCodeReviewRound(state) {
  ensureFindingsSection(state);
  state.codeFindings.round += 1;
  return state.codeFindings.round;
}

export function buildCodeReviewGate(state) {
  ensureFindingsSection(state);

  const findings = state.codeFindings.findings;
  const round = state.codeFindings.round;

  const unresolvedBlocking = findings.filter(
    (f) => (f.severity === "CRITICAL" || f.severity === "HIGH") &&
           f.status !== "RESOLVED"
  );

  if (unresolvedBlocking.length === 0) {
    return {
      gate: "PASS",
      round,
      unresolvedBlocking: 0,
      totalFindings: findings.length,
      message: "No unresolved Critical/High findings. Code review passed."
    };
  }

  if (round >= MAX_CODE_REVIEW_ROUNDS) {
    return {
      gate: "ESCALATE",
      round,
      unresolvedBlocking: unresolvedBlocking.length,
      totalFindings: findings.length,
      message: `${unresolvedBlocking.length} unresolved Critical/High finding(s) after ${round} round(s). Escalate to user.`
    };
  }

  return {
    gate: "FAIL",
    round,
    unresolvedBlocking: unresolvedBlocking.length,
    totalFindings: findings.length,
    message: `${unresolvedBlocking.length} unresolved Critical/High finding(s) in round ${round}.`
  };
}

function findFinding(state, findingId) {
  const finding = state.codeFindings.findings.find((f) => f.id === findingId);
  if (!finding) {
    throw new Error(`Finding not found: ${findingId}`);
  }
  return finding;
}

function nextFindingId(findings) {
  if (findings.length === 0) {
    return 1;
  }
  return findings[findings.length - 1].id + 1;
}

function ensureFindingsSection(state) {
  if (!state.codeFindings) {
    state.codeFindings = { round: 0, findings: [] };
  }
}

function validateStateShape(state, statePath) {
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

function timestamp() {
  return new Date().toISOString();
}

function nextEventId(events) {
  if (events.length === 0) {
    return 1;
  }

  return events[events.length - 1].id + 1;
}
