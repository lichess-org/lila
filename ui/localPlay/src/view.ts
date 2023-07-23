//import { Controller } from './interfaces';
import { Chessground } from 'chessground';
import { h, VNode } from 'snabbdom';
import { makeConfig as makeCgConfig } from './chessground';
//import { getNow } from 'puz/util';
//import * as licon from 'common/licon';
//import { onInsert } from 'common/snabbdom';

export default function (ctrl: any): VNode {
  return h('div.local-play', renderPlay(ctrl));
}

function chessground(ctrl: any): VNode {
  return h('div.cg-wrap', {
    hook: {
      insert: vnode => ctrl.ground(Chessground(vnode.elm as HTMLElement, makeCgConfig(ctrl))),
    },
  });
}

function renderPlay(ctrl: any): VNode[] {
  return [
    h('div.puz-board.main-board', [chessground(ctrl), ctrl.promotion.view()]),
    h('div.puz-side', [
      renderStart(ctrl),
      h('div.puz-bots', [
        // ...
      ]),
      h('div.puz-side__table', [renderControls(ctrl)]),
    ]),
  ];
}

function renderControls(ctrl: any): VNode {
  ctrl;
  return h('div.puz-side__control', ['gah']);
}

function renderStart(ctrl: any): VNode {
  ctrl;
  return h('div.puz-side__top.puz-side__start', [
    h('div.puz-side__start__text', [h('strong', 'Play vs Bots'), h('span', 'gah')]),
  ]);
}
