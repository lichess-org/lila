import { finallyDelay, throttlePromiseDelay } from 'lib/async';
import * as xhr from 'lib/xhr';
import type TournamentController from './ctrl';
import { alert } from 'lib/view';

// when the tournament no longer exists
// randomly delay reloads in case of massive tournament to avoid ddos
const onFail = (): void => {
  setTimeout(site.reload, Math.floor(Math.random() * 9000));
};

export const join = throttlePromiseDelay(
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
            else site.reload();
          });
      }, onFail),
);

export const withdraw = throttlePromiseDelay(
  () => 1000,
  (ctrl: TournamentController) =>
    xhr
      .text('/tournament/' + ctrl.data.id + '/withdraw', {
        method: 'POST',
      })
      .then(() => {}, onFail),
);

export const loadPage = throttlePromiseDelay(
  () => 1000,
  (ctrl: TournamentController, p: number, callback?: () => void) =>
    xhr.json(`/tournament/${ctrl.data.id}/standing/${p}`).then(data => {
      ctrl.loadPage(data);
      callback?.();
      ctrl.redraw();
    }, onFail),
);

export const loadPageOf = (ctrl: TournamentController, userId: string) =>
  xhr.json(`/tournament/${ctrl.data.id}/page-of/${userId}`);

// use lila when lila-http is offline
const reloadEndpointFallback = (ctrl: TournamentController) => `/tournament/${ctrl.data.id}`;

// don't use xhr.json to avoid getting the X-Requested-With header
// that causes a CORS preflight check
export const reloadNow: (ctrl: TournamentController) => Promise<void> = finallyDelay(
  (ctrl: TournamentController) => Math.floor(ctrl.nbWatchers / 2) * (ctrl.data.me ? 1 : 3),
  (ctrl: TournamentController) =>
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
      },
    )
      .then(res => xhr.ensureOk(res).json())
      .then(
        data => {
          ctrl.reload(data);
          ctrl.redraw();
        },
        () => {
          if (ctrl.data.reloadEndpoint !== reloadEndpointFallback(ctrl)) {
            ctrl.data.reloadEndpoint = reloadEndpointFallback(ctrl);
            return reloadNow(ctrl);
          } else return onFail();
        },
      ),
);

export const reloadSoon = throttlePromiseDelay(() => 4000 + Math.floor(Math.random() * 1000), reloadNow);

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
