import { Chessground } from 'chessground';
import { h, VNode } from 'snabbdom';
import { makeConfig as makeCgConfig } from './chessground';
import { onInsert } from 'common/snabbdom';

export default function (ctrl: any): VNode {
  return h('div#local-play', renderPlay(ctrl));
}

function chessground(ctrl: any): VNode {
  return h('div.cg-wrap', {
    hook: {
      insert: vnode => (ctrl.cg = Chessground(vnode.elm as HTMLElement, makeCgConfig(ctrl))),
    },
  });
}

function renderPlay(ctrl: any): VNode[] {
  return [
    h('div.puz-board.main-board', [chessground(ctrl), ctrl.promotion.view()]),
    h('div.puz-side', [
      h(
        'div',
        h('div#black.puz-bot', { hook: onInsert(el => ctrl.dropHandler('black', el)) }, [
          h('p', 'Drop black weights here (otherwise stockfish)'),
        ])
      ),
      h('div#pgn'),
      h('div', [
        h('div#white.puz-bot', { hook: onInsert(el => ctrl.dropHandler('white', el)) }, [
          h('p', 'Drop white weights here (otherwise human)'),
        ]),
        h('hr'),
        h(
          'span',
          h(
            'button#go.button.disabled',
            { hook: onInsert(el => el.addEventListener('click', ctrl.go.bind(ctrl))) },
            'GO'
          )
        ),
      ]),
    ]),
  ];
}
