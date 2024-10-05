import { Board, SwissOpts } from '../interfaces';
import { renderClock } from 'common/miniBoard';
import { h, VNode } from 'snabbdom';
import { opposite } from 'chessground/util';
import { player as renderPlayer } from './util';

export function many(boards: Board[], opts: SwissOpts): VNode {
  return h('div.swiss__boards.now-playing', boards.map(renderBoard(opts)));
}

export function top(boards: Board[], opts: SwissOpts): VNode {
  return h('div.swiss__board__top.swiss__table', boards.slice(0, 1).map(renderBoard(opts)));
}

const renderBoard =
  (opts: SwissOpts) =>
  (board: Board): VNode =>
    h(
      `div.swiss__board.mini-game.mini-game-${board.id}.mini-game--init.is2d`,
      {
        key: board.id,
        attrs: { 'data-state': `${board.fen},${board.orientation},${board.lastMove}`, 'data-live': board.id },
        hook: {
          insert(vnode) {
            site.powertip.manualUserIn(vnode.elm as HTMLElement);
          },
        },
      },
      [
        boardPlayer(board, opposite(board.orientation), opts),
        h('a.cg-wrap', { attrs: { href: `/${board.id}/${board.orientation}` } }),
        boardPlayer(board, board.orientation, opts),
      ],
    );

function boardPlayer(board: Board, color: Color, opts: SwissOpts) {
  const player = board[color];
  return h('span.mini-game__player', [
    h('span.mini-game__user', [h('strong', '#' + player.rank), renderPlayer(player, true, opts.showRatings)]),
    board.clock
      ? renderClock(color, board.clock[color])
      : h('span.mini-game__result', board.winner ? (board.winner == color ? 1 : 0) : '½'),
  ]);
}
