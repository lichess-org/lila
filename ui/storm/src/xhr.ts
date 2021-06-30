import * as xhr from 'common/xhr';
import throttle from 'common/throttle';
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

export const setZen = throttle(1000, zen =>
  xhr.text('/pref/zen', {
    method: 'post',
    body: xhr.form({ zen: zen ? 1 : 0 }),
  })
);
