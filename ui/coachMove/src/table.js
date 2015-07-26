var m = require('mithril');

var coach = require('coach');
var strings = coach.shared.strings;

var headers = [
  ['name', 'Category'],
  ['nbGames', 'Games'],
  ['result', 'Result', strings.result],
  ['ratingDiffAvg', 'Rating', strings.ratingDiff],
  ['acpl', 'ACPL', strings.acpl],
  ['lastPlayed', 'Last played']
];

function thead(ctrl) {
  return m('thead', m('tr', headers.map(function(h) {
    var props = {
      key: h[0],
    };
    var spanProps = {};
    if (h[2]) {
      spanProps.class = 'hint--top';
      spanProps['data-hint'] = h[2];
    }
    return m('th', props, m('span', spanProps, h[1]));
  })));
}

module.exports = function(ctrl) {
  var d = ctrl.data;
  var percent = function(nb) {
    return Math.round(nb * 100 / d.perfs[0].results.base.nbGames);
  };
  var acplAvg = ctrl.data.perfs[0].results.base.gameSections.all.acpl.avg;
  return m('table.selector.slist', [
    thead(ctrl),
    m('tbody', ctrl.data.perfs.map(function(o, i) {
      var perf = o.perf;
      var perfResults = o.results;
      var results = perfResults.base;
      var acpl = results.gameSections.all.acpl.avg;
      return m('tr', {
        key: perf.key,
        onclick: function() {
          ctrl.inspect(perf.key);
        },
        class: (ctrl.vm.inspecting === perf.key ? 'active' : '')
      }, [
        m('td', [
          m('div.name.text', {
            'data-icon': perf.icon
          }, perf.name)
        ]),
        m('td', [
          m('div', results.nbGames + ' (' + percent(results.nbGames) + '%)')
        ]),
        m('td', coach.resultBar(results)),
        m('td', coach.shared.progress(results.nbGames > 0 ? results.ratingDiff / results.nbGames : 0)),
        m('td', [
          m('span.progress', acpl === null ? '-' : m('span', {
            class: acpl > acplAvg ? 'negative' : 'positive'
          }, acpl))
        ]),
        m('td', [
          m('time.moment-from-now', {
            datetime: results.lastPlayed
          })
        ])
      ]);
    }))
  ]);
};
