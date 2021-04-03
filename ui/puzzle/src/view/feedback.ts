import afterView from './after';
import { bind } from '../util';
import { Controller, MaybeVNode } from '../interfaces';
import { h } from 'snabbdom';
import { VNode } from 'snabbdom';

const viewSolution = (ctrl: Controller): VNode =>
  ctrl.streak
    ? h(
        'div.view_solution.skip',
        {
          class: { show: !!ctrl.streak?.data.skip },
        },
        [
          h(
            'a.button.button-empty',
            {
              hook: bind('click', ctrl.skip),
              attrs: {
                title: ctrl.trans.noarg('streakSkipExplanation'),
              },
            },
            ctrl.trans.noarg('skip')
          ),
        ]
      )
    : h(
        'div.view_solution',
        {
          class: { show: ctrl.vm.canViewSolution },
        },
        [
          h(
            'a.button.button-empty',
            {
              hook: bind('click', ctrl.viewSolution),
            },
            ctrl.trans.noarg('viewTheSolution')
          ),
        ]
      );

const initial = (ctrl: Controller): VNode =>
  h('div.puzzle__feedback.play', [
    h('div.player', [
      h('div.no-square', h('piece.king.' + ctrl.vm.pov)),
      h('div.instruction', [
        h('strong', ctrl.trans.noarg('yourTurn')),
        h('em', ctrl.trans.noarg(ctrl.vm.pov === 'white' ? 'findTheBestMoveForWhite' : 'findTheBestMoveForBlack')),
      ]),
    ]),
    viewSolution(ctrl),
  ]);

const good = (ctrl: Controller): VNode =>
  h('div.puzzle__feedback.good', [
    h('div.player', [
      h('div.icon', '✓'),
      h('div.instruction', [h('strong', ctrl.trans.noarg('bestMove')), h('em', ctrl.trans.noarg('keepGoing'))]),
    ]),
    viewSolution(ctrl),
  ]);

const fail = (ctrl: Controller): VNode =>
  h('div.puzzle__feedback.fail', [
    h('div.player', [
      h('div.icon', '✗'),
      h('div.instruction', [
        h('strong', ctrl.trans.noarg('notTheMove')),
        h('em', ctrl.trans.noarg('trySomethingElse')),
      ]),
    ]),
    viewSolution(ctrl),
  ]);

export default function (ctrl: Controller): MaybeVNode {
  if (ctrl.vm.mode === 'view') return afterView(ctrl);
  switch (ctrl.vm.lastFeedback) {
    case 'init':
      return initial(ctrl);
    case 'good':
      return good(ctrl);
    case 'fail':
      return fail(ctrl);
  }
  return;
}
