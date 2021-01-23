import chessground from './chessground';
import renderClock from './clock';
import StormCtrl from '../ctrl';
import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';

export default function(ctrl: StormCtrl): VNode {
  return h('main.storm.storm--' + ctrl.vm.mode,
    ctrl.vm.mode == 'play' ? renderPlay(ctrl) : renderEnd(ctrl)
  );
}

const renderPlay = (ctrl: StormCtrl): VNode[] => [
  h('div.storm__board.main-board', [
    chessground(ctrl),
    ctrl.promotion.view()
  ]),
  h('div.storm__side', [
    renderClock(ctrl)
  ])
];

const renderEnd = (ctrl: StormCtrl): VNode[] => [
  h('div.storm__summary', 'Game summary'),
  h('div.storm__puzzles', 'Puzzles played')
];
