import fs from "node:fs";
import { collectAllKnownIds } from "./refs.mjs";
import { ensureFeatureMatches, loadState, resolveStatePath } from "../log/store.mjs";

const ENFORCED_RISK_STATUSES = new Set(["OPEN", "REOPENED", "ADDRESSED"]);

export function validateRiskLinks(plan, cwd, feature, explicitStatePath) {
  const issues = [];
  const statePath = resolveStatePath(cwd, feature, explicitStatePath);

  if (!fs.existsSync(statePath)) {
    return {
      valid: true,
      issues,
      checked: false,
      statePath,
      checkedRiskCount: 0
    };
  }

  const state = loadState(statePath);
  ensureFeatureMatches(state, feature, statePath);

  const knownIds = collectAllKnownIds(plan);
  const enforcedRisks = (state.architecturalRisks?.risks ?? []).filter((risk) => ENFORCED_RISK_STATUSES.has(risk.status));

  for (const risk of enforcedRisks) {
    const planRefs = Array.isArray(risk.planRefs) ? risk.planRefs : [];
    const connectedAreas = Array.isArray(risk.connectedAreas) ? risk.connectedAreas : [];

    const missingRefs = planRefs.filter((refId) => !knownIds.has(refId));
    if (missingRefs.length > 0) {
      issues.push(
        `Risk ${risk.id} (${risk.status}) references missing plan IDs: ${missingRefs.join(", ")}.`
      );
    }

    const missingConnectedAreas = connectedAreas.filter((refId) => !knownIds.has(refId));
    if (missingConnectedAreas.length > 0) {
      issues.push(
        `Risk ${risk.id} (${risk.status}) references missing connected plan IDs: ${missingConnectedAreas.join(", ")}.`
      );
    }
  }

  return {
    valid: issues.length === 0,
    issues,
    checked: true,
    statePath,
    checkedRiskCount: enforcedRisks.length
  };
}
