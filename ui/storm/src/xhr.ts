import * as xhr from 'common/xhr';
import { RunResponse, StormRun } from './interfaces';

export function record(run: StormRun, notAnExploit: string): Promise<RunResponse> {
  return xhr.json('/storm', {
    method: 'POST',
    body: xhr.form({
      ...run,
      time: Math.round(run.time),
      notAnExploit
    })
  });
}
