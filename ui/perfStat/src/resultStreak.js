var m = require('mithril');
var util = require('./util');

function streak(s, title) {
  return m('div.streak', [
    m('h3', [
      title + ': ',
      m('strong', s.v),
      ' game' + (s.v > 1 ? 's' : '')
    ]),
    util.fromTo(s)
  ]);
}

function streaks(s) {
  return [
    streak(s.max, 'Longest'),
    streak(s.cur, 'Current')
  ];
}

module.exports = function(d) {
  return [
    m('div.half', [
      m('h2', 'Winning streaks'),
      util.fMap(d.stat.resultStreak.win, streaks, util.noData)
    ]),
    m('div.half', [
      m('h2', 'Losing streaks'),
      util.fMap(d.stat.resultStreak.loss, streaks, util.noData)
    ])
  ];
};
