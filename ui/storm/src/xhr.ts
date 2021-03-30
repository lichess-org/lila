import * as xhr from 'common/xhr';
import { RunResponse, StormRecap } from './interfaces';

export function record(run: StormRecap, notAnExploit: string): Promise<RunResponse> {
  return xhr.json('/storm', {
    method: 'POST',
    body: xhr.form({
      ...run,
      time: Math.round(run.time),
      notAnExploit,
    }),
  });
}
