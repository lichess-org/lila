import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import afterView from './after';
import { bind, spinner } from '../util';
import { Controller, MaybeVNode } from '../interfaces';

function viewSolution(ctrl: Controller): VNode {
  return h('div.view_solution', {
    class: { show: ctrl.vm.canViewSolution }
  }, [
    h('a.button.button-empty', {
      hook: bind('click', ctrl.viewSolution)
    }, ctrl.trans.noarg('viewTheSolution'))
  ]);
}

function initial(ctrl: Controller): VNode {
  return h('div.puzzle__feedback.play', [
    h('div.player', [
      h('div.no-square', h('piece.king.' + ctrl.vm.pov)),
      h('div.instruction', [
        h('strong', ctrl.trans.noarg('yourTurn')),
        h('em', ctrl.trans.noarg(ctrl.vm.pov === 'white' ? 'findTheBestMoveForWhite' : 'findTheBestMoveForBlack'))
      ])
    ]),
    viewSolution(ctrl)
  ]);
}

function good(ctrl: Controller): VNode {
  return h('div.puzzle__feedback.good', [
    h('div.player', [
      h('div.icon', '✓'),
      h('div.instruction', [
        h('strong', ctrl.trans.noarg('bestMove')),
        h('em', ctrl.trans.noarg('keepGoing'))
      ])
    ]),
    viewSolution(ctrl)
  ]);
}

function retry(ctrl: Controller): VNode {
  return h('div.puzzle__feedback.retry', [
    h('div.player', [
      h('div.icon', '!'),
      h('div.instruction', [
        h('strong', ctrl.trans.noarg('goodMove')),
        h('em', ctrl.trans.noarg('butYouCanDoBetter'))
      ])
    ]),
    viewSolution(ctrl)
  ]);
}

function fail(ctrl: Controller): VNode {
  return h('div.puzzle__feedback.fail', [
    h('div.player', [
      h('div.icon', '✗'),
      h('div.instruction', [
        h('strong', ctrl.trans.noarg('puzzleFailed')),
        h('em', ctrl.trans.noarg('butYouCanKeepTrying'))
      ])
    ]),
    viewSolution(ctrl)
  ]);
}

function loading(): VNode {
  return h('div.puzzle__feedback.loading', spinner());
}

export default function(ctrl: Controller): MaybeVNode {
  if (ctrl.vm.loading) return loading();
  if (ctrl.vm.mode === 'view') return afterView(ctrl);
  if (ctrl.vm.lastFeedback === 'init') return initial(ctrl);
  if (ctrl.vm.lastFeedback === 'good') return good(ctrl);
  if (ctrl.vm.lastFeedback === 'retry') return retry(ctrl);
  if (ctrl.vm.lastFeedback === 'fail') return fail(ctrl);
  return;
}
