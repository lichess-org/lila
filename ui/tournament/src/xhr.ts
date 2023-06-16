import throttle from 'common/throttle';
import TournamentController from './ctrl';

const headers = {
  Accept: 'application/vnd.lishogi.v3+json',
};

// when the tournament no longer exists
function onFail(_1, _2, errorMessage) {
  if (errorMessage === 'Forbidden') location.href = '/';
  else setTimeout(window.lishogi.reload, Math.floor(Math.random() * 9000));
}

function join(ctrl: TournamentController, password?: string, team?: string) {
  return $.ajax({
    method: 'POST',
    url: '/tournament/' + ctrl.data.id + '/join',
    data: JSON.stringify({
      p: password || null,
      team: team || null,
    }),
    contentType: 'application/json; charset=utf-8',
    headers,
  }).fail(onFail);
}

function withdraw(ctrl: TournamentController) {
  return $.ajax({
    method: 'POST',
    url: '/tournament/' + ctrl.data.id + '/withdraw',
    headers,
  }).fail(onFail);
}

function loadPage(ctrl: TournamentController, p: number) {
  $.ajax({
    url: '/tournament/' + ctrl.data.id + '/standing/' + p,
    headers,
  }).then(data => {
    ctrl.loadPage(data);
    ctrl.redraw();
  }, onFail);
}

function loadPageOf(ctrl: TournamentController, userId: string): JQueryXHR {
  return $.ajax({
    url: '/tournament/' + ctrl.data.id + '/page-of/' + userId,
    headers,
  });
}

function reload(ctrl: TournamentController) {
  return $.ajax({
    url: '/tournament/' + ctrl.data.id,
    data: {
      page: ctrl.focusOnMe ? null : ctrl.page,
      playerInfo: ctrl.playerInfo.id,
      partial: true,
    },
    headers,
  }).then(data => {
    ctrl.reload(data);
    ctrl.redraw();
    const extraDelay = Math.floor(ctrl.nbWatchers) * (data.me ? 1 : 3);
    return new Promise(resolve => setTimeout(resolve, extraDelay));
  }, onFail);
}

function playerInfo(ctrl: TournamentController, userId: string) {
  return $.ajax({
    url: ['/tournament', ctrl.data.id, 'player', userId].join('/'),
    headers,
  }).then(data => {
    ctrl.setPlayerInfoData(data);
    ctrl.redraw();
  }, onFail);
}

function teamInfo(ctrl: TournamentController, teamId: string) {
  return $.ajax({
    url: ['/tournament', ctrl.data.id, 'team', teamId].join('/'),
    headers,
  }).then(data => {
    ctrl.setTeamInfo(data);
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
  playerInfo,
  teamInfo,
};
