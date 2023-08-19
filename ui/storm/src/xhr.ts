import * as xhr from 'common/xhr';
import { throttlePromiseDelay } from 'common/throttle';
import { RunResponse, StormRecap } from './interfaces';

export function record(run: StormRecap): Promise<RunResponse> {
  return xhr.json('/storm', {
    method: 'POST',
    body: xhr.form({
      ...run,
      time: Math.round(run.time),
      notAnExploit:
        "Yes, we know that you can send whatever score you like. That's why there's no leaderboards and no competition.",
    }),
  });
}

export const setZen = throttlePromiseDelay(
  () => 1000,
  zen =>
    xhr.text('/pref/zen', {
      method: 'post',
      body: xhr.form({ zen: zen ? 1 : 0 }),
    }),
);
