var m = require('mithril');
var chessground = require('chessground');

var coach = require('coach');

function sideCommands(ctrl) {
  return [
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

module.exports = function(ctrl) {
  var d = ctrl.data;
  var o = d.perfs.filter(function(o) {
    return o.perf.key === ctrl.vm.inspecting;
  })[0];
  var perf = o.perf;
  var perfResults = o.results;
  var results = perfResults.base;
  return m('div.top.inspect', [
    sideCommands(ctrl),
    coach.resultBar(results),
    m('div.main', [
      coach.shared.progress(results.ratingDiff / results.nbGames),
      m('h2', {
        'data-icon': perf.icon
      }, perf.name),
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
      m('div.right', [
        results.bestWin ? [
          m('br'),
          ' Best win: ',
          coach.bestWin(results.bestWin, ctrl.color)
        ] : null,
        m('table', [
          m('tr', [
            m('tr', [
              m('th', 'Average opponent'),
              m('tr', m('strong', results.opponentRatingAvg))
            ]),
          ])
        ])
      ])
    ])
  ]);
};
