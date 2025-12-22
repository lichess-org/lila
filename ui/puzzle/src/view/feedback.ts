import { bind, requiresI18n, type MaybeVNode } from 'lib/view';
import { h, type VNode } from 'snabbdom';
import afterView from './after';
import type PuzzleCtrl from '../ctrl';

const viewSolution = (ctrl: PuzzleCtrl): VNode =>
  ctrl.streak
    ? h('div.view_solution.skip', { class: { show: !!ctrl.streak?.data.skip } }, [
        requiresI18n('storm', ctrl.redraw, cat =>
          h(
            'a.button.button-empty',
            { hook: bind('click', ctrl.skip), attrs: { title: i18n.puzzle.streakSkipExplanation } },
            cat.skip,
          ),
        ),
      ])
    : h('div.view_solution', { class: { show: ctrl.canViewSolution() } }, [
        ctrl.mode !== 'view'
          ? h(
              'a.button' + (ctrl.showHint() ? '' : '.button-empty'),
              { hook: bind('click', ctrl.toggleHint) },
              i18n.site.getAHint,
            )
          : undefined,
        h('a.button.button-empty', { hook: bind('click', ctrl.viewSolution) }, i18n.site.viewTheSolution),
      ]);

const initial = (ctrl: PuzzleCtrl): VNode =>
  h('div.puzzle__feedback.play', [
    h('div.player', [
      h('div.no-square', h('piece.king.' + ctrl.pov)),
      h('div.instruction', [
        h('strong', i18n.site.yourTurn),
        h('em', i18n.puzzle[ctrl.pov === 'white' ? 'findTheBestMoveForWhite' : 'findTheBestMoveForBlack']),
      ]),
    ]),
    viewSolution(ctrl),
  ]);

const good = (ctrl: PuzzleCtrl): VNode =>
  h('div.puzzle__feedback.good', [
    h('div.player', [
      h('div.icon', '✓'),
      h('div.instruction', [h('strong', i18n.puzzle.bestMove), h('em', i18n.puzzle.keepGoing)]),
    ]),
    viewSolution(ctrl),
  ]);

const fail = (ctrl: PuzzleCtrl): VNode =>
  h('div.puzzle__feedback.fail', [
    h('div.player', [
      h('div.icon', '✗'),
      h('div.instruction', [h('strong', i18n.puzzle.notTheMove), h('em', i18n.puzzle.trySomethingElse)]),
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
