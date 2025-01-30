import { type MaybeVNode, bind } from 'common/snabbdom';
import { i18n, i18nFormatCapitalized } from 'i18n';
import { colorName } from 'shogi/color-name';
import { type VNode, h } from 'snabbdom';
import type { Controller } from '../interfaces';
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
        i18n('viewTheSolution'),
      ),
    ],
  );
}

function initial(ctrl: Controller): VNode {
  return h('div.puzzle__feedback.play', [
    h('div.player', [
      h('div.instruction', [
        h('strong', i18n('yourTurn')),
        h('em', i18nFormatCapitalized('puzzle:findTheBestMoveForX', colorName(ctrl.vm.pov, false))),
      ]),
    ]),
    viewSolution(ctrl),
  ]);
}

function good(ctrl: Controller): VNode {
  return h('div.puzzle__feedback.good', [
    h('div.player', [
      h('div.icon', { attrs: { 'data-icon': 'K' } }),
      h('div.instruction', [
        h('strong', i18n('puzzle:bestMove')),
        h('em', i18n('puzzle:keepGoing')),
      ]),
    ]),
    viewSolution(ctrl),
  ]);
}

function fail(ctrl: Controller): VNode {
  return h('div.puzzle__feedback.fail', [
    h('div.player', [
      h('div.icon', { attrs: { 'data-icon': 'L' } }),
      h('div.instruction', [
        h('strong', i18n('puzzle:notTheMove')),
        h('em', i18n('puzzle:trySomethingElse')),
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
