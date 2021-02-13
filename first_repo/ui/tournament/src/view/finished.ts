import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import TournamentController from '../ctrl';
import { TournamentData, MaybeVNodes } from '../interfaces';
import * as pagination from '../pagination';
import { controls, standing, podium } from './arena';
import { teamStanding } from './battle';
import header from './header';
import playerInfo from './playerInfo';
import teamInfo from './teamInfo';
import { numberRow } from './util';

function confetti(data: TournamentData): VNode | undefined {
  if (data.me && data.isRecentlyFinished && lichess.once('tournament.end.canvas.' + data.id))
    return h('canvas#confetti', {
      hook: {
        insert: _ => lichess.loadScript('javascripts/confetti.js'),
      },
    });
}

function stats(data: TournamentData, noarg: any): VNode {
  const tableData = [
    numberRow(noarg('averageElo'), data.stats.averageRating, 'raw'),
    numberRow(noarg('gamesPlayed'), data.stats.games),
    numberRow(noarg('movesPlayed'), data.stats.moves),
    numberRow(noarg('whiteWins'), [data.stats.whiteWins, data.stats.games], 'percent'),
    numberRow(noarg('blackWins'), [data.stats.blackWins, data.stats.games], 'percent'),
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
    ...(teamS ? [header(ctrl), teamS] : [h('div.podium-wrap', [confetti(ctrl.data), header(ctrl), podium(ctrl)])]),
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
    ? stats(ctrl.data, ctrl.trans.noarg)
    : undefined;
}
