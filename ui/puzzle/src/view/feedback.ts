import { bind, MaybeVNode } from 'common/snabbdom';
import { h, VNode } from 'snabbdom';
import afterView from './after';
import PuzzleCtrl from '../ctrl';

const viewSolution = (ctrl: PuzzleCtrl): VNode =>
  ctrl.streak
    ? h('div.view_solution.skip', { class: { show: !!ctrl.streak?.data.skip } }, [
        h(
          'a.button.button-empty',
          { hook: bind('click', ctrl.skip), attrs: { title: ctrl.trans.noarg('streakSkipExplanation') } },
          ctrl.trans.noarg('skip'),
        ),
      ])
    : h('div.view_solution', { class: { show: ctrl.canViewSolution() } }, [
        h(
          'a.button.button-empty',
          { hook: bind('click', ctrl.viewSolution) },
          ctrl.trans.noarg('viewTheSolution'),
        ),
      ]);

const initial = (ctrl: PuzzleCtrl): VNode =>
  h('div.puzzle__feedback.play', [
    h('div.player', [
      h('div.no-square', h('piece.king.' + ctrl.pov)),
      h('div.instruction', [
        h('strong', ctrl.trans.noarg('yourTurn')),
        h(
          'em',
          ctrl.trans.noarg(ctrl.pov === 'white' ? 'findTheBestMoveForWhite' : 'findTheBestMoveForBlack'),
        ),
      ]),
    ]),
    viewSolution(ctrl),
  ]);

const good = (ctrl: PuzzleCtrl): VNode =>
  h('div.puzzle__feedback.good', [
    h('div.player', [
      h('div.icon', '✓'),
      h('div.instruction', [
        h('strong', ctrl.trans.noarg('bestMove')),
        h('em', ctrl.trans.noarg('keepGoing')),
      ]),
    ]),
    viewSolution(ctrl),
  ]);

const fail = (ctrl: PuzzleCtrl): VNode =>
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

export default function (ctrl: PuzzleCtrl): MaybeVNode {
  if (ctrl.mode === 'view') return afterView(ctrl);
  switch (ctrl.lastFeedback) {
    case 'init':
      return initial(ctrl);
    case 'good':
      return good(ctrl);
    case 'fail':
      return fail(ctrl);
  }
  return;
}
