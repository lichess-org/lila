var m = require('mithril');
var util = require('./util');

function percent(x, y) {
  if (y === 0) return 0;
  return Math.round(x * 100 / y);
}

module.exports = function(d) {
  var c = d.stat.count;
  var per = function(x, y) {
    return percent(x, y || c.all) + '%';
  };
  return [
    m('div', m('table', m('tbody', [
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
      ]),
      c.seconds ? m('tr.full', [
        m('th', 'Time spent playing'),
        m('td[colspan=2]', util.formatSeconds(c.seconds, 'short'))
      ]) : null
    ]))),
    m('div', m('table', m('tbody', [
      m('tr', [
        m('th', 'Average opponent'),
        m('td', c.opAvg),
        m('td')
      ]),
      m('tr.full', [
        m('th', 'Victories'),
        m('td', util.green(c.win)),
        m('td', util.green(per(c.win)))
      ]),
      m('tr.full', [
        m('th', 'Draws'),
        m('td', c.draw),
        m('td', per(c.draw))
      ]),
      m('tr.full', [
        m('th', 'Defeats'),
        m('td', util.red(c.loss)),
        m('td', util.red(per(c.loss)))
      ]),
      m('tr.full', (function(color) {
        return [
          m('th', 'Disconnections'),
          m('td', color(c.disconnects)),
          m('td', color(per(c.disconnects, c.loss)))
        ];
      })(percent(c.disconnects, c.loss) >= 15 ? util.red : util.identity))
    ])))
  ];
};
