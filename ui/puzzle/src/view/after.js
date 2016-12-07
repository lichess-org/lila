var m = require('mithril');

// function voteFunction(ctrl, v) {
//   return function() {
//     ctrl.hasEverVoted.set(1);
//     xhr.vote(ctrl, v);
//   };
// }

// function renderVote(ctrl) {
//   return m('div.upvote' + (ctrl.data.round ? '.enabled' : ''), [
//     m('a[data-icon=S]', {
//       title: ctrl.trans('thisPuzzleIsCorrect'),
//       class: ctrl.data.voted === true ? ' active' : '',
//       onclick: voteFunction(ctrl, 1)
//     }),
//     m('span.count.hint--bottom[data-hint=Popularity]', ctrl.data.puzzle.vote),
//     m('a[data-icon=R]', {
//       title: ctrl.trans('thisPuzzleIsWrong'),
//       class: ctrl.data.voted === false ? ' active' : '',
//       onclick: voteFunction(ctrl, 0)
//     })
//   ]);
// }

module.exports = function(ctrl) {
  var data = ctrl.getData();
  return m('div.feedback.after', [
    // (!ctrl.hasEverVoted.get() && ctrl.data.puzzle.enabled && ctrl.data.voted === null) ? m('div.please_vote', [
    //   m('p.first', [
    //     m('strong', ctrl.trans('wasThisPuzzleAnyGood')),
    //     m('br'),
    //     m('span', ctrl.trans('pleaseVotePuzzle'))
    //   ]),
    //   m('p.then',
    //     m('strong', ctrl.trans('thankYou'))
    //   )
    // ]) : null,
    // (ctrl.data.puzzle.enabled && ctrl.data.user) ? renderVote(ctrl) : null,
    m('a.continue', {
      onclick: ctrl.nextPuzzle
    }, [
      m('i[data-icon=G]'),
      ctrl.trans.noarg('continueTraining')
    ])
  ]);
}
