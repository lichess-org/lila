var m = require('mithril');
var chessground = require('chessground');

var coach = require('coach');
var movechart = require('./movechart');

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
  if (!o) return m('div.top.nodata', [
    sideCommands(ctrl),
    m('p', 'No results for this data range and perf!')
  ]);
  var perf = o.perf;
  var perfResults = o.results;
  var results = perfResults.base;
  return m('div.top.inspect', [
    sideCommands(ctrl),
    coach.resultBar(results),
    m('div.main.clearfix', [
      coach.shared.progress(results.ratingDiff / results.nbGames),
      m('h2', m('strong.text', {
        'data-icon': perf.icon
      }, perf.name)),
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
      movechart(ctrl),
    ])
  ]);
};
