import { nextRiskId, timestamp, validateValue } from "./common.mjs";
import { MAX_ARCHITECTURE_REVIEW_ROUNDS, RISK_SEVERITIES } from "./schema.mjs";

export function addRisk(state, severity, description, by, suggestedFix, planRefs = [], connectedAreas = []) {
  validateValue(severity, RISK_SEVERITIES, "risk severity");
  ensureRisksSection(state);

  const id = nextRiskId(state.architecturalRisks.risks);

  const risk = {
    id,
    severity,
    description,
    suggestedFix: suggestedFix ?? null,
    planRefs: sanitizeStringArray(planRefs),
    connectedAreas: sanitizeStringArray(connectedAreas),
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

  void by;

  return risk;
}

export function incrementReviewRound(state) {
  ensureRisksSection(state);

  if (state.architecturalRisks.round >= MAX_ARCHITECTURE_REVIEW_ROUNDS) {
    throw new Error(
      `Architecture review hard cap reached (${MAX_ARCHITECTURE_REVIEW_ROUNDS} rounds). Cannot increment further. Resolve manually or restart the plan.`
    );
  }

  state.architecturalRisks.round += 1;
  return state.architecturalRisks.round;
}

export function getUnresolvedBlockingRisks(state) {
  ensureRisksSection(state);

  return state.architecturalRisks.risks.filter(
    (risk) => (risk.severity === "CRITICAL" || risk.severity === "HIGH") &&
      risk.status !== "RESOLVED"
  );
}

export function buildArchitectureGate(state) {
  ensureRisksSection(state);

  const risks = state.architecturalRisks.risks;
  const round = state.architecturalRisks.round;
  const unresolvedBlocking = getUnresolvedBlockingRisks(state);

  const allResponded = risks
    .filter((risk) => risk.status === "OPEN" || risk.status === "REOPENED")
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
      unresolvedRisks: unresolvedBlocking.map((risk) => ({
        id: risk.id,
        severity: risk.severity,
        description: risk.description,
        status: risk.status
      })),
      totalRisks: risks.length,
      message: `${unresolvedBlocking.length} unresolved Critical/High risk(s) after ${round} round(s). TL must decide: PROCEED_TO_CODING, FINAL_ADJUSTMENT, or ESCALATE_TO_USER.`
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

export function summarizeRisks(state) {
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
    risks: risks.map((risk) => ({
      id: risk.id,
      severity: risk.severity,
      status: risk.status,
      description: risk.description,
      suggestedFix: risk.suggestedFix ?? null,
      planRefs: risk.planRefs ?? [],
      connectedAreas: risk.connectedAreas ?? [],
      responseNote: risk.responseNote
    }))
  };
}

function findRisk(state, riskId) {
  const risk = state.architecturalRisks.risks.find((entry) => entry.id === riskId);
  if (!risk) {
    throw new Error(`Risk not found: ${riskId}`);
  }
  return risk;
}

function ensureRisksSection(state) {
  if (!state.architecturalRisks) {
    state.architecturalRisks = { round: 0, risks: [] };
  }
}

function sanitizeStringArray(values) {
  if (!Array.isArray(values)) {
    return [];
  }

  return values
    .map((value) => (typeof value === "string" ? value.trim() : ""))
    .filter((value) => value.length > 0);
}
