import { COLORS } from 'chessops';
import { h, type VNode } from 'snabbdom';

import { licon } from 'lib/licon';
import { once } from 'lib/storage';
import { a, type MaybeVNodes } from 'lib/view';
import { numberRow, toggleBox } from 'lib/view/util';

import type TournamentController from '../ctrl';
import type { TournamentData } from '../interfaces';
import { controls, standing, podium } from './arena';
import { teamStanding } from './battle';
import header from './header';
import playerInfo from './playerInfo';
import teamInfo from './teamInfo';

function confetti(data: TournamentData): VNode | undefined {
  if (data.me && data.isRecentlyFinished && once('tournament.end.canvas.' + data.id))
    return h('canvas#confetti', {
      hook: { insert: _ => site.asset.loadEsm('bits.confetti') },
    });
  return undefined;
}

const downloadLink = (href: string, text: MaybeVNodes) =>
  a(href)('.text', { 'data-icon': licon.Download, download: true }, text);

function stats(ctrl: TournamentController): VNode | undefined {
  const data = ctrl.data;
  const stats = data.stats;
  if (!stats) return undefined;
  const tableData = [
    ctrl.opts.showRatings ? numberRow(i18n.site.averageElo, stats.averageRating, 'raw') : null,
    numberRow(i18n.site.gamesPlayed, stats.games),
    numberRow(i18n.site.movesPlayed, stats.moves),
    ...COLORS.map(c => numberRow(i18n.site[`${c}Wins`], [stats[`${c}Wins`], stats.games], 'percent')),
    numberRow(i18n.site.drawRate, [stats.draws, stats.games], 'percent'),
  ];

  if (data.berserkable) {
    tableData.push(numberRow(i18n.arena.berserkRate, [stats.berserks / 2, stats.games], 'percent'));
  }

  return h('div.tour__stats', [
    h('h2', i18n.site.tournamentComplete),
    h('table', tableData),
    h('div.tour__stats__downloads', [
      ...(data.teamBattle
        ? [
            h(
              'a',
              { attrs: { href: `/tournament/${data.id}/teams` } },
              i18n.arena.viewAllXTeams(Object.keys(data.teamBattle.teams).length),
            ),
            h('br'),
          ]
        : []),
      toggleBox(i18n.site.download, [
        downloadLink(`/api/tournament/${data.id}/games`, [i18n.site.downloadAllGames, ' (PGN)']),
        data.me &&
          downloadLink(`/api/tournament/${data.id}/games?player=${ctrl.opts.userId}`, [
            i18n.site.downloadMyGames,
            ' (PGN)',
          ]),
        downloadLink(`/api/tournament/${data.id}/results`, [i18n.site.downloadResults, ' (NDJSON)']),
        downloadLink(`/api/tournament/${data.id}/results?as=csv`, [i18n.site.downloadResults, ' (CSV)']),
      ]),
    ]),
  ]);
}

export const name = 'finished';

export function main(ctrl: TournamentController): MaybeVNodes {
  return [
    h('div.podium-wrap', [confetti(ctrl.data), header(ctrl), teamStanding(ctrl, 'finished') || podium(ctrl)]),
    controls(ctrl),
    standing(ctrl),
  ];
}

export function table(ctrl: TournamentController): VNode | undefined {
  return ctrl.playerInfo.id ? playerInfo(ctrl) : ctrl.teamInfo.requested ? teamInfo(ctrl) : stats(ctrl);
}
