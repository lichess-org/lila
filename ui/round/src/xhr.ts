import type RoundController from './ctrl';
import { text, json, form } from 'lib/xhr';
import type { RoundData } from './interfaces';

export const reload = (d: RoundData): Promise<RoundData> => {
  const url = d.player.spectator ? `/${d.game.id}/${d.player.color}` : `/${d.game.id}${d.player.id}`;
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
