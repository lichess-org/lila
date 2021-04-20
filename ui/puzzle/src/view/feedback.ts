import afterView from './after';
import { bind } from '../util';
import { Controller, MaybeVNode } from '../interfaces';
import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

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

function tsumeHint(ctrl: Controller, prefix: string = " - "): string | undefined {
  const tl = ctrl.tsumeLength() > 0 ? ((ctrl.tsumeLength() - 1) | 1) : 0;
  switch (tl) {
    case 0:
      return undefined;
    case 1:
    case 3:
    case 5:
    case 7:
        return prefix + ctrl.trans.noarg('mateIn' + tl);
    default:
      return prefix + ctrl.trans.noarg('mateIn9');
  }
}

function initial(ctrl: Controller): VNode {
  return h('div.puzzle__feedback.play', [
    h('div.player', [
      h('div.no-square', h('div.color-piece.' + ctrl.vm.pov)),
      h('div.instruction', [
        h('strong', [ctrl.trans.noarg('yourTurn'), tsumeHint(ctrl)]),
        h('em', ctrl.trans.noarg(ctrl.vm.pov === 'sente' ? 'findTheBestMoveForBlack' : 'findTheBestMoveForWhite')),
    ]),
    ]),
    viewSolution(ctrl),
  ]);
}

function good(ctrl: Controller): VNode {
  return h('div.puzzle__feedback.good', [
    h('div.player', [
      h('div.icon', '✓'),
      h('div.instruction', [
        h('strong', ctrl.trans.noarg('bestMove')),
        h('em', [
          ctrl.trans.noarg('keepGoing'),
          tsumeHint(ctrl, " "),
        ]),
      ]),
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
        h('em', [
          ctrl.trans.noarg('trySomethingElse'),
          tsumeHint(ctrl, " "),
        ]),
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
