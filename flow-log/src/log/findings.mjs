import { nextFindingId, timestamp, validateValue } from "./common.mjs";
import { FINDING_SEVERITIES, MAX_CODE_REVIEW_ROUNDS } from "./schema.mjs";

const NON_BLOCKING_FINDING_SEVERITIES = new Set(["MEDIUM", "LOW"]);
const DECISION_STATUSES = new Set(["ACCEPTED", "DEFERRED"]);

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
    decisionReason: null,
    decisionBy: null,
    decisionAt: null,
    followUpRef: null,
    resolvedBy: null,
    resolvedAt: null
  };

  state.codeFindings.findings.push(finding);
  return finding;
}

export function decideFinding(state, findingId, newStatus, reason, by, followUpRef) {
  ensureFindingsSection(state);
  validateValue(newStatus, Array.from(DECISION_STATUSES), "finding decision status");

  const finding = findFinding(state, findingId);

  if (!NON_BLOCKING_FINDING_SEVERITIES.has(finding.severity)) {
    throw new Error(`Finding ${findingId} has severity '${finding.severity}' — only MEDIUM or LOW findings can be ACCEPTED or DEFERRED.`);
  }

  if (finding.status === "RESOLVED") {
    throw new Error(`Finding ${findingId} is '${finding.status}' — resolved findings cannot be marked ACCEPTED or DEFERRED.`);
  }

  finding.status = newStatus;
  finding.decisionReason = reason ?? null;
  finding.decisionBy = by ?? null;
  finding.decisionAt = timestamp();
  finding.followUpRef = followUpRef ?? null;

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

export function reopenFinding(state, findingId, reason) {
  ensureFindingsSection(state);
  const finding = findFinding(state, findingId);

  if (finding.status !== "FIXED" && finding.status !== "DISPUTED" && !DECISION_STATUSES.has(finding.status)) {
    throw new Error(`Finding ${findingId} is '${finding.status}' — can only reopen FIXED, DISPUTED, ACCEPTED, or DEFERRED findings.`);
  }

  finding.status = "REOPENED";
  finding.responseNote = reason ?? finding.responseNote;
  finding.respondedBy = null;
  finding.respondedAt = null;
  finding.decisionReason = null;
  finding.decisionBy = null;
  finding.decisionAt = null;
  finding.followUpRef = null;
  finding.resolvedBy = null;
  finding.resolvedAt = null;

  return finding;
}

export function incrementCodeReviewRound(state) {
  ensureFindingsSection(state);
  state.codeFindings.round += 1;
  return state.codeFindings.round;
}

export function getUnresolvedBlockingFindings(state) {
  ensureFindingsSection(state);

  return state.codeFindings.findings.filter(
    (finding) => (finding.severity === "CRITICAL" || finding.severity === "HIGH") &&
      finding.status !== "RESOLVED"
  );
}

export function getUndecidedNonBlockingFindings(state) {
  ensureFindingsSection(state);

  return state.codeFindings.findings.filter(
    (finding) => NON_BLOCKING_FINDING_SEVERITIES.has(finding.severity) &&
      finding.status !== "RESOLVED" &&
      !DECISION_STATUSES.has(finding.status)
  );
}

export function buildCodeReviewGate(state) {
  ensureFindingsSection(state);

  const findings = state.codeFindings.findings;
  const round = state.codeFindings.round;
  const unresolvedBlocking = getUnresolvedBlockingFindings(state);
  const debt = summarizeFindingDebt(findings);

  if (unresolvedBlocking.length === 0) {
    return {
      gate: "PASS",
      round,
      unresolvedBlocking: 0,
      ...debt,
      totalFindings: findings.length,
      message: buildFindingPassMessage(debt)
    };
  }

  if (round >= MAX_CODE_REVIEW_ROUNDS) {
    return {
      gate: "ESCALATE",
      round,
      unresolvedBlocking: unresolvedBlocking.length,
      ...debt,
      totalFindings: findings.length,
      message: `${unresolvedBlocking.length} unresolved Critical/High finding(s) after ${round} round(s). Escalate to user.`
    };
  }

  return {
    gate: "FAIL",
    round,
    unresolvedBlocking: unresolvedBlocking.length,
    ...debt,
    totalFindings: findings.length,
    message: `${unresolvedBlocking.length} unresolved Critical/High finding(s) in round ${round}.`
  };
}

export function summarizeFindings(state) {
  const section = state.codeFindings ?? { round: 0, findings: [] };
  const findings = section.findings;

  const bySeverity = { CRITICAL: 0, HIGH: 0, MEDIUM: 0, LOW: 0 };
  const byStatus = { OPEN: 0, FIXED: 0, DISPUTED: 0, RESOLVED: 0, REOPENED: 0, ACCEPTED: 0, DEFERRED: 0 };

  for (const finding of findings) {
    bySeverity[finding.severity] = (bySeverity[finding.severity] ?? 0) + 1;
    byStatus[finding.status] = (byStatus[finding.status] ?? 0) + 1;
  }

  return {
    round: section.round,
    total: findings.length,
    bySeverity,
    byStatus,
    debt: summarizeFindingDebt(findings),
    findings: findings.map((finding) => ({
      id: finding.id,
      severity: finding.severity,
      status: finding.status,
      description: finding.description,
      file: finding.file,
      responseNote: finding.responseNote,
      decisionReason: finding.decisionReason ?? null,
      decisionBy: finding.decisionBy ?? null,
      decisionAt: finding.decisionAt ?? null,
      followUpRef: finding.followUpRef ?? null
    }))
  };
}

function findFinding(state, findingId) {
  const finding = state.codeFindings.findings.find((entry) => entry.id === findingId);
  if (!finding) {
    throw new Error(`Finding not found: ${findingId}`);
  }
  return finding;
}

function ensureFindingsSection(state) {
  if (!state.codeFindings) {
    state.codeFindings = { round: 0, findings: [] };
  }
}

function summarizeFindingDebt(findings) {
  return {
    accepted: findings.filter((finding) => finding.status === "ACCEPTED").length,
    deferred: findings.filter((finding) => finding.status === "DEFERRED").length,
    undecidedNonBlocking: findings.filter(
      (finding) => NON_BLOCKING_FINDING_SEVERITIES.has(finding.severity) &&
        finding.status !== "RESOLVED" &&
        !DECISION_STATUSES.has(finding.status)
    ).length
  };
}

function buildFindingPassMessage(debt) {
  if (debt.undecidedNonBlocking > 0) {
    return `No unresolved Critical/High findings, but ${debt.undecidedNonBlocking} non-blocking Medium/Low finding(s) still need TL decision (ACCEPTED or DEFERRED).`;
  }

  if (debt.accepted > 0 || debt.deferred > 0) {
    return `No unresolved Critical/High findings. ${debt.accepted} ACCEPTED and ${debt.deferred} DEFERRED non-blocking finding(s) are recorded.`;
  }

  return "No unresolved Critical/High findings. Code review passed.";
}
