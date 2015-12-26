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
  return m('div', [
    m('div.half', streak(s.max, 'Longest streak', display)),
    m('div.half', streak(s.cur, 'Current streak', display))
  ]);
}

function formatSeconds(s) {
  var d = moment.duration(s, 'seconds');
  var hours = d.days() * 24 + d.hours();
  return hours + ' hours, ' + d.minutes() + ' minutes';
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
          return [
            m('strong', v),
            ' game' + (v > 1 ? 's' : '')
          ];
        })
      ];
    });
  },
  time: function(d) {
    return util.fMap(d.stat.playStreak.time, function(s) {
      return [
        m('h2', m('span', {
          title: 'less than one hour between games'
        }, 'Max time spent playing')),
        streaks(s, formatSeconds)
      ];
    });
  }
};

// fMap(d.stat.playStreak.nb, function(s) {
//   return m('div', [
//     m('h2', 'Games played in a row (less than one hour between games)'),
//     streaks(s)
//   ]);
// }),
// fMap(d.stat.playStreak.time, function(s) {
//   return m('div', [
//     m('h2', 'Max time spent playing (less than one hour between games)'),
//     streaks(s, function(s) {
//       return [
//         m('strong', formatSeconds(s.v)),
//         fromTo(s)
//       ];
//     })
//   ]);
// })
