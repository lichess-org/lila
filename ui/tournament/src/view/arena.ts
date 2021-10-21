import { h, VNode } from 'snabbdom';
import { bind, dataIcon } from 'common/snabbdom';
import TournamentController from '../ctrl';
import { player as renderPlayer, ratio2percent, playerName } from './util';
import { teamName } from './battle';
import { MaybeVNodes, Pagination, PodiumPlayer, Score, StandingPlayer } from '../interfaces';
import * as button from './button';
import * as pagination from '../pagination';

const scoreTagNames = ['score', 'streak', 'double'];

function scoreTag(s: Score) {
  const [score, tag] = Array.isArray(s) ? s : [s];
  return h(scoreTagNames[(tag || 1) - 1], [score]);
}

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
          ? h('i', {
              attrs: {
                'data-icon': '',
                title: ctrl.trans.noarg('pause'),
              },
            })
          : player.rank
      ),
      h('td.player', [
        renderPlayer(player, false, ctrl.opts.showRatings, userId === ctrl.data.defender),
        ...(battle && player.team ? [' ', teamName(battle, player.team)] : []),
      ]),
      h('td.sheet', player.sheet.scores.map(scoreTag)),
      h('td.total', [
        player.sheet.fire && !ctrl.data.isFinished
          ? h('strong.is-gold', { attrs: dataIcon('') }, player.sheet.total)
          : h('strong', player.sheet.total),
      ]),
    ]
  );
}

function podiumUsername(p: PodiumPlayer) {
  return h(
    'a.text.ulpt.user-link',
    {
      attrs: { href: '/@/' + p.name },
    },
    playerName(p)
  );
}

function podiumStats(p: PodiumPlayer, berserkable: boolean, ctrl: TournamentController): VNode {
  const noarg = ctrl.trans.noarg,
    nb = p.nb;
  return h('table.stats', [
    p.performance && ctrl.opts.showRatings ? h('tr', [h('th', noarg('performance')), h('td', p.performance)]) : null,
    h('tr', [h('th', noarg('gamesPlayed')), h('td', nb.game)]),
    ...(nb.game
      ? [
          h('tr', [h('th', noarg('winRate')), h('td', ratio2percent(nb.win / nb.game))]),
          berserkable ? h('tr', [h('th', noarg('berserkRate')), h('td', ratio2percent(nb.berserk / nb.game))]) : null,
        ]
      : []),
  ]);
}

function podiumPosition(
  p: PodiumPlayer,
  pos: string,
  berserkable: boolean,
  ctrl: TournamentController
): VNode | undefined {
  if (p) return h('div.' + pos, [h('div.trophy'), podiumUsername(p), podiumStats(p, berserkable, ctrl)]);
  return undefined;
}

let lastBody: MaybeVNodes | undefined;

export function podium(ctrl: TournamentController) {
  const p = ctrl.data.podium || [];
  return h('div.podium', [
    podiumPosition(p[1], 'second', ctrl.data.berserkable, ctrl),
    podiumPosition(p[0], 'first', ctrl.data.berserkable, ctrl),
    podiumPosition(p[2], 'third', ctrl.data.berserkable, ctrl),
  ]);
}

function preloadUserTips(el: HTMLElement) {
  lichess.powertip.manualUserIn(el);
}

export function controls(ctrl: TournamentController, pag: Pagination): VNode {
  return h('div.tour__controls', [h('div.pager', pagination.renderPager(ctrl, pag)), button.joinWithdraw(ctrl)]);
}

export function standing(ctrl: TournamentController, pag: Pagination, klass?: string): VNode {
  const tableBody = pag.currentPageResults ? pag.currentPageResults.map(res => playerTr(ctrl, res)) : lastBody;
  if (pag.currentPageResults) lastBody = tableBody;
  return h(
    'table.slist.tour__standing' + (klass ? '.' + klass : ''),
    {
      class: { loading: !pag.currentPageResults },
    },
    [
      h(
        'tbody',
        {
          hook: {
            insert: vnode => preloadUserTips(vnode.elm as HTMLElement),
            update(_, vnode) {
              preloadUserTips(vnode.elm as HTMLElement);
            },
          },
        },
        tableBody
      ),
    ]
  );
}
