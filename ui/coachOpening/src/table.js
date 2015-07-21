var m = require('mithril');

var headers = [
  ['name', 'Opening'],
  ['nbGames', 'Games'],
  ['nbWin', 'Win'],
  ['nbDraw', 'Draw'],
  ['nbLoss', 'Loss'],
  ['ratingDiff', 'Rating'],
  ['acpl', 'Mistakes'],
  ['lastPlayed', 'Last played']
];

function thead(ctrl) {
  return m('thead', m('tr', headers.map(function(h) {
    var props = {
      'data-sort-by': h[0]
    };
    if (ctrl.vm.sort.field === h[0])
      props['data-icon'] = ctrl.vm.sort.order === -1 ? 'R' : 'S';
    return m('th', props, h[1]);
  })));
}

function signed(i) {
  return (i > 0 ? '+' : '') + i;
}

function sorts(list, sort) {
  return {
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
  }
}

module.exports = function(ctrl) {
  var d = ctrl.data;
  var percent = function(nb) {
    return Math.round(nb * 100 / d.openings.nbGames);
  };
  return m('table.slist', sorts(ctrl.list, ctrl.vm.sort), [
    thead(ctrl),
    m('tbody', ctrl.list.map(function(o) {
      return m('tr', [
        m('td.opening', [
          m('div.name', o.name),
          m('div.moves', d.moves[o.name])
        ]),
        m('td.games', [
          m('div', o.nbGames + ' (' + percent(o.nbGames) + '%)')
        ]),
        m('td.win', o.nbWin),
        m('td.draw', o.nbDraw),
        m('td.loss', o.nbLoss),
        m('td.rating', [
          m('div', signed(o.ratingDiff))
        ]),
        m('td.mistake', [
          m('div', o.acpl === null ? '-' : (o.acpl + ' ACPL'))
        ]),
        m('td.lastPlayed', [
          m('time.moment-from-now', {
            datetime: o.lastPlayed
          })
        ])
      ]);
    }))
  ]);
};
