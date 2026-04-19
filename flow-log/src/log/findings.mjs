import { nextFindingId, timestamp, validateValue } from "./common.mjs";
import { FINDING_SEVERITIES, MAX_CODE_REVIEW_ROUNDS } from "./schema.mjs";

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

  void by;

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

export function buildCodeReviewGate(state) {
  ensureFindingsSection(state);

  const findings = state.codeFindings.findings;
  const round = state.codeFindings.round;
  const unresolvedBlocking = getUnresolvedBlockingFindings(state);

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

export function summarizeFindings(state) {
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
    findings: findings.map((finding) => ({
      id: finding.id,
      severity: finding.severity,
      status: finding.status,
      description: finding.description,
      file: finding.file,
      responseNote: finding.responseNote
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
