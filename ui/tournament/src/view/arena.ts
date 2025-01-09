import { MaybeVNodes, bind, dataIcon } from 'common/snabbdom';
import { VNode, h } from 'snabbdom';
import TournamentController from '../ctrl';
import { teamName } from './battle';
import { playerName, preloadUserTips, ratio2percent, player as renderPlayer } from './util';
import { PageData } from '../interfaces';
import { i18n } from 'i18n';

const scoreTagNames = ['score', 'streak', 'double'];

function scoreTag(s) {
  return h(scoreTagNames[(s[1] || 1) - 1], [Array.isArray(s) ? s[0] : s]);
}

function playerTr(ctrl: TournamentController, player) {
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
                'data-icon': 'Z',
                title: i18n('pause'),
              },
            })
          : player.rank
      ),
      h('td.player', [
        renderPlayer(player, false, true, userId === ctrl.data.defender),
        ...(battle && player.team ? [' ', teamName(battle, player.team)] : []),
      ]),
      h('td.sheet', player.sheet.scores.map(scoreTag)),
      h('td.total', [
        player.sheet.fire && !ctrl.data.isFinished
          ? h('strong.is-gold', { attrs: dataIcon('Q') }, player.sheet.total)
          : h('strong', player.sheet.total),
      ]),
    ]
  );
}

function podiumUsername(p) {
  return h(
    'a.text.ulpt.user-link',
    {
      attrs: { href: '/@/' + p.name },
    },
    playerName(p)
  );
}

function podiumStats(p): VNode {
  const nb = p.nb;
  return h('table.stats', [
    p.performance ? h('tr', [h('th', i18n('performance')), h('td', p.performance)]) : null,
    h('tr', [h('th', i18n('gamesPlayed')), h('td', nb.game)]),
    ...(nb.game
      ? [
          h('tr', [h('th', i18n('winRate')), h('td', ratio2percent(nb.win / nb.game))]),
          h('tr', [h('th', i18n('berserkRate')), h('td', ratio2percent(nb.berserk / nb.game))]),
        ]
      : []),
  ]);
}

function podiumPosition(p, pos): VNode | undefined {
  if (p) return h('div.' + pos, [h('div.trophy'), podiumUsername(p), podiumStats(p)]);
}

let lastBody: MaybeVNodes | undefined;

export function podium(ctrl: TournamentController): VNode {
  const p = ctrl.data.podium || [];
  return h('div.tour__podium', [
    podiumPosition(p[1], 'second'),
    podiumPosition(p[0], 'first'),
    podiumPosition(p[2], 'third'),
  ]);
}

export function standing(ctrl: TournamentController, pag: PageData, klass?: string): VNode {
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
