var m = require('mithril');

var coach = require('coach');
var strings = coach.shared.strings;

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
  return m('table.selector.slist', [
    thead(ctrl.list, ctrl),
    m('tbody', ctrl.list.map(function(o, i) {
      return m('tr', {
        key: o.opening.eco,
        onclick: function() {
          ctrl.inspect(o.opening.eco);
        },
        class: (ctrl.isInspecting(o.opening.eco) ? 'active' : '')
      }, [
        m('td', [
          m('div.name', o.opening.name),
          m('div.moves', o.opening.formattedMoves)
        ]),
        m('td', [
          m('div', o.results.nbGames + ' (' + percent(o.results.nbGames) + '%)')
        ]),
        m('td', coach.resultBar(o.results)),
        m('td', coach.shared.progress(o.ratingDiffAvg)),
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
