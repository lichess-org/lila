import throttle from 'common/throttle';
import SwissController from './ctrl';

const headers = {
  'Accept': 'application/vnd.lichess.v5+json'
};

// when the tournament no longer exists
function onFail(_1, _2, errorMessage) {
  if (errorMessage === 'Forbidden') location.href = '/';
  else window.lichess.reload();
}

function join(ctrl: SwissController) {
  return $.ajax({
    method: 'POST',
    url: '/swiss/' + ctrl.data.id + '/join',
    contentType: 'application/json; charset=utf-8',
    headers
  }).fail(onFail);
}

function withdraw(ctrl: SwissController) {
  return $.ajax({
    method: 'POST',
    url: '/swiss/' + ctrl.data.id + '/withdraw',
    headers
  }).fail(onFail);
}

function loadPage(ctrl: SwissController, p: number) {
  $.ajax({
    url: '/swiss/' + ctrl.data.id + '/standing/' + p,
    headers
  }).then(data => {
    ctrl.loadPage(data);
    ctrl.redraw();
  }, onFail);
}

function loadPageOf(ctrl: SwissController, userId: string): JQueryXHR {
  return $.ajax({
    url: '/swiss/' + ctrl.data.id + '/page-of/' + userId,
    headers
  });
}

function reload(ctrl: SwissController) {
  return $.ajax({
    url: '/swiss/' + ctrl.data.id,
    data: {
      page: ctrl.focusOnMe ? null : ctrl.page,
      playerInfo: ctrl.playerInfo.id,
      partial: true
    },
    headers
  }).then(data => {
    ctrl.reload(data);
    ctrl.redraw();
  }, onFail);
}

function playerInfo(ctrl: SwissController, userId: string) {
  return $.ajax({
    url: ['/swiss', ctrl.data.id, 'player', userId].join('/'),
    headers
  }).then(data => {
    ctrl.setPlayerInfoData(data);
    ctrl.redraw();
  }, onFail);
}

export default {
  join: throttle(1000, join),
  withdraw: throttle(1000, withdraw),
  loadPage: throttle(1000, loadPage),
  loadPageOf,
  reloadSoon: throttle(4000, reload),
  reloadNow: reload,
  playerInfo
};
