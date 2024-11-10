import { text as xhrText, json as xhrJson, form as xhrForm } from 'common/xhr';
import { throttlePromiseDelay } from 'common/timing';
import type { RunResponse, StormRecap } from './interfaces';

export function record(run: StormRecap): Promise<RunResponse> {
  return xhrJson('/storm', {
    method: 'POST',
    body: xhrForm({
      ...run,
      time: Math.round(run.time),
      notAnExploit:
        "Yes, we know that you can send whatever score you like. That's why there's no leaderboards and no competition.",
    }),
  });
}

export const setZen: (zen: any) => Promise<void> = throttlePromiseDelay(
  () => 1000,
  zen =>
    xhrText('/pref/zen', {
      method: 'post',
      body: xhrForm({ zen: zen ? 1 : 0 }),
    }),
);
