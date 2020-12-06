import { bind, dataIcon } from '../util';
import { Controller } from '../interfaces';
import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

const renderVote = (ctrl: Controller): VNode => h('div.puzzle__vote', [
  ctrl.session.isNew() ? h('div.puzzle__vote__help', [
    h('p', ctrl.trans.noarg('didYouLikeThisPuzzle')),
    h('p', ctrl.trans.noarg('voteToLoadNextOne'))
  ]) : null,
  h('div.puzzle__vote__buttons', {
    class: {
      enabled: !ctrl.vm.voteDisabled
    }
  }, [
    h('div.vote.vote-up', {
      hook: bind('click', () => ctrl.vote(true))
    }),
    h('div.vote.vote-down', {
      hook: bind('click', () => ctrl.vote(false))
    })
  ])
]);

const renderContinue = (ctrl: Controller) =>
  h('a.continue', {
    hook: bind('click', ctrl.nextPuzzle)
  }, [
    h('i', { attrs: dataIcon('G') }),
    ctrl.trans.noarg('continueTraining')
  ]);

export default function(ctrl: Controller): VNode {
  const data = ctrl.getData();
  return h('div.puzzle__feedback.after', [
    h('div.complete', ctrl.trans.noarg(ctrl.vm.lastFeedback == 'win' ? 'puzzleSuccess' : 'puzzleComplete')),
    data.user ? renderVote(ctrl) : renderContinue(ctrl)
  ]);
}
