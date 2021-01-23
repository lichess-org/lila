import chessground from './chessground';
import renderClock from './clock';
import StormCtrl from '../ctrl';
import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import renderEnd from "./end";

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
    renderCombo(ctrl),
    renderClock(ctrl)
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
    h('div.storm__combo__bar',
      h('div.storm__combo__bar__in', {
        attrs: {
          style: `width:${ctrl.comboPercent()}%`
        }
      }, ctrl.comboLevel())
    )
  ]);
