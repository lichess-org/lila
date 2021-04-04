import { h } from 'snabbdom';
import { VNode } from 'snabbdom';
import { opposite } from 'chessground/util';
import { player as renderPlayer } from './util';
import { Board } from '../interfaces';

export function many(boards: Board[]): VNode {
  return h('div.swiss__boards.now-playing', boards.map(renderBoard));
}

export function top(boards: Board[]): VNode {
  return h('div.swiss__board__top.swiss__table', boards.slice(0, 1).map(renderBoard));
}

const renderBoard = (board: Board): VNode =>
  h(
    `div.swiss__board.mini-game.mini-game-${board.id}.mini-game--init.is2d`,
    {
      key: board.id,
      attrs: {
        'data-state': `${board.fen},${board.orientation},${board.lastMove}`,
        'data-live': board.id,
      },
      hook: {
        insert(vnode) {
          lichess.powertip.manualUserIn(vnode.elm as HTMLElement);
        },
      },
    },
    [
      boardPlayer(board, opposite(board.orientation)),
      h('a.cg-wrap', {
        attrs: {
          href: `/${board.id}/${board.orientation}`,
        },
      }),
      boardPlayer(board, board.orientation),
    ]
  );

function boardPlayer(board: Board, color: Color) {
  const player = board[color];
  return h('span.mini-game__player', [
    h('span.mini-game__user', [h('strong', '#' + player.rank), renderPlayer(player, true, true)]),
    board.clock
      ? h(`span.mini-game__clock.mini-game__clock--${color}`, {
          attrs: {
            'data-time': board.clock[color],
            'data-managed': 1,
          },
        })
      : h('span.mini-game__result', board.winner ? (board.winner == color ? 1 : 0) : '½'),
  ]);
}
