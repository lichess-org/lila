import RoundController from './ctrl';
import { throttlePromiseDelay } from 'common/throttle';
import * as xhr from 'common/xhr';
import { RoundData } from './interfaces';

export const reload = (ctrl: RoundController): Promise<RoundData> => xhr.json(ctrl.data.url.round);

export const setPreference = (key: string, value: string): Promise<string> =>
  xhr.text(`/pref/${key}`, { method: 'post', body: xhr.form({ [key]: value }) });

export const whatsNext = (ctrl: RoundController): Promise<{ next?: string }> =>
  xhr.json(`/whats-next/${ctrl.data.game.id}${ctrl.data.player.id}`);

export const challengeRematch = (gameId: string): Promise<unknown> =>
  xhr.json('/challenge/rematch-of/' + gameId, {
    method: 'post',
  });

export const setZen = throttlePromiseDelay(
  () => 1000,
  zen =>
    xhr.text('/pref/zen', {
      method: 'post',
      body: xhr.form({ zen: zen ? 1 : 0 }),
    })
);
