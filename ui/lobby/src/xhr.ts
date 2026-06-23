import debounce from 'debounce-promise';

import { json as xhrJson, form } from 'lib/xhr';

import type { NowPlayingUpdate, Pool, Seek } from './interfaces';

export const seeks: () => Promise<Seek[]> = debounce(() => xhrJson('/lobby/seeks'), 3000, { leading: true });

export const nowPlaying = (): Promise<NowPlayingUpdate> => xhrJson('/account/now-playing');

export const anonPoolSeek = (pool: Pool) =>
  xhrJson('/setup/hook/' + site.sri, {
    method: 'POST',
    body: form({
      variant: 1,
      timeMode: 1,
      time: pool.lim,
      increment: pool.inc,
      days: 1,
      color: 'random',
    }),
  });
