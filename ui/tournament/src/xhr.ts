import { throttle } from 'common';
import TournamentController from './ctrl';

const headers = {
  'Accept': 'application/vnd.lichess.v2+json'
};

// when the tournament no longer exists
function reloadPage() {
  window.lichess.reload();
}

function join(ctrl: TournamentController, password?: string) {
  return $.ajax({
    method: 'POST',
    url: '/tournament/' + ctrl.data.id + '/join',
    data: JSON.stringify({ p: password || null }),
    contentType: 'application/json; charset=utf-8',
    headers
  }).fail(reloadPage);
}

function withdraw(ctrl: TournamentController) {
  return $.ajax({
    method: 'POST',
    url: '/tournament/' + ctrl.data.id + '/withdraw',
    headers
  }).fail(reloadPage);
}

function loadPage(ctrl: TournamentController, p: number) {
  $.ajax({
    url: '/tournament/' + ctrl.data.id + '/standing/' + p,
    headers
  }).then(data => {
    ctrl.loadPage(data);
    ctrl.redraw();
  }, reloadPage);
}

function reloadTournament(ctrl: TournamentController) {
  return $.ajax({
    url: '/tournament/' + ctrl.data.id,
    data: {
      page: ctrl.focusOnMe ? null : ctrl.page,
      playerInfo: ctrl.playerInfo.id
    },
    headers
  }).then(data => {
    ctrl.reload(data);
    ctrl.redraw();
  }, reloadPage);
}

function playerInfo(ctrl: TournamentController, userId: string) {
  return $.ajax({
    url: ['/tournament', ctrl.data.id, 'player', userId].join('/'),
    headers
  }).then(data => {
    ctrl.setPlayerInfoData(data);
    ctrl.redraw();
  }, reloadPage);
}

export default {
  join: throttle(1000, false, join),
  withdraw: throttle(1000, false, withdraw),
  loadPage: throttle(1000, false, loadPage),
  reloadTournament: throttle(2000, false, reloadTournament),
  playerInfo
};
