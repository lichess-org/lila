import { h, type VNode } from 'snabbdom';
import * as licon from 'lib/licon';
import type TournamentController from '../ctrl';
import type { TournamentData } from '../interfaces';
import { players } from '../pagination';
import { controls, standing, podium } from './arena';
import { teamStanding } from './battle';
import header from './header';
import playerInfo from './playerInfo';
import teamInfo from './teamInfo';
import { numberRow } from 'lib/view/util';
import { type MaybeVNodes } from 'lib/view';
import { once } from 'lib/storage';

function confetti(data: TournamentData): VNode | undefined {
  if (data.me && data.isRecentlyFinished && once('tournament.end.canvas.' + data.id))
    return h('canvas#confetti', {
      hook: { insert: _ => site.asset.loadEsm('bits.confetti') },
    });
  return undefined;
}

function stats(ctrl: TournamentController): VNode | undefined {
  const data = ctrl.data;
  if (!data.stats) return undefined;
  const tableData = [
    ctrl.opts.showRatings ? numberRow(i18n.site.averageElo, data.stats.averageRating, 'raw') : null,
    numberRow(i18n.site.gamesPlayed, data.stats.games),
    numberRow(i18n.site.movesPlayed, data.stats.moves),
    numberRow(i18n.site.whiteWins, [data.stats.whiteWins, data.stats.games], 'percent'),
    numberRow(i18n.site.blackWins, [data.stats.blackWins, data.stats.games], 'percent'),
    numberRow(i18n.site.drawRate, [data.stats.draws, data.stats.games], 'percent'),
  ];

  if (data.berserkable) {
    tableData.push(numberRow(i18n.arena.berserkRate, [data.stats.berserks / 2, data.stats.games], 'percent'));
  }

  return h('div.tour__stats', [
    h('h2', i18n.site.tournamentComplete),
    h('table', tableData),
    h('div.tour__stats__links.force-ltr', [
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
      h(
        'a.text',
        { attrs: { 'data-icon': licon.Download, href: `/api/tournament/${data.id}/games`, download: true } },
        i18n.site.downloadAllGames,
      ),
      data.me &&
        h(
          'a.text',
          {
            attrs: {
              'data-icon': licon.Download,
              href: `/api/tournament/${data.id}/games?player=${ctrl.opts.userId}`,
              download: true,
            },
          },
          'Download my games',
        ),
      h(
        'a.text',
        {
          attrs: { 'data-icon': licon.Download, href: `/api/tournament/${data.id}/results`, download: true },
        },
        'Download results as NDJSON',
      ),
      h(
        'a.text',
        {
          attrs: {
            'data-icon': licon.Download,
            href: `/api/tournament/${data.id}/results?as=csv`,
            download: true,
          },
        },
        'Download results as CSV',
      ),
      h('br'),
      h(
        'a.text',
        { attrs: { 'data-icon': licon.InfoCircle, href: '/api#tag/arena-tournaments' } },
        'Arena API documentation',
      ),
    ]),
  ]);
}

export const name = 'finished';

export function main(ctrl: TournamentController): MaybeVNodes {
  const pag = players(ctrl);
  return [
    h('div.podium-wrap', [confetti(ctrl.data), header(ctrl), teamStanding(ctrl, 'finished') || podium(ctrl)]),
    controls(ctrl, pag),
    standing(ctrl, pag),
  ];
}

export function table(ctrl: TournamentController): VNode | undefined {
  return ctrl.playerInfo.id ? playerInfo(ctrl) : ctrl.teamInfo.requested ? teamInfo(ctrl) : stats(ctrl);
}
