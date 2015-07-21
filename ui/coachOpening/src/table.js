var m = require('mithril');

var progress = require('./shared').progress;
var resultBar = require('./shared').resultBar;

var headers = [
  ['name', 'Opening'],
  ['nbGames', 'Games'],
  ['result', 'Result'],
  ['ratingDiff', 'Rating'],
  ['acpl', 'ACPL'],
  ['lastPlayed', 'Last played']
];

function thead(list, sort) {
  return m('thead', {
    onclick: function(e) {
      var prop = e.target.getAttribute("data-sort-by");
      if (prop) {
        var first = list[0];
        list.sort(function(a, b) {
          return a[prop] > b[prop] ? 1 : a[prop] < b[prop] ? -1 : 0;
        });
        sort.field = prop;
        sort.order = 1;
        if (first === list[0]) {
          list.reverse();
          sort.order = -1;
        }
      }
    }
  }, m('tr', headers.map(function(h) {
    var props = {
      key: h[0],
      'data-sort-by': h[0]
    };
    if (sort.field === h[0]) props['data-icon'] = sort.order === -1 ? 'R' : 'S';
    return m('th', props, h[1]);
  })));
}

module.exports = function(ctrl) {
  var d = ctrl.data;
  var percent = function(nb) {
    return Math.round(nb * 100 / d.openings.nbGames);
  };
  return m('table.slist', [
    thead(ctrl.list, ctrl.vm.sort),
    m('tbody', ctrl.list.map(function(o, i) {
      return m('tr', {
        key: o.name,
        'data-opening': o.name,
        onmouseover: function(e) {
          ctrl.vm.hover = null;
          var chart = ctrl.vm.chart;
          if (!chart) return;
          var point = chart.series[1].data.filter(function(c) {
            return c.name === o.name;
          })[0];
          if (point && !point.selected) point.select();
        },
        onmouseout: function() {
          if (ctrl.vm.chart) ctrl.vm.chart.getSelectedPoints().forEach(function(point) {
            point.select(false);
          });
        },
        onclick: function() {
          ctrl.inspect(o.name);
        },
        class: (ctrl.vm.hover === o.name ? 'hover' : (ctrl.isInspecting(o.name) ? 'active' : ''))
      }, [
        m('td', [
          m('div.name', o.name),
          m('div.moves', d.moves[o.name])
        ]),
        m('td', [
          m('div', o.nbGames + ' (' + percent(o.nbGames) + '%)')
        ]),
        m('td', resultBar(o)),
        m('td', progress(o.ratingDiff)),
        m('td', [
          m('div', o.acpl === null ? '-' : o.acpl)
        ]),
        m('td', [
          m('time.moment-from-now', {
            datetime: o.lastPlayed
          })
        ])
      ]);
    }))
  ]);
};
