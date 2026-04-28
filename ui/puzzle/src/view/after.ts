import * as licon from 'lib/licon';
import { type VNode, type MaybeVNodes, bind, dataIcon, hl } from 'lib/view';

import type PuzzleCtrl from '../ctrl';

const renderVote = (ctrl: PuzzleCtrl): VNode =>
  hl(
    'div.puzzle__vote',
    {},
    !ctrl.autoNexting() && [
      ctrl.session.isNew() &&
        ctrl.data.user?.provisional &&
        hl('div.puzzle__vote__help', i18n.puzzle.didYouLikeThisPuzzle),
      hl('div.puzzle__vote__buttons', [
        hl('button.button.button-empty.vote-up', {
          class: { active: ctrl.voted === true },
          attrs: { title: i18n.puzzle.upVote },
          hook: bind('click', () => ctrl.vote(true)),
        }),
        hl('button.button.button-empty.vote-down', {
          class: { active: ctrl.voted === false },
          attrs: { title: i18n.puzzle.downVote },
          hook: bind('click', () => ctrl.vote(false)),
        }),
      ]),
    ],
  );

const renderStreak = (ctrl: PuzzleCtrl): MaybeVNodes => [
  hl('div.complete', [
    hl('span.game-over', 'GAME OVER'),
    hl('span', i18n.puzzle.yourStreakX.asArray(hl('strong', `${ctrl.streak?.data.index ?? 0}`))),
  ]),
  hl('a.continue', { attrs: { href: ctrl.routerWithLang('/streak') } }, [
    hl('i', { attrs: dataIcon(licon.PlayTriangle) }),
    i18n.puzzle.newStreak,
  ]),
];

export default function (ctrl: PuzzleCtrl): VNode {
  const data = ctrl.data;
  const win = ctrl.lastFeedback === 'win';
  const canPlayComputer = !ctrl.node.san?.includes('#');
  return hl(
    'div.puzzle__feedback.after',
    ctrl.streak && !win
      ? renderStreak(ctrl)
      : [
          hl('div.complete', i18n.puzzle[win ? 'puzzleSuccess' : 'puzzleComplete']),
          hl('button.continue', { hook: bind('click', ctrl.nextPuzzle) }, [
            hl('i', { attrs: dataIcon(licon.PlayTriangle) }),
            i18n.puzzle[ctrl.streak ? 'continueTheStreak' : 'continueTraining'],
          ]),
          hl('div.puzzle__more', [
            canPlayComputer
              ? hl('a.practice.button.button-empty', {
                  attrs: {
                    'data-icon': licon.Bullseye,
                    href: `/analysis/${ctrl.node.fen.replace(/ /g, '_')}?color=${ctrl.pov}#practice`,
                    title: i18n.site.playAgainstComputer,
                    target: '_blank',
                  },
                })
              : hl('a'),
            data.user ? renderVote(ctrl) : undefined,
          ]),
        ],
  );
}
