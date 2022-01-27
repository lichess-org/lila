import { throttlePromise, finallyDelay } from 'common/throttle';
import * as xhr from 'common/xhr';
import TournamentController from './ctrl';

// when the tournament no longer exists
// randomly delay reloads in case of massive tournament to avoid ddos
const onFail = (): void => {
  setTimeout(lichess.reload, Math.floor(Math.random() * 9000));
};

export const join = throttlePromise(
  finallyDelay(
    () => 5000,
    (ctrl: TournamentController, password?: string, team?: string) =>
      xhr
        .textRaw('/tournament/' + ctrl.data.id + '/join', {
          method: 'POST',
          // must use JSON body for app compat
          body: JSON.stringify({
            p: password || null,
            team: team || null,
          }),
          headers: { 'Content-Type': 'application/json' },
        })
        .then(res => {
          if (!res.ok)
            res.json().then(t => {
              if (t.error) alert(t.error);
              else lichess.reload();
            });
        }, onFail)
  )
);

export const withdraw = throttlePromise(
  finallyDelay(
    () => 1000,
    (ctrl: TournamentController) =>
      xhr
        .text('/tournament/' + ctrl.data.id + '/withdraw', {
          method: 'POST',
        })
        .then(_ => {}, onFail)
  )
);

export const loadPage = throttlePromise(
  finallyDelay(
    () => 1000,
    (ctrl: TournamentController, p: number, callback?: () => void) =>
      xhr.json(`/tournament/${ctrl.data.id}/standing/${p}`).then(data => {
        ctrl.loadPage(data);
        callback?.();
        ctrl.redraw();
      }, onFail)
  )
);

export const loadPageOf = (ctrl: TournamentController, userId: string) =>
  xhr.json(`/tournament/${ctrl.data.id}/page-of/${userId}`);

// don't use xhr.json to avoid getting the X-Requested-With header
// that causes a CORS preflight check
// TODO FIXME
export const reloadNow = (ctrl: TournamentController): Promise<void> =>
  fetch(
    xhr.url(ctrl.data.reloadEndpoint, {
      page: ctrl.focusOnMe ? undefined : ctrl.page,
      playerInfo: ctrl.playerInfo.id,
      partial: true,
      me: ctrl.data.myUsername,
    }),
    {
      ...xhr.defaultInit,
      headers: xhr.jsonHeader,
    }
  )
    .then(res => xhr.ensureOk(res).json())
    .then(data => {
      ctrl.reload(data);
      ctrl.redraw();
      const extraDelay = Math.floor(ctrl.nbWatchers / 2) * (data.me ? 1 : 3);
      return new Promise(resolve => setTimeout(resolve, extraDelay));
    }, onFail);

export const reloadSoon = throttlePromise(finallyDelay(() => 4000 + Math.floor(Math.random() * 1000), reloadNow));

export const playerInfo = (ctrl: TournamentController, userId: string) =>
  xhr.json(`/tournament/${ctrl.data.id}/player/${userId}`).then(data => {
    ctrl.setPlayerInfoData(data);
    ctrl.redraw();
  }, onFail);

export const teamInfo = (ctrl: TournamentController, teamId: string) =>
  xhr.json(`/tournament/${ctrl.data.id}/team/${teamId}`).then(data => {
    ctrl.setTeamInfo(data);
    ctrl.redraw();
  }, onFail);
