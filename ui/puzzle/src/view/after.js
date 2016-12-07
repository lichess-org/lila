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

function renderViewTable(ctrl) {
  return [
    (!ctrl.hasEverVoted.get() && ctrl.data.puzzle.enabled && ctrl.data.voted === null) ? m('div.please_vote', [
      m('p.first', [
        m('strong', ctrl.trans.noarg('wasThisPuzzleAnyGood')),
        m('br'),
        m('span', ctrl.trans.noarg('pleaseVotePuzzle'))
      ]),
      m('p.then',
        m('strong', ctrl.trans.noarg('thankYou'))
      )
    ]) : null,
    m('div.box', [
      (ctrl.data.puzzle.enabled && ctrl.data.user) ? renderVote(ctrl) : null,
      m('h2.text[data-icon="-"]',
        m('a', {
          href: '/training/' + ctrl.data.puzzle.id
        }, ctrl.trans('puzzleId', ctrl.data.puzzle.id))
      ),
      m('p', m.trust(ctrl.trans('ratingX', m('strong', ctrl.data.puzzle.rating)))),
      m('p', m.trust(ctrl.trans('playedXTimes', m('strong', ctrl.data.puzzle.attempts)))),
      m('p',
        m('input.copyable.autoselect[readonly][spellCheck=false]', {
          value: 'https://lichess.org/training/' + ctrl.data.puzzle.id
        })
      )
    ]),
    m('div.continue_wrap', [
      renderCommentary(ctrl),
      renderResult(ctrl),
      ctrl.data.win === null ? m('button.continue.button.text[data-icon=G]', {
        onclick: partial(xhr.newPuzzle, ctrl)
      }, ctrl.trans.noarg('continueTraining')) : m('a.continue.button.text[data-icon=G]', {
        onclick: partial(xhr.newPuzzle, ctrl)
      }, ctrl.trans.noarg('continueTraining')), !(ctrl.data.win === null ? ctrl.data.round.win : ctrl.data.win) ? m('a.retry.text[data-icon=P]', {
        onclick: partial(xhr.retry, ctrl)
      }, ctrl.trans.noarg('retryThisPuzzle')) : null
    ])
  ];
}

// function renderViewControls(ctrl, fen) {
//   var d = ctrl.data;
//   var history = d.replay.history;
//   var step = d.replay.step;
//   return m('div.game_control', [
//     d.puzzle.gameId ? m('a.button.hint--bottom', {
//       'data-hint': ctrl.trans('fromGameLink', d.puzzle.gameId),
//       href: '/' + d.puzzle.gameId + '/' + d.puzzle.color + '#' + d.puzzle.initialPly
//     }, m('span[data-icon=v]')) : null,
//     m('a.button.hint--bottom', {
//       'data-hint': ctrl.trans('analysis'),
//       href: puzzle.makeUrl('/analysis/', fen) + '?color=' + ctrl.chessground.data.orientation,
//     }, m('span[data-icon=A]')),
//     m('div#GameButtons.hint--bottom', {
//       'data-hint': 'Review puzzle solution'
//     }, [
//       ['first', 'W', 0],
//       ['prev', 'Y', step - 1],
//       ['next', 'X', step + 1],
//       ['last', 'V', history.length - 1]
//     ].map(function(b) {
//       var enabled = step != b[2] && b[2] >= 0 && b[2] < history.length;
//       return m('a.button.' + b[0] + (enabled ? '' : '.disabled'), {
//         'data-icon': b[1],
//         onmousedown: enabled ? partial(ctrl.jump, b[2]) : null
//       });
//     }))
//   ]);
// }

module.exports = function(ctrl) {
  return m('div.feedback.view', [
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
    m('a.continue.button.text[data-icon=G]', {
      onclick: ctrl.nextPuzzle
    }, ctrl.trans.noarg('continueTraining'))
  ]);
}
