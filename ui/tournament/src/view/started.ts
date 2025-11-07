import { h, type VNode } from 'snabbdom';
import { controls, standing } from './arena';
import { teamStanding } from './battle';
import header from './header';
import tourTable from './table';
import playerInfo from './playerInfo';
import teamInfo from './teamInfo';
import { players } from '../pagination';
import type TournamentController from '../ctrl';
import type { MaybeVNodes } from 'lib/snabbdom';

function joinTheGame(gameId: string) {
  return h('a.tour__ur-playing.button.is.is-after', { attrs: { href: '/' + gameId } }, [
    i18n.site.youArePlaying,
    h('br'),
    i18n.site.joinTheGame,
  ]);
}

function notice(ctrl: TournamentController): VNode {
  return ctrl.willBePaired()
    ? h('div.tour__notice.bar-glider', i18n.site.standByX(ctrl.data.myUsername!))
    : h('div.tour__notice.closed', i18n.arena.tournamentPairingsAreNowClosed);
}

export const name = 'started';

export function main(ctrl: TournamentController): MaybeVNodes {
  const gameId = ctrl.myGameId(),
    pag = players(ctrl);
  return [
    header(ctrl),
    gameId ? joinTheGame(gameId) : ctrl.isIn() ? notice(ctrl) : null,
    teamStanding(ctrl, 'started'),
    controls(ctrl, pag),
    standing(ctrl, pag, 'started'),
  ];
}

export function table(ctrl: TournamentController): VNode | undefined {
  return ctrl.playerInfo.id ? playerInfo(ctrl) : ctrl.teamInfo.requested ? teamInfo(ctrl) : tourTable(ctrl);
}
