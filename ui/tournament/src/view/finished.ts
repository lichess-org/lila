import { COLORS } from 'chessops';
import { h, type VNode } from 'snabbdom';

import { licon } from 'lib/licon';
import { once } from 'lib/storage';
import { type MaybeVNodes } from 'lib/view';
import { numberRow } from 'lib/view/util';

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

  // Helper function: generate divider + group label text (using attrs.style to avoid TS type inference errors)
  const sectionHeader = (text: string) =>
    h(
      'div',
      { attrs: { style: 'display: flex; align-items: center; margin: 1.2em 0 0.6em 0; gap: 0.8em;' } },
      [
        h(
          'small',
          {
            attrs: {
              style:
                'color: var(--muted-color); font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; font-size: 0.75em; white-space: nowrap;',
            },
          },
          text,
        ),
        h('hr', { attrs: { style: 'flex: 1; border: none; border-top: 1px solid var(--border-color);' } }),
      ],
    );

  const links: MaybeVNodes = [];

  // 1. Players
  links.push(sectionHeader('Players'));

  if (data.teamBattle) {
    links.push(
      h(
        'a',
        { attrs: { href: `/tournament/${data.id}/teams` } },
        i18n.arena.viewAllXTeams(Object.keys(data.teamBattle.teams).length),
      ),
      h('br'),
    );
  }

  links.push(
    h(
      'a.text',
      { attrs: { 'data-icon': licon.Download, href: `/api/tournament/${data.id}/games`, download: true } },
      i18n.site.downloadAllGames,
    ),
  );
  if (data.me) {
    links.push(
      h(
        'a.text',
        {
          attrs: {
            'data-icon': licon.Download,
            href: `/api/tournament/${data.id}/games?player=${ctrl.opts.userId}`,
            download: true,
          },
        },
        i18n.site.downloadMyGames,
      ),
    );
  }

  // 2. Data & Analysis
  links.push(sectionHeader('Data & Analysis'));
  links.push(
    h(
      'a.text',
      {
        attrs: {
          'data-icon': licon.Download,
          href: `/api/tournament/${data.id}/results?as=csv`,
          download: true,
        },
      },
      i18n.site.downloadResultsAsCsv,
    ),
    h(
      'a.text',
      { attrs: { 'data-icon': licon.Download, href: `/api/tournament/${data.id}/results`, download: true } },
      i18n.site.downloadResultsAsNdjson,
    ),
  );

  // 3. Developers
  links.push(sectionHeader('Developers'));
  links.push(
    h(
      'a.text',
      { attrs: { 'data-icon': licon.InfoCircle, href: '/api#tag/arena-tournaments' } },
      i18n.site.arenaApiDocumentation,
    ),
  );

  return h('div.tour__stats', [
    h('h2', i18n.site.tournamentComplete),
    h('table', tableData),
    h('div.tour__stats__links.force-ltr', links),
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
