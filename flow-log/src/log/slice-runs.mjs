import { timestamp } from "./common.mjs";

export function ensureSliceRunsState(state) {
  if (state.sliceRuns) {
    normalizeExistingSliceRunTypes(state.sliceRuns);
    return state.sliceRuns;
  }

  const legacyBatches = state.batches ?? { current: null, total: 0, history: [] };
  state.sliceRuns = {
    current: legacyBatches.current ? normalizeLegacyBatch(legacyBatches.current) : null,
    total: legacyBatches.total ?? (legacyBatches.history ?? []).length,
    history: (legacyBatches.history ?? []).map((entry) => normalizeLegacyBatch(entry))
  };
  delete state.batches;

  return state.sliceRuns;
}

function normalizeExistingSliceRunTypes(sliceRuns) {
  if (sliceRuns.current && !sliceRuns.current.type) {
    sliceRuns.current.type = "final";
  }

  for (const entry of sliceRuns.history ?? []) {
    if (!entry.type) {
      entry.type = "final";
    }
  }
}

export function startSliceRun(state, slice, type, by) {
  const sliceRuns = ensureSliceRunsState(state);
  if (sliceRuns.current) {
    throw new Error(`Slice-run ${sliceRuns.current.run} is already in progress.`);
  }

  const now = timestamp();
  const runNumber = sliceRuns.total + 1;

  sliceRuns.current = {
    run: runNumber,
    slice,
    type,
    changedFiles: [],
    startedAt: now,
    completedAt: null,
    status: "in-progress",
    by: by ?? null
  };
  sliceRuns.total = runNumber;
}

export function completeSliceRun(state, status) {
  const sliceRuns = ensureSliceRunsState(state);
  if (!sliceRuns.current) {
    throw new Error("No slice-run is currently in progress.");
  }

  const now = timestamp();
  sliceRuns.current.completedAt = now;
  sliceRuns.current.status = status ?? "complete";
  sliceRuns.history.push(sliceRuns.current);
  sliceRuns.current = null;
}

export function completeFlow(state) {
  const now = timestamp();
  state.timing.completedAt = now;

  const start = new Date(state.timing.startedAt).getTime();
  const end = new Date(now).getTime();
  state.timing.durationMinutes = Math.round((end - start) / 60000);
}

export function summarizeSliceRuns(state) {
  const sliceRuns = ensureSliceRunsState(state);

  return {
    current: sliceRuns.current
      ? {
        run: sliceRuns.current.run,
        slice: sliceRuns.current.slice,
        type: sliceRuns.current.type,
        changedFileCount: (sliceRuns.current.changedFiles ?? []).length,
        changedFiles: sliceRuns.current.changedFiles ?? []
      }
      : null,
    completed: sliceRuns.history.length,
    total: sliceRuns.total
  };
}

function normalizeLegacyBatch(entry) {
  return {
    run: entry.run ?? entry.batch ?? null,
    slice: entry.slice ?? normalizeLegacySlices(entry.slices),
    type: entry.type ?? "final",
    changedFiles: entry.changedFiles ?? [],
    startedAt: entry.startedAt ?? null,
    completedAt: entry.completedAt ?? null,
    status: entry.status ?? "complete",
    by: entry.by ?? null
  };
}

function normalizeLegacySlices(slices) {
  if (!Array.isArray(slices) || slices.length === 0) {
    return null;
  }

  return slices.length === 1 ? slices[0] : slices.join(", ");
}
