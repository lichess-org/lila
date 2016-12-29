var m = require('mithril');

function renderVote(ctrl) {
  var data = ctrl.getData();
  if (!data.puzzle.enabled) return;
  return m('div.vote', [
    m('a[data-icon=S]', {
      title: ctrl.trans.noarg('thisPuzzleIsCorrect'),
      class: ctrl.vm.voted === true ? ' active' : '',
      onclick: function() {
        ctrl.vote(true);
      }
    }),
    m('span.count.hint--bottom[data-hint=Popularity]', Math.max(0, data.puzzle.vote)),
    m('a[data-icon=R]', {
      title: ctrl.trans.noarg('thisPuzzleIsWrong'),
      class: ctrl.vm.voted === false ? ' active' : '',
      onclick: function() {
        ctrl.vote(false);
      }
    })
  ]);
}

module.exports = function(ctrl) {
  var data = ctrl.getData();
  return m('div.feedback.after', [
    // (!ctrl.hasEverVoted.get() && data.puzzle.enabled && data.voted === null) ? m('div.please_vote', [
    //   m('p.first', [
    //     m('strong', ctrl.trans('wasThisPuzzleAnyGood')),
    //     m('br'),
    //     m('span', ctrl.trans('pleaseVotePuzzle'))
    //   ]),
    //   m('p.then',
    //     m('strong', ctrl.trans('thankYou'))
    //   )
    // ]) : null,
    // (data.puzzle.enabled && data.user) ? renderVote(ctrl) : null,
    m('div.half.top', [
      ctrl.vm.lastFeedback === 'win' ? m('div.complete.feedback.win', m('div.player', [
        m('div.icon', '✓'),
        m('div.instruction', ctrl.trans.noarg('victory'))
      ])) : m('div.complete', 'Puzzle complete!'),
      data.user ? renderVote(ctrl) : null
    ]),
    m('a.half.continue', {
      onclick: ctrl.nextPuzzle
    }, [
      m('i[data-icon=G]'),
      ctrl.trans.noarg('continueTraining')
    ])
  ]);
}
