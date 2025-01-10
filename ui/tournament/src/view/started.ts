import type { MaybeVNodes } from 'common/snabbdom';
import { i18n, i18nFormat } from 'i18n';
import { type VNode, h } from 'snabbdom';
import type TournamentController from '../ctrl';
import * as pagination from '../pagination';
import * as tour from '../tournament';
import { standing } from './arena';
import { teamStanding } from './battle';
import { arenaControls, organizedControls, robinControls } from './controls';
import header from './header';
import { standing as oStanding } from './organized';
import playerInfo from './player-info';
import {
  howDoesThisWork,
  playing,
  standing as rStanding,
  recents,
  upcoming,
  yourCurrent,
  yourUpcoming,
} from './robin';
import tourTable from './table';
import teamInfo from './team-info';

function joinTheGame(gameId: string) {
  return h(
    'a.tour__ur-playing.button.is.is-after',
    {
      attrs: { href: `/${gameId}` },
    },
    [i18n('youArePlaying'), h('br'), i18n('joinTheGame')],
  );
}

function notice(ctrl: TournamentController): VNode {
  return tour.willBePaired(ctrl)
    ? h('div.tour__notice', i18nFormat('standByX', ctrl.data.me.username))
    : h('div.tour__notice.closed', i18n('tournamentPairingsAreNowClosed'));
}

export const name = 'started';

export function main(ctrl: TournamentController): MaybeVNodes {
  const gameId = ctrl.myGameId(),
    pag = pagination.players(ctrl);
  console.log('pag', pag);
  if (ctrl.isArena())
    return [
      header(ctrl),
      gameId ? joinTheGame(gameId) : tour.isIn(ctrl) ? notice(ctrl) : null,
      teamStanding(ctrl, 'started'),
      arenaControls(ctrl, pag),
      standing(ctrl, pag, 'started'),
    ];
  else if (ctrl.isRobin())
    return [
      header(ctrl),
      robinControls(ctrl),
      rStanding(ctrl, 'started'),
      yourCurrent(ctrl),
      yourUpcoming(ctrl),
      playing(ctrl),
      recents(ctrl),
      howDoesThisWork(),
    ];
  else
    return [
      header(ctrl),
      organizedControls(ctrl, pag),
      oStanding(ctrl, pag, 'started'),
      yourCurrent(ctrl),
      yourUpcoming(ctrl),
      upcoming(ctrl),
      playing(ctrl),
      recents(ctrl),
      howDoesThisWork(),
    ];
}

export function table(ctrl: TournamentController): VNode | undefined {
  return ctrl.playerInfo.id
    ? playerInfo(ctrl)
    : ctrl.teamInfo.requested
      ? teamInfo(ctrl)
      : tourTable(ctrl);
}
