import config from '../config';
import renderClock from 'puz/view/clock';
import renderEnd from './end';
import StormCtrl from '../ctrl';
import { Chessground } from 'chessground';
import { h, VNode } from 'snabbdom';
import { makeCgOpts, povMessage } from 'puz/run';
import { makeConfig as makeCgConfig } from 'puz/view/chessground';
import { getNow } from 'puz/util';
import { playModifiers, renderCombo } from 'puz/view/util';
import { onInsert } from 'common/snabbdom';

export default function (ctrl: StormCtrl): VNode {
  if (ctrl.vm.dupTab) return renderReload(ctrl, 'thisRunWasOpenedInAnotherTab');
  if (ctrl.vm.lateStart) return renderReload(ctrl, 'thisRunHasExpired');
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
            makeCgConfig(makeCgOpts(ctrl.run, !ctrl.run.endAt, ctrl.flipped), ctrl.pref, ctrl.userMove)
          )
        ),
    },
  });

const renderBonus = (bonus: number) => `${bonus}s`;

const renderPlay = (ctrl: StormCtrl): VNode[] => {
  const run = ctrl.run;
  const malus = run.modifier.malus;
  const bonus = run.modifier.bonus;
  const now = getNow();
  return [
    h('div.puz-board.main-board', [chessground(ctrl), ctrl.promotion.view()]),
    h('div.puz-side', [
      run.clock.startAt ? renderSolved(ctrl) : renderStart(ctrl),
      h('div.puz-clock', [
        renderClock(run, ctrl.endNow, true),
        !!malus && malus.at > now - 900 ? h('div.puz-clock__malus', '-' + malus.seconds) : null,
        !!bonus && bonus.at > now - 900 ? h('div.puz-clock__bonus', '+' + bonus.seconds) : null,
        ...(run.clock.started() ? [] : [h('span.puz-clock__pov', ctrl.trans.noarg(povMessage(run)))]),
      ]),
      h('div.puz-side__table', [renderControls(ctrl), renderCombo(config, renderBonus)(run)]),
    ]),
  ];
};

const renderSolved = (ctrl: StormCtrl): VNode =>
  h('div.puz-side__top.puz-side__solved', [h('div.puz-side__solved__text', ctrl.countWins())]);

const renderControls = (ctrl: StormCtrl): VNode =>
  h('div.puz-side__control', [
    h('a.puz-side__control__flip.button', {
      class: {
        active: ctrl.flipped,
        'button-empty': !ctrl.flipped,
      },
      attrs: {
        'data-icon': '',
        title: ctrl.trans.noarg('flipBoard') + ' (Keyboard: f)',
      },
      hook: onInsert(el => el.addEventListener('click', ctrl.flip)),
    }),
    h('a.puz-side__control__reload.button.button-empty', {
      attrs: {
        href: '/storm',
        'data-icon': '',
        title: ctrl.trans('newRun'),
      },
    }),
    h('a.puz-side__control__end.button.button-empty', {
      attrs: {
        'data-icon': '',
        title: ctrl.trans('endRun'),
      },
      hook: onInsert(el => el.addEventListener('click', ctrl.endNow)),
    }),
  ]);

const renderStart = (ctrl: StormCtrl) =>
  h('div.puz-side__top.puz-side__start', [
    h('div.puz-side__start__text', [h('strong', 'Puzzle Storm'), h('span', ctrl.trans('moveToStart'))]),
  ]);

const renderReload = (ctrl: StormCtrl, msgKey: string) =>
  h('div.storm.storm--reload.box.box-pad', [
    h('i', { attrs: { 'data-icon': '' } }),
    h('p', ctrl.trans.noarg(msgKey)),
    h(
      'a.storm--dup__reload.button',
      {
        attrs: { href: '/storm' },
      },
      ctrl.trans.noarg('clickToReload')
    ),
  ]);
