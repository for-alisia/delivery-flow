import { timestamp } from "./common.mjs";

export function setE2EMode(state, mode, by, reason) {
  state.e2e = {
    mode,
    decidedAt: timestamp(),
    decidedBy: by ?? null,
    reason: reason ?? null
  };

  return state.e2e;
}