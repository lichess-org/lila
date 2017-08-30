import { h } from 'snabbdom';
import { bind, dataIcon } from '../util';

function renderVote(ctrl) {
  var data = ctrl.getData();
  if (!data.puzzle.enabled) return;
  return h('div.vote', [
    h('a', {
      attrs: {
        'data-icon': 'S',
        title: ctrl.trans.noarg('thisPuzzleIsCorrect')
      },
      class: { active: ctrl.vm.voted === true },
      hook: bind('click', () => ctrl.vote(true))
    }),
    h('span.count.hint--bottom', {
      attrs: {
        'data-hint': 'Popularity'
      }
    }, '' + Math.max(0, data.puzzle.vote)),
    h('a', {
      attrs: {
        'data-icon': 'R',
        title: ctrl.trans.noarg('thisPuzzleIsWrong')
      },
      class: { active: ctrl.vm.voted === false },
      hook: bind('click', () => ctrl.vote(false))
    })
  ]);
}

export default function(ctrl) {
  const data = ctrl.getData();
  const voteCall = !!data.user && ctrl.callToVote() && data.puzzle.enabled && data.voted === undefined;
  return h('div.feedback.after' + (voteCall ? '.call' : ''), [
    voteCall ? h('div.vote_call', [
      h('strong', ctrl.trans('wasThisPuzzleAnyGood')),
      h('br'),
      h('span', ctrl.trans('pleaseVotePuzzle'))
    ]) : (ctrl.thanks() ? h('div.vote_call',
      h('strong', ctrl.trans('thankYou'))
    ) : null),
    h('div.half.top', [
      ctrl.vm.lastFeedback === 'win' ? h('div.complete.feedback.win', h('div.player', [
        h('div.icon', '✓'),
        h('div.instruction', ctrl.trans.noarg('success'))
      ])) : h('div.complete', 'Puzzle complete!'),
      data.user ? renderVote(ctrl) : null
    ]),
    h('a.half.continue', {
      hook: bind('click', ctrl.nextPuzzle)
    }, [
      h('i', { attrs: dataIcon('G') }),
      ctrl.trans.noarg('continueTraining')
    ])
  ]);
}
