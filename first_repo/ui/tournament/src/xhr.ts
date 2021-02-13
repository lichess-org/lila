import throttle from 'common/throttle';
import * as xhr from 'common/xhr';
import TournamentController from './ctrl';

// when the tournament no longer exists
const onFail = () => lichess.reload();

export const join = throttle(1000, (ctrl: TournamentController, password?: string, team?: string) =>
  xhr
    .textRaw('/tournament/' + ctrl.data.id + '/join', {
      method: 'POST',
      body: JSON.stringify({
        p: password || null,
        team: team || null,
      }),
      headers: { 'Content-Type': 'application/json' },
    })
    .then(res => {
      if (!res.ok) res.text().then(alert);
    })
);

export const withdraw = throttle(1000, (ctrl: TournamentController) =>
  xhr
    .text('/tournament/' + ctrl.data.id + '/withdraw', {
      method: 'POST',
    })
    .catch(onFail)
);

export const loadPage = throttle(1000, (ctrl: TournamentController, p: number) =>
  xhr.json(`/tournament/${ctrl.data.id}/standing/${p}`).then(data => {
    ctrl.loadPage(data);
    ctrl.redraw();
  }, onFail)
);

export const loadPageOf = (ctrl: TournamentController, userId: string) =>
  xhr.json(`/tournament/${ctrl.data.id}/page-of/${userId}`);

export const reloadNow = (ctrl: TournamentController) =>
  xhr
    .json(
      xhr.url('/tournament/' + ctrl.data.id, {
        page: ctrl.focusOnMe ? undefined : ctrl.page,
        playerInfo: ctrl.playerInfo.id,
        partial: true,
      })
    )
    .then(data => {
      ctrl.reload(data);
      ctrl.redraw();
    }, onFail);

export const reloadSoon = throttle(4000, reloadNow);

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
