import {
  appendEvent,
  assertFileExists,
  buildArtifactApprovalState,
  completeBatch,
  resetChecksForRedCard,
  saveState,
  startBatch
} from "../log/index.mjs";
import {
  assertPlan,
  loadPlan
} from "../plan/index.mjs";
import {
  normalizeArray,
  optionalFlag
} from "../cli-helpers.mjs";
import { openState } from "./shared.mjs";

export function handleStartBatch(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  const slices = normalizeArray(optionalFlag(parsed, "slice"));
  const by = optionalFlag(parsed, "by");
  validateRequestedSlices(state, cwd, slices);
  startBatch(state, slices, by);
  appendEvent(state, {
    type: "batchStart",
    by: by ?? "TL",
    reason: `Started batch ${state.batches.current.batch} for slices: ${slices.join(", ")}`,
    batch: state.batches.current.batch,
    slices
  });
  saveState(statePath, state);

  return {
    ok: true,
    command: "start-batch",
    feature,
    statePath,
    batch: state.batches.current
  };
}

export function handleCompleteBatch(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  const status = optionalFlag(parsed, "status") ?? "complete";
  const currentBatch = state.batches.current;
  completeBatch(state, status);
  appendEvent(state, {
    type: "batchEnd",
    by: currentBatch?.by ?? null,
    reason: `Completed batch ${currentBatch?.batch ?? "unknown"} with status '${status}'.`,
    batch: currentBatch?.batch ?? null,
    slices: currentBatch?.slices ?? [],
    batchStatus: status
  });
  saveState(statePath, state);

  const last = state.batches.history[state.batches.history.length - 1];

  return {
    ok: true,
    command: "complete-batch",
    feature,
    statePath,
    completedBatch: last,
    totalBatches: state.batches.total
  };
}

export function handleResetChecks(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  const reason = optionalFlag(parsed, "reason") ?? "Red card — checks reset for coder retry";
  resetChecksForRedCard(state);
  appendEvent(state, {
    type: "redCard",
    by: optionalFlag(parsed, "by") ?? "TL",
    reason,
    target: optionalFlag(parsed, "target") ?? "JavaCoder"
  });
  saveState(statePath, state);

  return {
    ok: true,
    command: "reset-checks",
    feature,
    statePath,
    checks: {
      verifyQuick: state.checks.verifyQuick.status,
      finalCheck: state.checks.finalCheck.status,
      karate: state.checks.karate.status
    },
    eventRecorded: state.events[state.events.length - 1]
  };
}

function validateRequestedSlices(state, cwd, slices) {
  if (slices.length === 0) {
    throw new Error("start-batch requires at least one --slice <approved-slice-id>.");
  }

  const duplicateSlices = slices.filter((sliceId, index) => slices.indexOf(sliceId) !== index);
  if (duplicateSlices.length > 0) {
    throw new Error(`Duplicate slice ids are not allowed in one batch: ${[...new Set(duplicateSlices)].join(", ")}`);
  }

  const planApproval = buildArtifactApprovalState(cwd, "plan", state.artifacts?.plan);
  if (!planApproval.path) {
    throw new Error("Plan artifact path is not registered. Register and approve the plan before starting a batch.");
  }
  if (!planApproval.exists) {
    throw new Error("Registered plan artifact does not exist on disk.");
  }
  if (!planApproval.approved) {
    throw new Error("Plan must be approved before starting a batch.");
  }
  if (planApproval.stale) {
    throw new Error("Plan approval is stale. Re-approve the plan before starting a batch.");
  }

  const planPath = assertFileExists(cwd, planApproval.path, "plan artifact");
  const plan = loadPlan(planPath);
  assertPlan(plan);

  const availableSliceIds = new Set((plan.slices ?? []).map((slice) => slice.id));
  const unknownSlices = slices.filter((sliceId) => !availableSliceIds.has(sliceId));
  if (unknownSlices.length > 0) {
    throw new Error(
      `Unknown approved slice ids: ${unknownSlices.join(", ")}. Available slices: ${[...availableSliceIds].join(", ")}`
    );
  }
}
