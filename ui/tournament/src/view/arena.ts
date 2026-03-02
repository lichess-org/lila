import { h, type VNode } from 'snabbdom';
import * as licon from 'lib/licon';
import { bind, dataIcon, type MaybeVNodes } from 'lib/view';
import { numberRow } from 'lib/view/util';
import type TournamentController from '../ctrl';
import { player as renderPlayer } from './util';
import { teamName } from './battle';
import type { PodiumPlayer, StandingPlayer } from '../interfaces';
import { joinWithdraw } from './button';
import { userLink } from 'lib/view/userLink';
import { defined } from 'lib';
import { renderPager, searchButton, searchInput } from 'lib/view/pagination';

const renderScoreString = (scoreString: string, streakable: boolean) => {
  const values = scoreString.split('').map(s => parseInt(s));
  values.reverse(); // in place!
  if (!streakable) return values.map(v => h(v > 1 ? 'streak' : 'score', v));
  const nodes = [];
  let streak = 0;
  for (const v of values) {
    const win = v === 2 ? streak < 2 : v > 2;
    const tag = streak > 1 && v > 1 ? 'double' : win ? 'streak' : 'score';
    if (win) {
      streak++;
    } else {
      streak = 0;
    }
    nodes.push(h(tag, v));
  }
  return nodes;
};

function playerTr(ctrl: TournamentController, player: StandingPlayer) {
  const userId = player.name.toLowerCase(),
    nbScores = player.sheet.scores.length;
  const battle = ctrl.data.teamBattle;
  return h(
    'tr',
    {
      key: userId,
      class: {
        me: ctrl.opts.userId === userId,
        long: nbScores > 35,
        xlong: nbScores > 80,
        active: ctrl.playerInfo.id === userId,
      },
      hook: bind('click', _ => ctrl.showPlayerInfo(player), ctrl.redraw),
    },
    [
      h(
        'td.rank',
        player.withdraw
          ? h('i', { attrs: { 'data-icon': licon.Pause, title: i18n.site.pause } })
          : player.rank,
      ),
      h('td.player', [
        renderPlayer(player, false, ctrl.opts.showRatings, userId === ctrl.data.defender),
        ...(battle && player.team ? [' ', teamName(battle, player.team)] : []),
      ]),
      h('td.sheet', renderScoreString(player.sheet.scores, !ctrl.data.noStreak)),
      h('td.total', [
        player.sheet.fire && !ctrl.data.isFinished
          ? h('strong.is-gold', { attrs: dataIcon(licon.Fire) }, player.score)
          : h('strong', player.score),
      ]),
    ],
  );
}

function podiumStats(p: PodiumPlayer, berserkable: boolean, ctrl: TournamentController): VNode {
  const nb = p.nb;
  return h('table.stats', [
    p.performance && ctrl.opts.showRatings
      ? h('tr', [h('th', i18n.site.performance), h('td', p.performance)])
      : null,
    h('tr', [h('th', i18n.site.gamesPlayed), h('td', nb.game)]),
    ...(nb.game
      ? [
          numberRow(i18n.site.winRate, [nb.win, nb.game], 'percent'),
          berserkable ? numberRow(i18n.arena.berserkRate, [nb.berserk, nb.game], 'percent') : null,
        ]
      : []),
  ]);
}

let lastBody: MaybeVNodes | undefined;

export function podium(ctrl: TournamentController) {
  const p = ctrl.data.podium || [];
  const podiumPosition = (p: PodiumPlayer, pos: string): VNode | undefined =>
    p
      ? h('div.' + pos, [
          h('div.trophy'),
          userLink({
            ...p,
            line: defined(p.patronColor),
            online: defined(p.patronColor),
            rating: undefined,
          }),
          podiumStats(p, ctrl.data.berserkable, ctrl),
        ])
      : undefined;
  return h('div.podium', [
    podiumPosition(p[1], 'second'),
    podiumPosition(p[0], 'first'),
    podiumPosition(p[2], 'third'),
  ]);
}

export function controls(ctrl: TournamentController): VNode {
  return h('div.tour__controls', [
    h('div.pager', renderPager(ctrl, searchButton(ctrl), searchInput(ctrl, { tour: ctrl.data.id }))),
    joinWithdraw(ctrl),
  ]);
}

export function standing(ctrl: TournamentController, klass?: string): VNode {
  const pag = ctrl.pager();
  const tableBody = pag.currentPageResults
    ? pag.currentPageResults.map(res => playerTr(ctrl, res))
    : lastBody;
  if (pag.currentPageResults) lastBody = tableBody;
  return h(
    'table.slist.tour__standing' + (klass ? '.' + klass : ''),
    { class: { loading: !pag.currentPageResults } },
    [
      h(
        'tbody',
        {
          hook: {
            insert: vnode => site.powertip.manualUserIn(vnode.elm as HTMLElement),
            update: (_, vnode) => site.powertip.manualUserIn(vnode.elm as HTMLElement),
          },
        },
        tableBody,
      ),
    ],
  );
}
