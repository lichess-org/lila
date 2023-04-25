import { MaybeVNode, bind } from 'common/snabbdom';
import { transWithColorName } from 'common/colorName';
import { VNode, h } from 'snabbdom';
import { Controller } from '../interfaces';
import afterView from './after';

function viewSolution(ctrl: Controller): VNode {
  return h(
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
}

function initial(ctrl: Controller): VNode {
  return h('div.puzzle__feedback.play', [
    h('div.player', [
      h('div.instruction', [
        h('strong', ctrl.trans.noarg('yourTurn')),
        h('em', transWithColorName(ctrl.trans, 'findTheBestMoveForX', ctrl.vm.pov, false)),
      ]),
    ]),
    viewSolution(ctrl),
  ]);
}

function good(ctrl: Controller): VNode {
  return h('div.puzzle__feedback.good', [
    h('div.player', [
      h('div.icon', '✓'),
      h('div.instruction', [h('strong', ctrl.trans.noarg('bestMove')), h('em', ctrl.trans.noarg('keepGoing'))]),
    ]),
    viewSolution(ctrl),
  ]);
}

function fail(ctrl: Controller): VNode {
  return h('div.puzzle__feedback.fail', [
    h('div.player', [
      h('div.icon', '✗'),
      h('div.instruction', [
        h('strong', ctrl.trans.noarg('notTheMove')),
        h('em', ctrl.trans.noarg('trySomethingElse')),
      ]),
    ]),
    viewSolution(ctrl),
  ]);
}

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
