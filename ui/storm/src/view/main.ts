import chessground from './chessground';
import config from '../config';
import renderClock from './clock';
import renderEnd from "./end";
import StormCtrl from '../ctrl';
import { getNow, onInsert } from '../util';
import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';

export default function(ctrl: StormCtrl): VNode {
  if (ctrl.vm.dupTab) return renderReload('This run was opened in another tab!');
  if (ctrl.vm.lateStart) return renderReload('This run has expired!');
  if (!ctrl.vm.run.endAt) return h('div.storm.storm-app.storm--play', {
    class: playModifiers(ctrl)
  }, renderPlay(ctrl));
  return h('main.storm.storm--end', renderEnd(ctrl));
}

const playModifiers = (ctrl: StormCtrl) => {
  const now = getNow();
  const malus = ctrl.vm.modifier.malus;
  const bonus = ctrl.vm.modifier.bonus;
  return {
    'storm--mod-puzzle': !!ctrl.vm.puzzleStartAt && ctrl.vm.puzzleStartAt > now - 90,
    'storm--mod-move': ctrl.vm.modifier.moveAt > now - 90,
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
    ctrl.vm.run.startAt ? renderSolved(ctrl) : renderStart(ctrl),
    renderClock(ctrl),
    h('div.storm__table', [
      renderControls(ctrl),
      renderCombo(ctrl)
    ])
  ])
];

const renderControls = (ctrl: StormCtrl): VNode =>
  h('div.storm__control', [
    h('a.storm__control__reload.button.button-empty', {
      attrs: {
        href: '/storm',
        'data-icon': 'B',
        title: ctrl.trans('newRun')
      }
    }),
    h('a.storm__control__end.button.button-empty', {
      attrs: {
        'data-icon': 'b',
        title: ctrl.trans('endRun'),
      },
      hook: onInsert(el => el.addEventListener('click', ctrl.endNow))
    })
  ]);

const renderCombo = (ctrl: StormCtrl): VNode => {
  const level = ctrl.comboLevel();
  return h('div.storm__combo', [
    h('div.storm__combo__counter', [
      h('span.storm__combo__counter__value', ctrl.vm.combo),
      h('span.storm__combo__counter__combo', 'COMBO')
    ]),
    h('div.storm__combo__bars', [
      h('div.storm__combo__bar', [
        h('div.storm__combo__bar__in', {
          attrs: { style: `width:${ctrl.comboPercent()}%` }
        }),
        h('div.storm__combo__bar__in-full')
      ]),
      h('div.storm__combo__levels',
        [0, 1, 2, 3].map(l =>
          h('div.storm__combo__level', {
            class: {
              active: l < level
            }
          }, h('span', `${config.combo.levels[l + 1][1]}s`))
        )
      )
    ])
  ]);
}

const renderSolved = (ctrl: StormCtrl): VNode =>
  h('div.storm__top.storm__solved', [
    h('div.storm__solved__text', ctrl.countWins())
  ]);

const renderStart = (ctrl: StormCtrl) =>
  h('div.storm__top.storm__start',
    h('div.storm__start__text', [
      h('strong', 'Puzzle Storm'),
      h('span', ctrl.trans('moveToStart'))
    ])
  );

const renderReload = (msg: string) =>
  h('div.storm.storm--reload.box.box-pad', [
    h('i', { attrs: { 'data-icon': '~' } }),
    h('p', msg),
    h('a.storm--dup__reload.button', {
      attrs: { href: '/storm' }
    }, 'Click to reload')
  ]);
