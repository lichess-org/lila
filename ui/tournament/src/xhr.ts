import throttle from 'common/throttle';
import TournamentController from './ctrl';

function onFail() {
  setTimeout(window.lishogi.reload, Math.floor(Math.random() * 9000));
}

function join(ctrl: TournamentController, password?: string, team?: string) {
  return window.lishogi.xhr
    .json('POST', `/tournament/${ctrl.data.id}/join`, {
      json: {
        p: password || null,
        team: team || null,
      },
    })
    .then(res => {
      if (!res.ok)
        res.json().then(t => {
          if (t.error) alert(t.error);
          else window.lishogi.reload();
        });
    }, onFail);
}

function withdraw(ctrl: TournamentController): Promise<any> {
  return window.lishogi.xhr.json('POST', `/tournament/${ctrl.data.id}/withdraw`).catch(onFail);
}

function loadPage(ctrl: TournamentController, p: number): Promise<void> {
  return window.lishogi.xhr
    .json('GET', `/tournament/${ctrl.data.id}/standing/${p}`)
    .then(data => {
      ctrl.loadPage(data);
      ctrl.redraw();
    })
    .catch(onFail);
}

function loadPageOf(ctrl: TournamentController, userId: string): Promise<any> {
  return window.lishogi.xhr.json('GET', `/tournament/${ctrl.data.id}/page-of/${userId}`);
}

function reload(ctrl: TournamentController): Promise<void> {
  return window.lishogi.xhr
    .json('GET', `/tournament/${ctrl.data.id}`, {
      url: {
        page: ctrl.focusOnMe ? undefined : ctrl.page,
        playerInfo: ctrl.playerInfo.id,
        partial: true,
      },
    })
    .then(data => {
      ctrl.reload(data);
      ctrl.redraw();
      const extraDelay = Math.floor(ctrl.nbWatchers) * (data.me ? 1 : 3);
      return new Promise(resolve => setTimeout(resolve, extraDelay));
    }, onFail);
}

export const playerInfo = (ctrl: TournamentController, userId: string): any =>
  window.lishogi.xhr.json('GET', `/tournament/${ctrl.data.id}/player/${userId}`).then(data => {
    ctrl.setPlayerInfoData(data);
    ctrl.redraw();
  }, onFail);

export const teamInfo = (ctrl: TournamentController, teamId: string): any =>
  window.lishogi.xhr.json('GET', `/tournament/${ctrl.data.id}/team/${teamId}`).then(data => {
    ctrl.setTeamInfo(data);
    ctrl.redraw();
  }, onFail);

interface Xhr {
  join: (ctrl: TournamentController, password?: string, team?: string) => void;
  withdraw: (ctrl: TournamentController) => void;
  loadPage: (ctrl: TournamentController, p: number) => void;
  loadPageOf: typeof loadPageOf;
  reloadSoon: (...args: any[]) => void;
  reloadNow: typeof reload;
  playerInfo: typeof playerInfo;
  teamInfo: typeof teamInfo;
}

const xhrFunctions: Xhr = {
  join: throttle(1000, join),
  withdraw: throttle(1000, withdraw),
  loadPage: throttle(1000, loadPage),
  loadPageOf: loadPageOf,
  reloadSoon: throttle(4000, reload),
  reloadNow: reload,
  playerInfo: playerInfo,
  teamInfo: teamInfo,
};

export default xhrFunctions;
