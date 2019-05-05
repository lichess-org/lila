import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import { controls, standing } from './arena';
import header from './header';
import tourTable from './table';
import playerInfo from './playerInfo';
import * as pagination from '../pagination';
import * as tour from '../tournament';
import TournamentController from '../ctrl';
import { MaybeVNodes } from '../interfaces';

function joinTheGame(ctrl: TournamentController, gameId: string) {
  return h('a.tour__ur-playing.button.is.is-after.glowing', {
    attrs: { href: '/' + gameId }
  }, [
    ctrl.trans('youArePlaying'), h('br'),
    ctrl.trans('joinTheGame')
  ]);
}

function notice(ctrl: TournamentController): VNode {
  return tour.willBePaired(ctrl) ? h('div.tour__notice.bar-glider',
    ctrl.trans('standByX', ctrl.data.me.username)
  ) : h('div.tour__notice.closed', ctrl.trans('tournamentPairingsAreNowClosed'));
}

export const name = 'started';

export function main(ctrl: TournamentController): MaybeVNodes {
  const gameId = ctrl.myGameId(),
    pag = pagination.players(ctrl);
  return [
    header(ctrl),
    gameId ? joinTheGame(ctrl, gameId) : (tour.isIn(ctrl) ? notice(ctrl) : null),
    controls(ctrl, pag),
    standing(ctrl, pag, 'started'),
  ];
}

export function table(ctrl: TournamentController): VNode {
  return ctrl.playerInfo.id ? playerInfo(ctrl) : tourTable(ctrl);
}
