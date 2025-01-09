import { MaybeVNodes } from 'common/snabbdom';
import { VNode, h } from 'snabbdom';
import TournamentController from '../ctrl';
import { TournamentData } from '../interfaces';
import * as pagination from '../pagination';
import { arenaControls, robinControls, organizedControls } from './controls';
import { recents, standing as rStanding, podium as rPodium } from './robin';
import { podium, standing } from './arena';
import { standing as oStanding } from './organized';
import { teamStanding } from './battle';
import header from './header';
import playerInfo from './player-info';
import teamInfo from './team-info';
import { numberRow } from './util';
import { once } from 'common/storage';
import { loadCompiledScript } from 'common/assets';
import { colorName } from 'shogi/color-name';
import { i18n, i18nFormatCapitalized } from 'i18n';

function confetti(data: TournamentData): VNode | undefined {
  if (data.me && data.isRecentlyFinished && once('tournament.end.canvas.' + data.id))
    return h('canvas#confetti', {
      hook: {
        insert: _ => loadCompiledScript('misc.confetti'),
      },
    });
}

function stats(data: TournamentData): VNode {
  const tableData = [
    numberRow(i18n('averageElo'), data.stats.averageRating, 'raw'),
    numberRow(i18n('gamesPlayed'), data.stats.games),
    numberRow(i18n('movesPlayed'), data.stats.moves),
    numberRow(
      i18nFormatCapitalized('xWins', colorName('sente', false)),
      [data.stats.senteWins, data.stats.games],
      'percent'
    ),
    numberRow(
      i18nFormatCapitalized('xWins', colorName('gote', false)),
      [data.stats.goteWins, data.stats.games],
      'percent'
    ),
    numberRow(i18n('draws'), [data.stats.draws, data.stats.games], 'percent'),
  ];

  if (data.berserkable) {
    const berserkRate = [data.stats.berserks / 2, data.stats.games];
    tableData.push(numberRow(i18n('berserkRate'), berserkRate, 'percent'));
  }

  return h('div.tour__stats', [h('h2', i18n('tournamentComplete')), h('table', tableData)]);
}

export const name = 'finished';

export function main(ctrl: TournamentController): MaybeVNodes {
  const pag = pagination.players(ctrl);
  const teamS = teamStanding(ctrl, 'finished');
  if (ctrl.isArena())
    return [
      ...(teamS
        ? [header(ctrl), teamS]
        : [h('div.big_top', [confetti(ctrl.data), header(ctrl), podium(ctrl)])]),
      arenaControls(ctrl, pag),
      standing(ctrl, pag),
    ];
  else if (ctrl.isRobin())
    return [
      ...(teamS
        ? [header(ctrl), teamS]
        : [h('div.big_top', [confetti(ctrl.data), header(ctrl), rPodium(ctrl)])]),
      robinControls(ctrl),
      rStanding(ctrl, 'finished'),
      recents(ctrl),
    ];
  else
    return [
      ...(teamS
        ? [header(ctrl), teamS]
        : [h('div.big_top', [confetti(ctrl.data), header(ctrl), rPodium(ctrl)])]),
      organizedControls(ctrl, pag),
      oStanding(ctrl, pag, 'finished'),
      recents(ctrl),
    ];
}

export function table(ctrl: TournamentController): VNode | undefined {
  return ctrl.playerInfo.id
    ? playerInfo(ctrl)
    : ctrl.teamInfo.requested
      ? teamInfo(ctrl)
      : stats
        ? stats(ctrl.data)
        : undefined;
}
