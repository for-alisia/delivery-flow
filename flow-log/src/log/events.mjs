import { nextEventId, timestamp } from "./common.mjs";
import { EVENT_TYPES } from "./schema.mjs";

export function appendEvent(state, event) {
  const entry = {
    id: nextEventId(state.events),
    type: event.type,
    by: event.by ?? null,
    reason: event.reason,
    target: event.target ?? null,
    relatedCheck: event.relatedCheck ?? null,
    relatedReview: event.relatedReview ?? null,
    createdAt: timestamp()
  };

  if (event.decision) {
    entry.decision = event.decision;
  }
  if (event.batch !== undefined) {
    entry.batch = event.batch;
  }
  if (event.slices) {
    entry.slices = event.slices;
  }
  if (event.batchStatus) {
    entry.batchStatus = event.batchStatus;
  }

  state.events.push(entry);
}

export function summarizeEventCounts(events) {
  const counts = Object.fromEntries(EVENT_TYPES.map((type) => [type, 0]));

  for (const event of events) {
    if (!(event.type in counts)) {
      counts[event.type] = 0;
    }

    counts[event.type] += 1;
  }

  return {
    total: events.length,
    counts
  };
}
