import Ctrl from './ctrl';
import { h } from 'snabbdom';
import { Game } from './interfaces';
import { onInsert } from 'common/snabbdom';

function miniGame(game: Game) {
  return h(
    'a',
    {
      attrs: {
        key: game.id,
        href: `/${game.id}/${game.color}`,
      },
    },
    [
      h('span.mini-board.is2d', {
        attrs: { 'data-state': `${game.fen},${game.color},${game.lastMove}` },
        hook: onInsert(el => lichess.miniBoard.init(el)),
      }),
      h('span.vstext', [
        h('span.vstext__pl', [
          game.user1.name,
          h('br'),
          game.user1.title ? game.user1.title + ' ' : '',
          game.user1.rating,
        ]),
        h('span.vstext__op', [
          game.user2.name,
          h('br'),
          game.user2.rating,
          game.user2.title ? ' ' + game.user2.title : '',
        ]),
      ]),
    ]
  );
}

export default function (ctrl: Ctrl) {
  if (!ctrl.vm.answer) return;

  return h('div.game-sample.box', [
    h('div.top', 'Some of the games used to generate this insight'),
    h('div.boards', ctrl.vm.answer.games.map(miniGame)),
  ]);
}
