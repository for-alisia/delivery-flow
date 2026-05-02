import {
  appendEvent,
  assertFileExists,
  buildArtifactApprovalState,
  completeSliceRun,
  ensureSliceRunsState,
  resetChecksForRedCard,
  SLICE_RUN_TYPES,
  saveState,
  startSliceRun,
  validateValue
} from "../log/index.mjs";
import {
  assertPlan,
  loadPlan
} from "../plan/index.mjs";
import { optionalFlag, requiredFlag } from "../cli-helpers.mjs";
import { openState } from "./shared.mjs";

export function handleStartSliceRun(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  const slice = requiredFlag(parsed, "slice");
  const type = requiredFlag(parsed, "type");
  const by = optionalFlag(parsed, "by");
  validateValue(type, SLICE_RUN_TYPES, "slice-run type");
  validateRequestedSlice(state, cwd, slice);
  startSliceRun(state, slice, type, by);
  appendEvent(state, {
    type: "sliceRunStart",
    by: by ?? "TL",
    reason: `Started ${type} slice-run ${state.sliceRuns.current.run} for slice: ${slice}`,
    sliceRun: state.sliceRuns.current.run,
    slice,
    sliceRunType: type
  });
  saveState(statePath, state);

  return {
    ok: true,
    command: "start-slice-run",
    feature,
    statePath,
    sliceRun: state.sliceRuns.current
  };
}

export function handleCompleteSliceRun(parsed, cwd) {
  const { feature, state, statePath } = openState(parsed, cwd);
  const status = optionalFlag(parsed, "status") ?? "complete";
  const currentSliceRun = ensureSliceRunsState(state).current;
  completeSliceRun(state, status);
  appendEvent(state, {
    type: "sliceRunEnd",
    by: currentSliceRun?.by ?? null,
    reason: `Completed slice-run ${currentSliceRun?.run ?? "unknown"} with status '${status}'.`,
    sliceRun: currentSliceRun?.run ?? null,
    slice: currentSliceRun?.slice ?? null,
    sliceRunType: currentSliceRun?.type ?? null,
    sliceRunStatus: status
  });
  saveState(statePath, state);

  const last = state.sliceRuns.history[state.sliceRuns.history.length - 1];

  return {
    ok: true,
    command: "complete-slice-run",
    feature,
    statePath,
    completedSliceRun: last,
    totalSliceRuns: state.sliceRuns.total
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

function validateRequestedSlice(state, cwd, slice) {
  const planApproval = buildArtifactApprovalState(cwd, "plan", state.artifacts?.plan);
  if (!planApproval.path) {
    throw new Error("Plan artifact path is not registered. Register and approve the plan before starting a slice-run.");
  }
  if (!planApproval.exists) {
    throw new Error("Registered plan artifact does not exist on disk.");
  }
  if (!planApproval.approved) {
    throw new Error("Plan must be approved before starting a slice-run.");
  }
  if (planApproval.stale) {
    throw new Error("Plan approval is stale. Re-approve the plan before starting a slice-run.");
  }

  const planPath = assertFileExists(cwd, planApproval.path, "plan artifact");
  const plan = loadPlan(planPath);
  assertPlan(plan);

  const availableSliceIds = new Set((plan.slices ?? []).map((entry) => entry.id));
  if (!availableSliceIds.has(slice)) {
    throw new Error(
      `Unknown approved slice id: ${slice}. Available slices: ${[...availableSliceIds].join(", ")}`
    );
  }
}
