import { Chessground } from 'chessground';
import { h, VNode } from 'snabbdom';
import { makeConfig } from '../chessground';
import { onInsert } from 'common/snabbdom';
import { ZerofishCtrl } from './ctrl';

export default function render(ctrl: ZerofishCtrl): VNode {
  return h('div#local-play', [
    h('div.puz-board.main-board', [chessground(ctrl), ctrl.promotion.view()]),
    h('div.puz-side', [
      h('div', bot(ctrl, 'black')),
      h('div#pgn'),
      h('div', [bot(ctrl, 'white'), h('hr'), controls(ctrl)]),
    ]),
  ]);
}

function chessground(ctrl: ZerofishCtrl): VNode {
  return h('div.cg-wrap', {
    hook: {
      insert: vnode => (ctrl.cg = Chessground(vnode.elm as HTMLElement, makeConfig(ctrl))),
    },
  });
}

function bot(ctrl: ZerofishCtrl, color: Color): VNode {
  return h(`div#${color}.puz-bot`, { hook: onInsert(el => ctrl.dropHandler(color, el)) }, [
    h('p', 'Drop weights here (otherwise stockfish)'),
    h(`p#${color}-totals.totals`),
  ]);
}

function controls(ctrl: ZerofishCtrl) {
  return h('span', [
    h(
      'button#go.button.disabled',
      {
        hook: onInsert(el =>
          el.addEventListener('click', () => ctrl.go(parseInt($('#num-games').val() as string) || 1)),
        ),
      },
      'GO',
    ),
    h('input#num-games', {
      attrs: { type: 'number', min: '1', max: '1000', value: '1' },
    }),
  ]);
}
