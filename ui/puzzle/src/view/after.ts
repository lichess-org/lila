import * as licon from 'common/licon';
import { MaybeVNodes, bind, dataIcon, looseH as h } from 'common/snabbdom';
import { VNode } from 'snabbdom';
import * as router from 'common/router';
import PuzzleCtrl from '../ctrl';

const renderVote = (ctrl: PuzzleCtrl): VNode =>
  h(
    'div.puzzle__vote',
    {},
    !ctrl.autoNexting() && [
      ctrl.session.isNew() &&
        ctrl.data.user?.provisional &&
        h('div.puzzle__vote__help', [
          h('p', ctrl.trans.noarg('didYouLikeThisPuzzle')),
          h('p', ctrl.trans.noarg('voteToLoadNextOne')),
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
    ctrl.trans.noarg('continueTraining'),
  ]);

const renderStreak = (ctrl: PuzzleCtrl): MaybeVNodes => [
  h('div.complete', [
    h('span.game-over', 'GAME OVER'),
    h('span', ctrl.trans.vdom('yourStreakX', h('strong', `${ctrl.streak?.data.index ?? 0}`))),
  ]),
  h('a.continue', { attrs: { href: router.withLang('/streak') } }, [
    h('i', { attrs: dataIcon(licon.PlayTriangle) }),
    ctrl.trans('newStreak'),
  ]),
];

export default function (ctrl: PuzzleCtrl): VNode {
  const data = ctrl.data;
  const win = ctrl.lastFeedback == 'win';
  return h(
    'div.puzzle__feedback.after',
    ctrl.streak && !win
      ? renderStreak(ctrl)
      : [
          h('div.complete', ctrl.trans.noarg(win ? 'puzzleSuccess' : 'puzzleComplete')),
          data.user ? renderVote(ctrl) : renderContinue(ctrl),
          h('div.puzzle__more', [
            h('a', {
              attrs: {
                'data-icon': licon.Bullseye,
                href: `/analysis/${ctrl.node.fen.replace(/ /g, '_')}?color=${ctrl.pov}#practice`,
                title: ctrl.trans.noarg('playWithTheMachine'),
                target: '_blank',
                rel: 'noopener',
              },
            }),
            data.user &&
              !ctrl.autoNexting() &&
              h(
                'a',
                { hook: bind('click', ctrl.nextPuzzle) },
                ctrl.trans.noarg(ctrl.streak ? 'continueTheStreak' : 'continueTraining'),
              ),
          ]),
        ],
  );
}
