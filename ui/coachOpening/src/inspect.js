var m = require('mithril');
var chessground = require('chessground');

var moves = require('./moves');
var progress = require('./shared').progress;
var momentFromNow = require('./shared').momentFromNow;
var resultBar = require('./shared').resultBar;

function bestWin(w) {
  if (!w.user) return;
  return m('a', {
    href: '/' + w.id
  }, [
    w.user.title ? (w.user.title + ' ') : '',
    w.user.name,
    ' (',
    m('strong', w.rating),
    ')'
  ]);
}

module.exports = function(ctrl, inspecting) {
  var d = ctrl.data;
  var eco = inspecting.eco;
  var opening = d.openings[eco].opening;
  var results = d.openings[eco].results;
  return m('div.top.inspect', [
    m('a.to.back', {
      'data-icon': 'L',
      onclick: ctrl.uninspect
    }),
    m('a.to.prev', {
      'data-icon': 'I',
      onclick: function() {
        ctrl.jumpBy(-1);
      }
    }),
    m('a.to.next', {
      'data-icon': 'H',
      onclick: function() {
        ctrl.jumpBy(1);
      }
    }),
    resultBar(results),
    m('h2', [
      m('strong', [
        opening.eco,
        ' ',
        opening.name
      ]),
      m('em', opening.moves),
      progress(results.ratingDiff / results.nbGames)
    ]),
    m('div.content', [
      m('div.board',
        chessground.view(ctrl.vm.inspecting.chessground)
      ),
      m('div.right', [
        moves(ctrl, results),
        m('table', [
          m('tr', [
            m('th', 'Played in'),
            m('tr', [
              m('a', [
                m('strong', results.nbGames),
                ' games (',
                m('strong', Math.round(results.nbGames * 100 / ctrl.data.colorResults.nbGames)),
                '%)'
              ])
            ])
          ]),
          m('tr', [
            m('th', 'Computer analysed in'),
            m('tr', [
              m('a', [
                m('strong', results.nbAnalysis),
                ' games (',
                m('strong', Math.round(results.nbAnalysis * 100 / results.nbGames)),
                '%)'
              ])
            ])
          ]),
          m('tr', [
            m('th', 'Average opponent'),
            m('tr', m('strong', results.opponentRatingAvg))
          ]),
          results.bestWin ? m('tr', [
            m('th', 'Best win'),
            m('tr', bestWin(results.bestWin))
          ]) : null,
          m('tr', [
            m('th', 'Last played'),
            m('tr', momentFromNow(results.lastPlayed))
          ])
        ])
      ])
    ])
  ]);
};
