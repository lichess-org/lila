import throttle from 'common/throttle';
import type RoundController from './ctrl';
import type { RoundData } from './interfaces';

export function reload(ctrl: RoundController): Promise<RoundData> {
  const url = ctrl.data.player.spectator
    ? `/${ctrl.data.game.id}/${ctrl.data.player.color}`
    : `/${ctrl.data.game.id}${ctrl.data.player.id}`;
  return window.lishogi.xhr.json('GET', url).catch(window.lishogi.reload);
}

export function whatsNext(ctrl: RoundController): Promise<{ next: string }> {
  return window.lishogi.xhr.json('GET', `/whats-next/${ctrl.data.game.id}${ctrl.data.player.id}`);
}

export function challengeRematch(gameId: string): Promise<void> {
  return window.lishogi.xhr.json('POST', `/challenge/rematch-of/${gameId}`);
}

export const setZen: (zen: boolean) => void = throttle(1000, zen =>
  window.lishogi.xhr.text('POST', '/pref/zen', {
    formData: { zen },
  }),
);
