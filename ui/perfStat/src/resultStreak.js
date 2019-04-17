var m = require('mithril');
var util = require('./util');

function streak(s, title, color) {
  return m('div.streak', [
    m('h3', [
      title + ': ',
      s.v > 0 ? color([
        m('strong', s.v),
        ' game' + (s.v > 1 ? 's' : '')
      ]) : 'none'
    ]),
    util.fromTo(s)
  ]);
}

function streaks(color) {
  return function(s) {
    return [
      streak(s.max, 'Longest', color),
      streak(s.cur, 'Current', color)
    ];
  };
}

module.exports = function(d) {
  return [
    m('div', [
      m('h2', 'Winning streak'),
      util.fMap(d.stat.resultStreak.win, streaks(util.green), util.noData)
    ]),
    m('div', [
      m('h2', 'Losing streak'),
      util.fMap(d.stat.resultStreak.loss, streaks(util.red), util.noData)
    ])
  ];
};
