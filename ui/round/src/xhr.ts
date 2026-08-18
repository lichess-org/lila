import { json } from 'lib/xhr';

import type RoundController from './ctrl';
import type { RoundData } from './interfaces';

export const reload = (d: RoundData): Promise<RoundData> => {
  const url = d.player.spectator ? `/${d.game.id}/${d.player.color}` : `/${d.game.id}${d.player.id}`;
  return json(url);
};

export const whatsNext = (ctrl: RoundController): Promise<{ next?: string }> =>
  json(`/whats-next/${ctrl.data.game.id}${ctrl.data.player.id}`);

export const challengeRematch = (gameId: string): Promise<unknown> =>
  json('/challenge/rematch-of/' + gameId, {
    method: 'post',
  });
