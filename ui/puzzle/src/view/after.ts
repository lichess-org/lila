import { bind, dataIcon } from '../util';
import { Controller } from '../interfaces';
import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

const renderVote = (ctrl: Controller): VNode => h('div.puzzle__vote', [
  ctrl.session.store().rounds.length ? null : h('div.puzzle__vote__help', [
    ctrl.trans.noarg('didYouLikeThisPuzzle'),
    h('br'),
    ctrl.trans.noarg('voteToLoadNextOne'),
  ]),
  h('div.puzzle__vote__buttons', [
    h('div.vote.vote-up', {
      hook: bind('click', () => ctrl.vote(true))
    }),
    h('div.vote.vote-down', {
      hook: bind('click', () => ctrl.vote(false))
    })
  ])
]);

const renderContinue = (ctrl: Controller) =>
  h('a.half.continue', {
    hook: bind('click', ctrl.nextPuzzle)
  }, [
    h('i', { attrs: dataIcon('G') }),
    ctrl.trans.noarg('continueTraining')
  ]);

export default function(ctrl: Controller): VNode {
  const data = ctrl.getData();
  return h('div.puzzle__feedback.after', [
    h('div.half.half-top', [
      ctrl.vm.lastFeedback === 'win' ? h('div.complete.feedback.win', h('div.player', [
        h('div.icon', '✓'),
        h('div.instruction', ctrl.trans.noarg('success'))
      ])) : h('div.complete', 'Puzzle complete!')
    ]),
    data.user ? renderVote(ctrl) : renderContinue(ctrl)
  ]);
}
