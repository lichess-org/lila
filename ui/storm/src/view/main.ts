import config from '../config';
import renderClock from 'puz/view/clock';
import renderEnd from './end';
import StormCtrl from '../ctrl';
import { Chessground } from 'chessground';
import { h } from 'snabbdom';
import { makeCgOpts } from 'puz/run';
import { makeConfig as makeCgConfig } from 'puz/view/chessground';
import { onInsert } from 'puz/util';
import { playModifiers, renderCombo } from 'puz/view/util';
import { VNode } from 'snabbdom/vnode';

export default function (ctrl: StormCtrl): VNode {
  if (ctrl.vm.dupTab) return renderReload('This run was opened in another tab!');
  if (ctrl.vm.lateStart) return renderReload('This run has expired!');
  if (!ctrl.run.endAt)
    return h(
      'div.storm.storm-app.storm--play',
      {
        class: playModifiers(ctrl.run),
      },
      renderPlay(ctrl)
    );
  return h('main.storm.storm--end', renderEnd(ctrl));
}

const chessground = (ctrl: StormCtrl): VNode =>
  h('div.cg-wrap', {
    hook: {
      insert: vnode =>
        ctrl.ground(
          Chessground(
            vnode.elm as HTMLElement,
            makeCgConfig(makeCgOpts(ctrl.run, !ctrl.run.endAt), ctrl.pref, ctrl.userMove)
          )
        ),
      destroy: _ => ctrl.withGround(g => g.destroy()),
    },
  });

const renderBonus = (bonus: number) => `${bonus}s`;

const renderPlay = (ctrl: StormCtrl): VNode[] => [
  h('div.puz-board.main-board', [chessground(ctrl), ctrl.promotion.view()]),
  h('div.puz-side', [
    ctrl.run.clock.startAt ? renderSolved(ctrl) : renderStart(ctrl),
    renderClock(ctrl.run, ctrl.endNow, true),
    h('div.puz-side__table', [renderControls(ctrl), renderCombo(config, renderBonus)(ctrl.run)]),
  ]),
];

const renderSolved = (ctrl: StormCtrl): VNode =>
  h('div.puz-side__top.puz-side__solved', [h('div.puz-side__solved__text', ctrl.countWins())]);

const renderControls = (ctrl: StormCtrl): VNode =>
  h('div.puz-side__control', [
    h('a.puz-side__control__reload.button.button-empty', {
      attrs: {
        href: '/storm',
        'data-icon': 'B',
        title: ctrl.trans('newRun'),
      },
    }),
    h('a.puz-side__control__end.button.button-empty', {
      attrs: {
        'data-icon': 'b',
        title: ctrl.trans('endRun'),
      },
      hook: onInsert(el => el.addEventListener('click', ctrl.endNow)),
    }),
  ]);

const renderStart = (ctrl: StormCtrl) =>
  h(
    'div.puz-side__top.puz-side__start',
    h('div.puz-side__start__text', [h('strong', 'Puzzle Storm'), h('span', ctrl.trans('moveToStart'))])
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
