import { MaybeVNodes } from 'common/snabbdom';
import { transWithColorName } from 'common/colorName';
import { VNode, h } from 'snabbdom';
import TournamentController from '../ctrl';
import { TournamentData } from '../interfaces';
import * as pagination from '../pagination';
import { controls, podium, standing } from './arena';
import { teamStanding } from './battle';
import header from './header';
import playerInfo from './playerInfo';
import teamInfo from './teamInfo';
import { numberRow } from './util';

function confetti(data: TournamentData): VNode | undefined {
  if (data.me && data.isRecentlyFinished && window.lishogi.once('tournament.end.canvas.' + data.id))
    return h('canvas#confetti', {
      hook: {
        insert: _ => window.lishogi.loadScript('javascripts/confetti.js'),
      },
    });
}

function stats(data: TournamentData, trans: Trans): VNode {
  const noarg = trans.noarg,
    tableData = [
      numberRow(noarg('averageElo'), data.stats.averageRating, 'raw'),
      numberRow(noarg('gamesPlayed'), data.stats.games),
      numberRow(noarg('movesPlayed'), data.stats.moves),
      numberRow(
        transWithColorName(trans, 'xWins', 'sente', false),
        [data.stats.senteWins, data.stats.games],
        'percent'
      ),
      numberRow(transWithColorName(trans, 'xWins', 'gote', false), [data.stats.goteWins, data.stats.games], 'percent'),
      numberRow(noarg('draws'), [data.stats.draws, data.stats.games], 'percent'),
    ];

  if (data.berserkable) {
    const berserkRate = [data.stats.berserks / 2, data.stats.games];
    tableData.push(numberRow(noarg('berserkRate'), berserkRate, 'percent'));
  }

  return h('div.tour__stats', [h('h2', noarg('tournamentComplete')), h('table', tableData)]);
}

export const name = 'finished';

export function main(ctrl: TournamentController): MaybeVNodes {
  const pag = pagination.players(ctrl);
  const teamS = teamStanding(ctrl, 'finished');
  return [
    ...(teamS ? [header(ctrl), teamS] : [h('div.big_top', [confetti(ctrl.data), header(ctrl), podium(ctrl)])]),
    controls(ctrl, pag),
    standing(ctrl, pag),
  ];
}

export function table(ctrl: TournamentController): VNode | undefined {
  return ctrl.playerInfo.id
    ? playerInfo(ctrl)
    : ctrl.teamInfo.requested
      ? teamInfo(ctrl)
      : stats
        ? stats(ctrl.data, ctrl.trans)
        : undefined;
}
