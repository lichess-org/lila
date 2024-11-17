import type RoundController from './ctrl';
import { throttlePromiseDelay } from 'common/timing';
import { text, json, form } from 'common/xhr';
import type { RoundData } from './interfaces';

export const reload = (ctrl: RoundController): Promise<RoundData> => {
  const url = ctrl.data.player.spectator
    ? `/${ctrl.data.game.id}/${ctrl.data.player.color}`
    : `/${ctrl.data.game.id}${ctrl.data.player.id}`;
  return json(url);
};

export const setPreference = (key: string, value: string): Promise<string> =>
  text(`/pref/${key}`, { method: 'post', body: form({ [key]: value }) });

export const whatsNext = (ctrl: RoundController): Promise<{ next?: string }> =>
  json(`/whats-next/${ctrl.data.game.id}${ctrl.data.player.id}`);

export const challengeRematch = (gameId: string): Promise<unknown> =>
  json('/challenge/rematch-of/' + gameId, {
    method: 'post',
  });

export const setZen: (zen: boolean) => Promise<void> = throttlePromiseDelay(
  () => 1000,
  zen =>
    text('/pref/zen', {
      method: 'post',
      body: form({ zen: zen ? 1 : 0 }),
    }),
);
