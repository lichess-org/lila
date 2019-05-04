var m = require('mithril');
var util = require('./util');

function streak(s, title, display) {
  return m('div.streak', [
    m('h3', [
      title + ': ',
      display(s.v)
    ]),
    util.fromTo(s)
  ]);
}

function streaks(s, display) {
  return m('div.split', [
    m('div', streak(s.max, 'Longest streak', display)),
    m('div', streak(s.cur, 'Current streak', display))
  ]);
}

var lessThan = 'Less than one hour between games.';

module.exports = {
  nb: function(d) {
    return util.fMap(d.stat.playStreak.nb, function(s) {
      return [
        m('h2', m('span', {
          title: lessThan
        }, 'Games played in a row')),
        streaks(s, function(v) {
          return v ? [
            m('strong', v),
            ' game' + (v > 1 ? 's' : '')
          ] : 'none';
        })
      ];
    });
  },
  time: function(d) {
    return util.fMap(d.stat.playStreak.time, function(s) {
      return [
        m('h2', m('span', {
          title: lessThan
        }, 'Max time spent playing')),
        streaks(s, util.formatSeconds)
      ];
    });
  }
};
