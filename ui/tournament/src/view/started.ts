import { MaybeVNodes } from 'common/snabbdom';
import { VNode, h } from 'snabbdom';
import TournamentController from '../ctrl';
import * as pagination from '../pagination';
import * as tour from '../tournament';
import { controls, standing } from './arena';
import { teamStanding } from './battle';
import header from './header';
import playerInfo from './playerInfo';
import tourTable from './table';
import teamInfo from './teamInfo';

function joinTheGame(ctrl: TournamentController, gameId: string) {
  return h(
    'a.tour__ur-playing.button.is.is-after',
    {
      attrs: { href: '/' + gameId },
    },
    [ctrl.trans.noarg('youArePlaying'), h('br'), ctrl.trans.noarg('joinTheGame')]
  );
}

function notice(ctrl: TournamentController): VNode {
  return tour.willBePaired(ctrl)
    ? h('div.tour__notice.bar-glider', ctrl.trans('standByX', ctrl.data.me.username))
    : h('div.tour__notice.closed', ctrl.trans('tournamentPairingsAreNowClosed'));
}

export const name = 'started';

export function main(ctrl: TournamentController): MaybeVNodes {
  const gameId = ctrl.myGameId(),
    pag = pagination.players(ctrl);
  return [
    header(ctrl),
    gameId ? joinTheGame(ctrl, gameId) : tour.isIn(ctrl) ? notice(ctrl) : null,
    teamStanding(ctrl, 'started'),
    controls(ctrl, pag),
    standing(ctrl, pag, 'started'),
  ];
}

export function table(ctrl: TournamentController): VNode | undefined {
  return ctrl.playerInfo.id ? playerInfo(ctrl) : ctrl.teamInfo.requested ? teamInfo(ctrl) : tourTable(ctrl);
}
