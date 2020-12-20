import * as xhr from 'common/xhr';
import { Pool } from './interfaces';

export const seeks = () => xhr.json('/lobby/seeks');

export const nowPlaying = () => xhr.json('/account/now-playing').then(o => o.nowPlaying);

export const anonPoolSeek = (pool: Pool) =>
  xhr.json('/setup/hook/' + window.lichess.sri, {
    method: 'POST',
    body: xhr.form({
      variant: 1,
      timeMode: 1,
      time: pool.lim,
      increment: pool.inc,
      days: 1,
      color: 'random'
    })
  });
