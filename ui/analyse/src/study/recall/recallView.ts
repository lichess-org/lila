import { h, type VNode } from 'snabbdom';

import * as licon from 'lib/licon';

import type RecallCtrl from './recallCtrl';

export function render(ctrl: RecallCtrl): VNode {
  const feedback = ctrl.feedback();
  const text =
    feedback === 'right'
      ? 'Correct move.'
      : feedback === 'wrong'
        ? 'Wrong move. Try again.'
        : 'Play the stored move.';
  return h('div.gamebook.recall-practice', [
    h('div.comment', [
      h('div.gamebook__meta', [
        h('i', { attrs: { 'data-icon': licon.Book } }),
        h('strong', i18n.study.recall),
      ]),
      h('p', text),
      h('p', 'Notation is hidden. Your moves are checked against the study line and are not recorded.'),
    ]),
  ]);
}

export function underboard(ctrl: RecallCtrl): VNode[] {
  const feedback = ctrl.feedback();
  return [
    h('div.feedback.ongoing', [
      h(
        'div.goal',
        feedback === 'right'
          ? 'Correct.'
          : feedback === 'wrong'
            ? 'Wrong move. Try again.'
            : 'Play the next move from memory.',
      ),
    ]),
  ];
}
