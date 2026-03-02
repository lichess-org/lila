import { json as xhrJson, form as xhrForm } from 'lib/xhr';
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
