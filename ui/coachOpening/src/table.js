var m = require('mithril');

var progress = require('./shared').progress;
var resultBar = require('./shared').resultBar;
var strings = require('./shared').strings;

var headers = [
  ['name', 'Opening'],
  ['nbGames', 'Games'],
  ['result', 'Result', strings.result],
  ['ratingDiffAvg', 'Rating', strings.ratingDiff],
  ['acpl', 'ACPL', strings.acpl],
  ['lastPlayed', 'Last played']
];

function thead(list, ctrl) {
  return m('thead', {
    onclick: function(e) {
      var prop = e.target.getAttribute("data-sort-by") || e.target.parentNode.getAttribute("data-sort-by");
      if (prop) ctrl.setSort(prop);
    }
  }, m('tr', headers.map(function(h) {
    var props = {
      key: h[0],
      'data-sort-by': h[0]
    };
    if (ctrl.vm.sort.prop === h[0]) props['data-icon'] = ctrl.vm.sort.order === -1 ? 'R' : 'S';
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
    return Math.round(nb * 100 / d.openingResults.nbGames);
  };
  var acplAvg = ctrl.data.colorResults.gameSections.all.acpl.avg;
  return m('table.openings.slist', [
    thead(ctrl.list, ctrl),
    m('tbody', ctrl.list.map(function(o, i) {
      return m('tr', {
        key: o.opening.eco,
        'data-opening': o.opening.eco,
        onmouseover: function(e) {
          ctrl.vm.hover = null;
          var chart = ctrl.vm.chart;
          if (!chart) return;
          var point = chart.series[1].data.filter(function(c) {
            return c.opening.eco === o.opening.eco;
          })[0];
          if (point && !point.selected) point.select();
        },
        onmouseout: function() {
          if (ctrl.vm.chart) ctrl.vm.chart.getSelectedPoints().forEach(function(point) {
            point.select(false);
          });
        },
        onclick: function() {
          ctrl.inspect(o.opening.eco);
        },
        class: (ctrl.vm.hover === o.opening.eco ? 'hover' : (ctrl.isInspecting(o.opening.eco) ? 'active' : ''))
      }, [
        m('td', [
          m('div.name', o.opening.name),
          m('div.moves', o.opening.formattedMoves)
        ]),
        m('td', [
          m('div', o.results.nbGames + ' (' + percent(o.results.nbGames) + '%)')
        ]),
        m('td', resultBar(o.results)),
        m('td', progress(o.ratingDiffAvg)),
        m('td', [
          m('span.progress', o.acpl === null ? '-' : m('span', {
            class: o.acpl > acplAvg ? 'negative' : 'positive'
          }, o.acpl))
        ]),
        m('td', [
          m('time.moment-from-now', {
            datetime: o.results.lastPlayed
          })
        ])
      ]);
    }))
  ]);
};
