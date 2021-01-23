import chessground from './chessground';
import renderClock from './clock';
import StormCtrl from '../ctrl';
import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import renderEnd from "./end";
import { getNow } from '../util';

export default function(ctrl: StormCtrl): VNode {
  if (ctrl.vm.mode == 'play') return h('main.storm.storm--play', {
    class: playModifiers(ctrl)
  }, renderPlay(ctrl));
  return h('main.storm.storm--end', renderEnd(ctrl));
}

const playModifiers = (ctrl: StormCtrl) => {
  const now = getNow();
  return {
    'storm--mod-move': ctrl.vm.modifier.moveAt > now - 90
  };
}

const renderPlay = (ctrl: StormCtrl): VNode[] => [
  h('div.storm__board.main-board', [
    chessground(ctrl),
    ctrl.promotion.view()
  ]),
  h('div.storm__side', [
    renderCombo(ctrl),
    renderClock(ctrl),
    renderSolved(ctrl)
  ])
];

const renderCombo = (ctrl: StormCtrl): VNode =>
  h('div.storm__combo', [
    h('div.storm__combo__icon'),
    h('div.storm__combo__counter', [
      h('span.storm__combo__counter__value', [
        h('span', 'x'),
        h('strong', ctrl.vm.combo)
      ]),
      h('span.storm__combo__counter__combo', 'COMBO')
    ]),
    h('div.storm__combo__bars', [
      h('div.storm__combo__bar',
        h('div.storm__combo__bar__in', {
          attrs: {
            style: `width:${ctrl.comboPercent()}%`
          }
        })
      ),
      h('div.storm__combo__levels',
        [...Array(ctrl.comboLevel()).keys()].map(_ =>
          h('div.storm__combo__level')
        )
      )
    ])
  ]);

const renderSolved = (ctrl: StormCtrl): VNode =>
  h('div.storm__solved', [
    h('strong', ctrl.countWins()),
    'puzzles solved'
  ]);
