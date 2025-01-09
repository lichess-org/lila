import { bind, dataIcon } from 'common/snabbdom';
import { VNode, h } from 'snabbdom';
import { Controller } from '../interfaces';
import { i18n } from 'i18n';

const renderVote = (ctrl: Controller): VNode =>
  h(
    'div.puzzle__vote',
    ctrl.autoNexting()
      ? []
      : [
          ctrl.session.isNew() && ctrl.getData().user?.provisional
            ? h('div.puzzle__vote__help', [
                h('p', i18n('puzzle:didYouLikeThisPuzzle')),
                h('p', i18n('puzzle:voteToLoadNextOne')),
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
    [h('i', { attrs: dataIcon('G') }), i18n('puzzle:continueTraining')]
  );

export default function (ctrl: Controller): VNode {
  const data = ctrl.getData();
  return h('div.puzzle__feedback.after', [
    h('div.complete', ctrl.vm.lastFeedback == 'win' ? i18n('puzzle:puzzleSuccess') : i18n('puzzle:puzzleComplete')),
    data.user ? renderVote(ctrl) : renderContinue(ctrl),
    h('div.puzzle__more', [
      h('a', {
        attrs: {
          'data-icon': '',
          href: `/analysis/${ctrl.vm.node.sfen.replace(/ /g, '_')}?color=${ctrl.vm.pov}#practice`,
          title: i18n('playWithTheMachine'),
        },
      }),
      ctrl.getData().user
        ? h(
            'a',
            {
              hook: bind('click', ctrl.nextPuzzle),
            },
            i18n('puzzle:continueTraining')
          )
        : undefined,
    ]),
  ]);
}
