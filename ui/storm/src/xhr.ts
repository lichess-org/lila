import { RunResponse, StormRecap } from './interfaces';

export function record(run: StormRecap, notAnExploit: string): Promise<RunResponse> {
  return window.lishogi.xhr.json('POST', '/storm', {
    formData: {
      ...run,
      time: Math.round(run.time),
      notAnExploit,
    },
  });
}
