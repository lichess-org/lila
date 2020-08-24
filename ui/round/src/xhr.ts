import RoundController from './ctrl';

import throttle from 'common/throttle';

export const headers = {
  'Accept': 'application/vnd.lichess.v5+json'
};

export const reload = (ctrl: RoundController) =>
  $.ajax({
    url: ctrl.data.url.round,
    headers
  }).fail(window.lichess.reload);

export const whatsNext = (ctrl: RoundController) =>
  $.ajax({
    url: '/whats-next/' + ctrl.data.game.id + ctrl.data.player.id,
    headers
  });

export const challengeRematch = (gameId: string) =>
  $.ajax({
    method: 'POST',
    url: '/challenge/rematch-of/' + gameId,
    headers
  });

export const setZen = throttle(1000, zen => $.post('/pref/zen', { zen: zen ? 1 : 0 }));
