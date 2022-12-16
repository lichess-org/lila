import throttle from 'common/throttle';
import RoundController from './ctrl';

export const headers = {
  Accept: 'application/vnd.lishogi.v4+json',
};

export function reload(ctrl: RoundController) {
  return $.ajax({
    url: ctrl.data.url.round,
    headers,
  }).fail(window.lishogi.reload);
}

export function whatsNext(ctrl: RoundController) {
  return $.ajax({
    url: '/whats-next/' + ctrl.data.game.id + ctrl.data.player.id,
    headers,
  });
}

export function challengeRematch(gameId: string) {
  return $.ajax({
    method: 'POST',
    url: '/challenge/rematch-of/' + gameId,
    headers,
  });
}

export const setZen = throttle(1000, zen => $.post('/pref/zen', { zen: zen ? 1 : 0 }));
