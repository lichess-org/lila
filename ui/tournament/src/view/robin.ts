import { bind, MaybeVNode } from 'common/snabbdom';
import { ids } from 'game/status';
import { VNode, h } from 'snabbdom';
import TournamentController from '../ctrl';
import {
  arrangementHasUser,
  playerName,
  preloadUserTips,
  ratio2percent,
  player as renderPlayer,
} from './util';
import { Arrangement } from '../interfaces';
import { arrangementThumbnail } from './arrangement-thumbnail';
import { i18n } from 'i18n';

function tableClick(ctrl: TournamentController): (e: Event) => void {
  return (e: Event) => {
    const target = e.target as HTMLElement;
    const players = target.dataset.p;
    console.log(players);
    if (players) {
      ctrl.showArrangement(ctrl.findOrCreateArrangement(players.split(';')));
    }
  };
}

function playerNameStanding(_ctrl: TournamentController, player) {
  const userId = player.name.toLowerCase();
  return h(
    'div',
    {
      key: userId,
    },
    [
      player.withdraw
        ? h('i', {
            attrs: {
              'data-icon': player.kicked ? 'L' : 'Z',
            },
          })
        : undefined,
      renderPlayer(player, false, true),
    ]
  );
}

export function standing(ctrl: TournamentController, klass?: string): VNode {
  const maxScore = Math.max(...ctrl.data.standing.players.map(p => p.score || 0)),
    size = ctrl.data.standing.players.length;
  return h('div.r-table-wrap' + (klass ? '.' + klass : '') + (size === 0 ? '.none' : ''), [
    h(
      'div.r-table-wrap-players',
      h('table', [
        h('thead', h('tr', [h('th', '#'), h('th', 'Player')])),
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
          ctrl.data.standing.players.map((player, i) =>
            h(
              'tr',
              {
                class: {
                  me: ctrl.opts.userId === player.id,
                  long: player.name.length > 15,
                  kicked: player.kicked,
                },
              },
              [h('td', i + 1), h('td.player-name', playerNameStanding(ctrl, player))]
            )
          )
        ),
      ])
    ),
    h(
      'div.r-table-wrap-arrs',
      h('table', [
        h(
          'thead',
          h(
            'tr',
            ctrl.data.standing.players.map((player, i) =>
              h('th', { attrs: { title: player.name } }, i + 1)
            )
          )
        ),
        h(
          'tbody',
          { hook: bind('click', tableClick(ctrl)) },
          ctrl.data.standing.players.map((player, i) =>
            h(
              'tr',
              {
                class: {
                  kicked: player.kicked,
                },
              },
              ctrl.data.standing.players.map((player2, j) => {
                const arr = ctrl.findArrangement([player.id, player2.id]),
                  key = player.id + ';' + player2.id;
                return h(
                  'td',
                  {
                    attrs: {
                      title: `${player.name} vs ${player2.name}`,
                      'data-p': key,
                    },
                    class: {
                      same: i === j,
                      h: ctrl.highlightArrs.includes(key),
                    },
                  },
                  !!arr?.status
                    ? h('div', {
                        class: {
                          p: arr.status == ids.started,
                          d: arr.status >= ids.mate && !arr.winner,
                          w: arr.winner === player.id,
                          l: arr.winner === player2.id,
                        },
                      })
                    : null
                );
              })
            )
          )
        ),
      ])
    ),
    h(
      'div.r-table-wrap-scores',
      h('table', [
        h('thead', h('tr', h('th', 'Σ'))),
        h(
          'tbody',
          ctrl.data.standing.players.map(player =>
            h(
              'tr',
              { class: { kicked: player.kicked } },
              h(
                'td',
                { class: { winner: !!maxScore && maxScore === player.score } },
                player.score || 0
              )
            )
          )
        ),
      ])
    ),
  ]);
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

function podiumStats(p, games: Arrangement[]): VNode {
  const userId = p.id,
    gamesOfPlayer = games.filter(a => arrangementHasUser(a, userId));
  return h('table.stats', [
    p.performance ? h('tr', [h('th', i18n('performance')), h('td', p.performance)]) : null,
    h('tr', [h('th', i18n('gamesPlayed')), h('td', gamesOfPlayer.length)]),
    ...(gamesOfPlayer.length
      ? [
          h('tr', [
            h('th', i18n('winRate')),
            h(
              'td',
              ratio2percent(
                gamesOfPlayer.filter(g => g.winner === userId).length / gamesOfPlayer.length
              )
            ),
          ]),
        ]
      : []),
  ]);
}

