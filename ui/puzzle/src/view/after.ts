import { bind, dataIcon } from '../util';
import { Controller, MaybeVNodes } from '../interfaces';
import { h, VNode } from 'snabbdom';

const renderVote = (ctrl: Controller): VNode =>
  h(
    'div.puzzle__vote',
    ctrl.autoNexting()
      ? []
      : [
          ctrl.session.isNew() && ctrl.getData().user?.provisional
            ? h('div.puzzle__vote__help', [
                h('p', ctrl.trans.noarg('didYouLikeThisPuzzle')),
                h('p', ctrl.trans.noarg('voteToLoadNextOne')),
              ])
            : null,
          h(
            'div.puzzle__vote__buttons',
            {
              class: {
                enabled: !ctrl.vm.voteDisabled,
              },
            },
            [
              h('div.vote.vote-up', {
                hook: bind('click', () => ctrl.vote(true)),
              }),
              h('div.vote.vote-down', {
                hook: bind('click', () => ctrl.vote(false)),
              }),
            ]
          ),
        ]
  );

const renderContinue = (ctrl: Controller) =>
  h(
    'a.continue',
    {
      hook: bind('click', ctrl.nextPuzzle),
    },
    [h('i', { attrs: dataIcon('') }), ctrl.trans.noarg('continueTraining')]
  );

const renderStreak = (ctrl: Controller): MaybeVNodes => [
  h('div.complete', [
    h('span.game-over', 'GAME OVER'),
    h('span', ctrl.trans.vdom('yourStreakX', h('strong', ctrl.streak?.data.index))),
  ]),
  h(
    'a.continue',
    {
      attrs: { href: '/streak' },
    },
    [h('i', { attrs: dataIcon('') }), ctrl.trans('newStreak')]
  ),
];

export default function (ctrl: Controller): VNode {
  const data = ctrl.getData();
  const win = ctrl.vm.lastFeedback == 'win';
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
                'data-icon': '',
                href: `/analysis/${ctrl.vm.node.fen.replace(/ /g, '_')}?color=${ctrl.vm.pov}#practice`,
                title: ctrl.trans.noarg('playWithTheMachine'),
                target: '_blank',
                rel: 'noopener',
              },
            }),
            data.user
              ? h(
                  'a',
                  {
                    hook: bind('click', ctrl.nextPuzzle),
                  },
                  ctrl.trans.noarg(ctrl.streak ? 'continueTheStreak' : 'continueTraining')
                )
              : undefined,
          ]),
        ]
  );
}
