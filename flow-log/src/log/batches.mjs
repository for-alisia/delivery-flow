import { timestamp } from "./common.mjs";

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

export function completeFlow(state) {
  const now = timestamp();
  state.timing.completedAt = now;

  const start = new Date(state.timing.startedAt).getTime();
  const end = new Date(now).getTime();
  state.timing.durationMinutes = Math.round((end - start) / 60000);
}

export function summarizeBatches(state) {
  if (!state.batches) {
    return { current: null, completed: 0, total: 0 };
  }

  return {
    current: state.batches.current?.batch ?? null,
    completed: state.batches.history.length,
    total: state.batches.total
  };
}
