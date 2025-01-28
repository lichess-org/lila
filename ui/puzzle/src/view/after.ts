import * as licon from 'common/licon';
import { type VNode, type MaybeVNodes, bind, dataIcon, looseH as h } from 'common/snabbdom';
import * as router from 'common/router';
import type PuzzleCtrl from '../ctrl';

const renderVote = (ctrl: PuzzleCtrl): VNode =>
  h(
    'div.puzzle__vote',
    {},
    !ctrl.autoNexting() && [
      ctrl.session.isNew() &&
        ctrl.data.user?.provisional &&
        h('div.puzzle__vote__help', [
          h('p', i18n.puzzle.didYouLikeThisPuzzle),
          h('p', i18n.puzzle.voteToLoadNextOne),
        ]),
      h('div.puzzle__vote__buttons', { class: { enabled: !ctrl.voteDisabled } }, [
        h('div.vote.vote-up', { hook: bind('click', () => ctrl.vote(true)) }),
        h('div.vote.vote-down', { hook: bind('click', () => ctrl.vote(false)) }),
      ]),
    ],
  );

const renderContinue = (ctrl: PuzzleCtrl) =>
  h('a.continue', { hook: bind('click', ctrl.nextPuzzle) }, [
    h('i', { attrs: dataIcon(licon.PlayTriangle) }),
    i18n.puzzle.continueTraining,
  ]);

const renderStreak = (ctrl: PuzzleCtrl): MaybeVNodes => [
  h('div.complete', [
    h('span.game-over', 'GAME OVER'),
    h('span', i18n.puzzle.yourStreakX.asArray(h('strong', `${ctrl.streak?.data.index ?? 0}`))),
  ]),
  h('a.continue', { attrs: { href: router.withLang('/streak') } }, [
    h('i', { attrs: dataIcon(licon.PlayTriangle) }),
    i18n.puzzle.newStreak,
  ]),
];

export default function (ctrl: PuzzleCtrl): VNode {
  const data = ctrl.data;
  const win = ctrl.lastFeedback === 'win';
  return h(
    'div.puzzle__feedback.after',
    ctrl.streak && !win
      ? renderStreak(ctrl)
      : [
          h('div.complete', i18n.puzzle[win ? 'puzzleSuccess' : 'puzzleComplete']),
          data.user ? renderVote(ctrl) : renderContinue(ctrl),
          h('div.puzzle__more', [
            h('a', {
              attrs: {
                'data-icon': licon.Bullseye,
                href: `/analysis/${ctrl.node.fen.replace(/ /g, '_')}?color=${ctrl.pov}#practice`,
                title: i18n.site.playWithTheMachine,
                target: '_blank',
              },
            }),
            data.user &&
              !ctrl.autoNexting() &&
              h(
                'a',
                { hook: bind('click', ctrl.nextPuzzle) },
                i18n.puzzle[ctrl.streak ? 'continueTheStreak' : 'continueTraining'],
              ),
          ]),
        ],
  );
}
