import * as xhr from 'common/xhr';
import { StormRun } from './interfaces';

export function record(run: StormRun): Promise<void> {
  return xhr.json('/storm', {
    method: 'POST',
    body: xhr.form({
      ...run,
      time: Math.round(run.time)
    })
  });
}
