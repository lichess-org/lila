import * as licon from 'lib/licon';
import { type VNode, type MaybeVNodes, bind, dataIcon, hl } from 'lib/snabbdom';
import type PuzzleCtrl from '../ctrl';

const renderVote = (ctrl: PuzzleCtrl): VNode =>
  hl(
    'div.puzzle__vote',
    {},
    !ctrl.autoNexting() && [
      ctrl.session.isNew() &&
        ctrl.data.user?.provisional &&
        hl('div.puzzle__vote__help', [
          hl('p', i18n.puzzle.didYouLikeThisPuzzle),
          hl('p', i18n.puzzle.voteToLoadNextOne),
        ]),
      hl('div.puzzle__vote__buttons', { class: { enabled: !ctrl.voteDisabled } }, [
        hl('div.vote.vote-up', { hook: bind('click', () => ctrl.vote(true)) }),
        hl('div.vote.vote-down', { hook: bind('click', () => ctrl.vote(false)) }),
      ]),
    ],
  );

const renderContinue = (ctrl: PuzzleCtrl) =>
  hl('a.continue', { hook: bind('click', ctrl.nextPuzzle) }, [
    hl('i', { attrs: dataIcon(licon.PlayTriangle) }),
    i18n.puzzle.continueTraining,
  ]);

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
  const canContinue = !ctrl.node.san?.includes('#');
  return hl(
    'div.puzzle__feedback.after',
    ctrl.streak && !win
      ? renderStreak(ctrl)
      : [
          hl('div.complete', i18n.puzzle[win ? 'puzzleSuccess' : 'puzzleComplete']),
          data.user ? renderVote(ctrl) : renderContinue(ctrl),
          hl('div.puzzle__more', [
            canContinue
              ? hl('a', {
                  attrs: {
                    'data-icon': licon.Bullseye,
                    href: `/analysis/${ctrl.node.fen.replace(/ /g, '_')}?color=${ctrl.pov}#practice`,
                    title: i18n.site.playWithTheMachine,
                    target: '_blank',
                  },
                })
              : hl('a'),
            data.user &&
              !ctrl.autoNexting() &&
              hl(
                'a',
                { hook: bind('click', ctrl.nextPuzzle) },
                i18n.puzzle[ctrl.streak ? 'continueTheStreak' : 'continueTraining'],
              ),
          ]),
        ],
  );
}
