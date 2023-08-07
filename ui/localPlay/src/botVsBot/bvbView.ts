import { Chessground } from 'chessground';
import { h, VNode } from 'snabbdom';
import { makeBvbConfig as makeCgConfig } from './bvbChessground';
import { onInsert } from 'common/snabbdom';
import { BvbCtrl } from './bvbCtrl';

export default function render(ctrl: BvbCtrl): VNode {
  return h('div#local-play', [
    h('div.puz-board.main-board', [chessground(ctrl), ctrl.promotion.view()]),
    h('div.puz-side', [
      h('div', bot(ctrl, 'black')),
      h('div#pgn'),
      h('div', [bot(ctrl, 'white'), h('hr'), controls(ctrl)]),
    ]),
  ]);
}

function chessground(ctrl: BvbCtrl): VNode {
  return h('div.cg-wrap', {
    hook: {
      insert: vnode => (ctrl.cg = Chessground(vnode.elm as HTMLElement, makeCgConfig(ctrl))),
    },
  });
}

function bot(ctrl: BvbCtrl, color: Color): VNode {
  return h(`div#${color}.puz-bot`, { hook: onInsert(el => ctrl.dropHandler(color, el)) }, [
    h('p', 'Drop weights here (otherwise stockfish)'),
    h(`p#${color}-totals.totals`),
  ]);
}

function controls(ctrl: BvbCtrl) {
  return h('span', [
    h(
      'button#go.button.disabled',
      {
        hook: onInsert(el =>
          el.addEventListener('click', () => ctrl.go(parseInt($('#num-games').val() as string) || 1))
        ),
      },
      'GO'
    ),
    h('input#num-games', {
      attrs: { type: 'number', min: '1', max: '1000', value: '1' },
    }),
  ]);
}
