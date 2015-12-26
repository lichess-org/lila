var m = require('mithril');

function percent(x, y) {
  if (y == 0) return 'N/A';
  return Math.round(x * 100 / y) + '%';
}

module.exports = function(d) {
  var c = d.stat.count;
  var per = function(x, y) {
    return percent(x, y || c.all);
  };
  return [
    m('div.half', m('table', m('tbody', [
      m('tr', [
        m('th', 'Total games'),
        m('td', c.all),
        m('td'),
      ]),
      m('tr.full', [
        m('th', 'Rated games'),
        m('td', c.rated),
        m('td', per(c.rated)),
      ]),
      m('tr.full', [
        m('th', 'Tournament games'),
        m('td', c.tour),
        m('td', per(c.tour)),
      ]),
      m('tr.full', [
        m('th', 'Berserked games'),
        m('td', c.berserk),
        m('td', per(c.berserk, c.tour)),
      ])
    ]))),
    m('div.half', m('table', m('tbody', [
      m('tr', [
        m('th', 'Average opponent'),
        m('td', c.opAvg),
        m('td')
      ]),
      m('tr.full', [
        m('th', 'Victories'),
        m('td', c.win),
        m('td', per(c.win))
      ]),
      m('tr.full', [
        m('th', 'Draws'),
        m('td', c.draw),
        m('td', per(c.draw))
      ]),
      m('tr.full', [
        m('th', 'Defeats'),
        m('td', c.loss),
        m('td', per(c.loss))
      ])
    ])))
  ];
};
