import { licon } from 'lib/licon';
import { type VNode, bind, icon, div, button, span, strong, a, type MaybeVNode } from 'lib/view';

import type PuzzleCtrl from '../ctrl';

const renderVote = (ctrl: PuzzleCtrl): MaybeVNode => {
  if (!ctrl.data.user) return null;
  if (ctrl.autoNexting()) return div('.puzzle__vote');

  return div('.puzzle__vote', [
    ctrl.session.isNew() && ctrl.data.user?.provisional
      ? div('.puzzle__vote__help', i18n.puzzle.didYouLikeThisPuzzle)
      : null,
    div('.puzzle__vote__buttons', [
      button('.button.button-empty.vote-up', {
        class: { active: ctrl.voted === true },
        title: i18n.puzzle.upVote,
        hook: bind('click', () => ctrl.vote(true)),
      }),
      button('.button.button-empty.vote-down', {
        class: { active: ctrl.voted === false },
        title: i18n.puzzle.downVote,
        hook: bind('click', () => ctrl.vote(false)),
      }),
    ]),
  ]);
};

const renderStreak = (ctrl: PuzzleCtrl): VNode[] => [
  div('.complete', [
    span('.game-over', i18n.site.gameOver),
    span(i18n.puzzle.yourStreakX.asArray(strong(ctrl.streak?.data.index ?? 0))),
  ]),
  a(ctrl.routerWithLang('/streak'))('.continue', [icon(licon.PlayTriangle)(), i18n.puzzle.newStreak]),
];

export default function (ctrl: PuzzleCtrl): VNode {
  const win = ctrl.lastFeedback === 'win';
  const canPlayComputer = !ctrl.node.san?.includes('#');
  return div(
    '.puzzle__feedback.after',
    ctrl.streak && !win
      ? renderStreak(ctrl)
      : [
          div('.complete', i18n.puzzle[win ? 'puzzleSuccess' : 'puzzleComplete']),
          button('.continue', { hook: bind('click', ctrl.nextPuzzle) }, [
            icon(licon.PlayTriangle)(),
            i18n.puzzle[ctrl.streak ? 'continueTheStreak' : 'continueTraining'],
          ]),
          div('.puzzle__more', [
            canPlayComputer
              ? a(`/analysis/${ctrl.node.fen.replace(/ /g, '_')}?color=${ctrl.pov}#practice`)(
                  '.practice.button.button-empty',
                  {
                    'data-icon': licon.Bullseye,
                    title: i18n.site.playAgainstComputer,
                    target: '_blank',
                  },
                )
              : null,
            renderVote(ctrl),
          ]),
        ],
  );
}
