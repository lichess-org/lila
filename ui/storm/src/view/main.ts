import { Chessground } from 'chessground';
import { makeConfig as makeCgConfig } from 'puz/view/chessground';
import config from 'puz/config';
import renderClock from './clock';
import renderEnd from './end';
import StormCtrl from '../ctrl';
import { getNow, onInsert } from 'puz/util';
import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { makeCgOpts } from 'puz/run';

export default function (ctrl: StormCtrl): VNode {
  if (ctrl.vm.dupTab) return renderReload('This run was opened in another tab!');
  if (ctrl.vm.lateStart) return renderReload('This run has expired!');
  if (!ctrl.run.endAt)
    return h(
      'div.storm.storm-app.storm--play',
      {
        class: playModifiers(ctrl),
      },
      renderPlay(ctrl)
    );
  return h('main.storm.storm--end', renderEnd(ctrl));
}

const playModifiers = (ctrl: StormCtrl) => {
  const now = getNow();
  const malus = ctrl.run.modifier.malus;
  const bonus = ctrl.run.modifier.bonus;
  return {
    'storm--mod-puzzle': ctrl.run.current.startAt > now - 90,
    'storm--mod-move': ctrl.run.modifier.moveAt > now - 90,
    'storm--mod-malus-slow': !!malus && malus.at > now - 950,
    'storm--mod-bonus-slow': !!bonus && bonus.at > now - 950,
  };
};

const chessground = (ctrl: StormCtrl): VNode =>
  h('div.cg-wrap', {
    hook: {
      insert: vnode =>
        ctrl.ground(
          Chessground(vnode.elm as HTMLElement, makeCgConfig(makeCgOpts(ctrl.run), ctrl.pref, ctrl.userMove))
        ),
      destroy: _ => ctrl.withGround(g => g.destroy()),
    },
  });

const renderPlay = (ctrl: StormCtrl): VNode[] => [
  h('div.storm__board.main-board', [chessground(ctrl), ctrl.promotion.view()]),
  h('div.storm__side', [
    ctrl.run.startAt ? renderSolved(ctrl) : renderStart(ctrl),
    renderClock(ctrl),
    h('div.storm__table', [renderControls(ctrl), renderCombo(ctrl)]),
  ]),
];

const renderControls = (ctrl: StormCtrl): VNode =>
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

const renderCombo = (ctrl: StormCtrl): VNode => {
  const level = ctrl.run.combo.level();
  return h('div.storm__combo', [
    h('div.storm__combo__counter', [
      h('span.storm__combo__counter__value', ctrl.run.combo),
      h('span.storm__combo__counter__combo', 'COMBO'),
    ]),
    h('div.storm__combo__bars', [
      h('div.storm__combo__bar', [
        h('div.storm__combo__bar__in', {
          attrs: { style: `width:${ctrl.run.combo.percent()}%` },
        }),
        h('div.storm__combo__bar__in-full'),
      ]),
      h(
        'div.storm__combo__levels',
        [0, 1, 2, 3].map(l =>
          h(
            'div.storm__combo__level',
            {
              class: {
                active: l < level,
              },
            },
            h('span', `${config.combo.levels[l + 1][1]}s`)
          )
        )
      ),
    ]),
  ]);
};

const renderSolved = (ctrl: StormCtrl): VNode =>
  h('div.storm__top.storm__solved', [h('div.storm__solved__text', ctrl.countWins())]);

const renderStart = (ctrl: StormCtrl) =>
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
