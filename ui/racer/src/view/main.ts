import { Chessground } from 'chessground';
import { makeConfig as makeCgConfig } from 'puz/view/chessground';
import renderClock from 'puz/view/clock';
import RacerCtrl from '../ctrl';
import { onInsert } from 'puz/util';
import { playModifiers, renderCombo } from 'puz/view/util';
import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { makeCgOpts } from 'puz/run';

export default function (ctrl: RacerCtrl): VNode {
  return h(
    'div.racer.racer-app.racer--play',
    {
      class: playModifiers(ctrl.run),
    },
    renderPlay(ctrl)
  );
}

const chessground = (ctrl: RacerCtrl): VNode =>
  h('div.cg-wrap', {
    hook: {
      insert: vnode =>
        ctrl.ground(
          Chessground(vnode.elm as HTMLElement, makeCgConfig(makeCgOpts(ctrl.run), ctrl.pref, ctrl.userMove))
        ),
      destroy: _ => ctrl.withGround(g => g.destroy()),
    },
  });

const renderPlay = (ctrl: RacerCtrl): VNode[] => [
  h('div.storm__board.main-board', [chessground(ctrl), ctrl.promotion.view()]),
  h('div.storm__side', [
    ctrl.run.startAt ? renderSolved(ctrl) : renderStart(ctrl),
    renderClock(ctrl.run, ctrl.endNow),
    h('div.storm__table', [renderControls(ctrl), renderCombo(ctrl.run)]),
  ]),
];

const renderControls = (ctrl: RacerCtrl): VNode =>
  h('div.storm__control', [
    h('a.storm__control__reload.button.button-empty', {
      attrs: {
        href: '/storm',
        'data-icon': 'B',
        title: ctrl.trans('newRun'),
      },
    }),
    h('a.storm__control__end.button.button-empty', {
      attrs: {
        'data-icon': 'b',
        title: ctrl.trans('endRun'),
      },
      hook: onInsert(el => el.addEventListener('click', ctrl.endNow)),
    }),
  ]);

const renderSolved = (ctrl: RacerCtrl): VNode =>
  h('div.storm__top.storm__solved', [h('div.storm__solved__text', ctrl.countWins())]);

const renderStart = (ctrl: RacerCtrl) =>
  h(
    'div.storm__top.storm__start',
    h('div.storm__start__text', [h('strong', 'Puzzle Storm'), h('span', ctrl.trans('moveToStart'))])
  );

const renderReload = (msg: string) =>
  h('div.storm.storm--reload.box.box-pad', [
    h('i', { attrs: { 'data-icon': '~' } }),
    h('p', msg),
    h(
      'a.storm--dup__reload.button',
      {
        attrs: { href: '/storm' },
      },
      'Click to reload'
    ),
  ]);
