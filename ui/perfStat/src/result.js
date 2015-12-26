var m = require('mithril');
var util = require('./util');

function resultTable(results, title) {
  return m('table', [
    m('thead', m('tr', m('th[colspan=2]', m('h2', title)))),
    m('tbody', results.map(function(r) {
      return m('tr', [
        m('td', util.showUser(r.opId, r.opInt)),
        m('td', util.gameLink(r.gameId, util.date(r.at)))
      ]);
    }))
  ]);
}

module.exports = function(d) {
  return [
    m('div.half', resultTable(d.stat.bestWins.results, 'Best rated victories')),
    m('div.half', resultTable(d.stat.worstLosses.results, 'Worst rated defeats'))
  ];
};
