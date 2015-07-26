var m = require('mithril');
var chessground = require('chessground');

var sections = require('./sections');
var coach = require('coach');

function sideCommands(ctrl) {
  return [
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
    })
  ];
}

module.exports = function(ctrl, inspecting) {
  var d = ctrl.data;
  var eco = inspecting.eco;
  var o = d.openings[eco];
  if (!o) return m('div.top.nodata', [
    sideCommands(ctrl),
    m('p', 'No results for this data range and opening!')
  ]);
  var opening = o.opening;
  var results = o.results;
  return m('div.top.inspect', [
    sideCommands(ctrl),
    coach.resultBar(results),
    m('div.main', [
      coach.shared.progress(results.ratingDiff / results.nbGames),
      m('h2', [
        m('strong', [
          opening.eco,
          ' ',
          opening.name
        ]),
        m('em', opening.moves)
      ]),
      m('div.baseline', [
        m('strong', results.nbGames),
        ' games, ',
        m('strong', results.nbAnalysis),
        ' analysed. Last played ',
        coach.shared.momentFromNow(results.lastPlayed),
        '.',
      ])
    ]),
    m('div.content', [
      m('div.board',
        chessground.view(ctrl.vm.inspecting.chessground)
      ),
      m('div.right', [
        // moves(ctrl, results),
        sections(ctrl, results),
        results.bestWin ? [
          m('br'),
          ' Best win: ',
          coach.bestWin(results.bestWin, ctrl.color)
        ] : null
        // m('table', [
        //   m('tr', [
        //   m('tr', [
        //     m('th', 'Average opponent'),
        //     m('tr', m('strong', results.opponentRatingAvg))
        //   ]),
        //   results.bestWin ? m('tr', [
        //     m('th', 'Best win'),
        //     m('tr', bestWin(results.bestWin))
        //   ]) : null
        // ])
      ])
    ])
  ]);
};