function podiumPosition(p, pos, games: Arrangement[]): VNode | undefined {
  if (p) return h('div.' + pos, [h('div.trophy'), podiumUsername(p), podiumStats(p, games)]);
}

export function podium(ctrl: TournamentController): VNode {
  const p = [...ctrl.data.standing.players].sort((a, b) => b.score - a.score).slice(0, 3),
    games = ctrl.data.standing.arrangements.filter(a => !!a.gameId);
  return h('div.tour__podium', [
    podiumPosition(p[1], 'second', games),
    podiumPosition(p[0], 'first', games),
    podiumPosition(p[2], 'third', games),
  ]);
}

export function yourCurrent(ctrl: TournamentController): MaybeVNode {
  const arrs = (ctrl.data.standing.arrangements as Arrangement[])
    .filter(a => arrangementHasUser(a, ctrl.opts.userId) && a.status === ids.started)
    .sort((a, b) => a.scheduledAt! - b.scheduledAt!)
    .map(a => arrangementThumbnail(ctrl, a));
  return arrs.some(a => !!a)
    ? h('div.arrs.arrs-current', [
        h('h2.arrs-title', 'Your current games'),
        h('div.arrs-grid', arrs),
      ])
    : null;
}

export function yourUpcoming(ctrl: TournamentController): MaybeVNode {
  const arrs = (ctrl.data.standing.arrangements as Arrangement[])
    .filter(a => arrangementHasUser(a, ctrl.opts.userId) && a.scheduledAt && !a.gameId)
    .sort((a, b) => a.scheduledAt! - b.scheduledAt!)
    .map(a => arrangementThumbnail(ctrl, a));
  return arrs.some(a => !!a)
    ? h('div.arrs.arrs-your-upcoming', [
        h('h2.arrs-title', 'Your upcoming games'),
        h('div.arrs-grid', arrs),
      ])
    : null;
}

export function upcoming(ctrl: TournamentController): MaybeVNode {
  const arrs = (ctrl.data.standing.arrangements as Arrangement[])
    .filter(a => !a.gameId)
    .map(a => arrangementThumbnail(ctrl, a));
  return arrs.some(a => !!a)
    ? h('div.arrs.arrs-upcoming', [h('h2.arrs-title', 'Upcoming games'), h('div.arrs-grid', arrs)])
    : null;
}
export function playing(ctrl: TournamentController): MaybeVNode {
  const arrs = (ctrl.data.standing.arrangements as Arrangement[])
    .filter(a => a.status === ids.started)
    .map(a => arrangementThumbnail(ctrl, a));
  return arrs.some(a => !!a)
    ? h('div.arrs.arrs-playing', [
        h('h2.arrs-title', 'Playing right now'),
        h('div.arrs-grid', arrs),
      ])
    : null;
}

export function recents(ctrl: TournamentController): MaybeVNode {
  const arrs = (ctrl.data.standing.arrangements as Arrangement[])
    .filter(a => a.status && a.status >= ids.mate)
    .slice(0, 3)
    .map(a => arrangementThumbnail(ctrl, a));
  return arrs.some(a => !!a)
    ? h('div.arrs.arrs-recents', [
        h('h2.arrs-title', 'Recently played games'),
        h('div.arrs-grid', arrs),
      ])
    : null;
}

export function howDoesThisWork(): VNode {
  return h('div.tour__faq.r-how', [
    h('h2', 'Rules'),
    h('div', [
      h(
        'p',
        'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus ac quam nec leo facilisis dignissim. Sed fringilla mi vel augue consequat, at cursus dolor laoreet.'
      ),
      h(
        'p',
        'Suspendisse potenti. Integer quis orci at sapien viverra malesuada. Donec gravida, eros ac facilisis laoreet, justo arcu malesuada urna, ut vehicula orci lectus ac lacus.'
      ),
      h(
        'p',
        'Proin volutpat sapien vel augue interdum, id feugiat mi ultrices. Ut tristique ipsum ac arcu vehicula, ut eleifend odio tempor. Aliquam erat volutpat.'
      ),
      h(
        'p',
        'Maecenas ultricies magna sit amet lectus volutpat, a luctus libero fermentum. Nam et nisl non magna eleifend venenatis. Cras ut turpis elit. Fusce auctor sem at urna commodo, at placerat tortor ultrices.'
      ),
    ]),
  ]);
}
