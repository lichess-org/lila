import { onInsert } from 'common/snabbdom';
import { i18n } from 'i18n';
import { makeSgOpts } from 'puz/run';
import renderClock from 'puz/view/clock';
import { makeConfig as makeSgConfig } from 'puz/view/shogiground';
import { playModifiers, renderCombo } from 'puz/view/util';
import { type VNode, h } from 'snabbdom';
import config from '../config';
import type StormCtrl from '../ctrl';
import renderEnd from './end';

export default function (ctrl: StormCtrl): VNode {
  if (ctrl.vm.dupTab) return renderReload('This run was opened in another tab!');
  if (ctrl.vm.lateStart) return renderReload('This run has expired!');
  if (!ctrl.run.endAt)
    return h(
      'div.storm.storm-app.storm--play',
      {
        class: playModifiers(ctrl.run),
      },
      renderPlay(ctrl),
    );
  return h('main.storm.storm--end', renderEnd(ctrl));
}

const shogigroundBoard = (ctrl: StormCtrl): VNode =>
  h('div.sg-wrap', {
    hook: {
      insert: vnode => {
        ctrl.shogiground.attach({ board: vnode.elm as HTMLElement });
        ctrl.shogiground.set(
          makeSgConfig(
            makeSgOpts(ctrl.run, !ctrl.run.endAt),
            ctrl.pref,
            ctrl.userMove,
            ctrl.userDrop,
          ),
        );
      },
    },
  });

const shogigroundHand = (ctrl: StormCtrl, pos: 'top' | 'bottom'): VNode => {
  return h(`div.sg-hand-wrap.hand-${pos}`, {
    hook: {
      insert: vnode => {
        ctrl.shogiground.attach({
          hands: {
            top: pos === 'top' ? (vnode.elm as HTMLElement) : undefined,
            bottom: pos === 'bottom' ? (vnode.elm as HTMLElement) : undefined,
          },
        });
      },
    },
  });
};

const renderBonus = (bonus: number) => `${bonus}s`;

const renderPlay = (ctrl: StormCtrl): VNode[] => [
  h('div.puz-board.main-board', shogigroundBoard(ctrl)),
  shogigroundHand(ctrl, 'top'),
  h('div.puz-side', [
    ctrl.run.clock.startAt ? renderSolved(ctrl) : renderStart(),
    renderClock(ctrl.run, ctrl.endNow, true),
    h('div.puz-side__table', [renderControls(ctrl), renderCombo(config, renderBonus)(ctrl.run)]),
  ]),
  shogigroundHand(ctrl, 'bottom'),
];

const renderSolved = (ctrl: StormCtrl): VNode =>
  h('div.puz-side__top.puz-side__solved', [h('div.puz-side__solved__text', ctrl.countWins())]);

const renderControls = (ctrl: StormCtrl): VNode =>
  h('div.puz-side__control', [
    h('a.puz-side__control__reload.button.button-empty', {
      attrs: {
        href: '/storm',
        'data-icon': 'B',
        title: i18n('storm:newRun'),
      },
    }),
    h('a.puz-side__control__end.button.button-empty', {
      attrs: {
        'data-icon': 'b',
        title: i18n('storm:endRun'),
      },
      hook: onInsert(el => el.addEventListener('click', ctrl.endNow)),
    }),
  ]);

const renderStart = () =>
  h(
    'div.puz-side__top.puz-side__start',
    h('div.puz-side__start__text', [
      h('strong', 'Tsume Storm'),
      h('span', i18n('storm:moveToStart')),
    ]),
  );

const renderReload = (msg: string) =>
  h('div.storm.storm--reload.box.box-pad', [
    h('i', { attrs: { 'data-icon': '.' } }),
    h('p', msg),
    h(
      'a.storm--dup__reload.button',
      {
        attrs: { href: '/storm' },
      },
      'Click to reload',
    ),
  ]);
