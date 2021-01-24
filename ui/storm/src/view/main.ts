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
  const malus = ctrl.vm.modifier.malus;
  const bonus = ctrl.vm.modifier.bonus;
  return {
    'storm--mod-move': ctrl.vm.modifier.moveAt > now - 90,
    'storm--mod-malus-quick': !!malus && malus.at > now - 90,
    'storm--mod-malus-slow': !!malus && malus.at > now - 950,
    'storm--mod-bonus-slow': !!bonus && bonus.at > now - 950
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

const renderCombo = (ctrl: StormCtrl): VNode => {
  const level = ctrl.comboLevel();
  return h('div.storm__combo', [
    h('div.storm__combo__icon'),
    h('div.storm__combo__counter', [
      h('span.storm__combo__counter__value', ctrl.vm.combo),
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
        [0, 1, 2, 3].map(l =>
          h('div.storm__combo__level', {
            class: {
              active: l < level
            }
          })
        )
      )
    ])
  ]);
}

const renderSolved = (ctrl: StormCtrl): VNode =>
  h('div.storm__solved', [
    h('div', [
      h('span.storm__solved__value', ctrl.countWins()),
      'puzzles solved'
    ]),
    h('div.alpha', [
      h('hr'),
      h('strong', 'This is an early preview'),
      h('p', "I'm not looking for feedback, yet.")
    ])
  ]);
