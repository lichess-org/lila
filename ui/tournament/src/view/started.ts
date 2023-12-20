import { h, VNode } from 'snabbdom';
import { controls, standing } from './arena';
import { teamStanding } from './battle';
import header from './header';
import tourTable from './table';
import playerInfo from './playerInfo';
import teamInfo from './teamInfo';
import * as pagination from '../pagination';
import TournamentController from '../ctrl';
import { MaybeVNodes } from 'common/snabbdom';

function joinTheGame(ctrl: TournamentController, gameId: string) {
  return h('a.tour__ur-playing.button.is.is-after', { attrs: { href: '/' + gameId } }, [
    ctrl.trans('youArePlaying'),
    h('br'),
    ctrl.trans('joinTheGame'),
  ]);
}

function notice(ctrl: TournamentController): VNode {
  return ctrl.willBePaired()
    ? h('div.tour__notice.bar-glider', ctrl.trans('standByX', ctrl.data.myUsername!))
    : h('div.tour__notice.closed', ctrl.trans('tournamentPairingsAreNowClosed'));
}

export const name = 'started';

export function main(ctrl: TournamentController): MaybeVNodes {
  const gameId = ctrl.myGameId(),
    pag = pagination.players(ctrl);
  return [
    header(ctrl),
    gameId ? joinTheGame(ctrl, gameId) : ctrl.isIn() ? notice(ctrl) : null,
    teamStanding(ctrl, 'started'),
    controls(ctrl, pag),
    standing(ctrl, pag, 'started'),
  ];
}

export function table(ctrl: TournamentController): VNode | undefined {
  return ctrl.playerInfo.id ? playerInfo(ctrl) : ctrl.teamInfo.requested ? teamInfo(ctrl) : tourTable(ctrl);
}
