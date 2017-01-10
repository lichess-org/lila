var m = require('mithril');

function fMap(v, f, orDefault) {
  return v ? f(v) : (orDefault || null);
}

function gameLink(id, content) {
  return m('a', {
    class: 'glpt',
    href: '/' + id
  }, content);
}

function date(d) {
  return m('time', {
    class: 'moment',
    datetime: d,
    'data-format': 'calendar'
  }, '...')
}

function fromTo(s) {
  return fMap(s.from, function(r) {
    return [
      'from ',
      gameLink(r.gameId, date(r.at)),
      ' to ',
      fMap(s.to, function(r) {
        return gameLink(r.gameId, date(r.at));
      }, 'now')
    ];
  }, m.trust('&nbsp;'));
}

module.exports = {
  fMap: fMap,
  gameLink: gameLink,
  date: date,
  showUser: function(u, rating) {
    return m('a', {
      class: 'ulpt',
      href: '/@/' + u.name
    }, (u.title ? (u.title + ' ') : '') + u.name + ' (' + rating + ')');
  },
  noData: 'Not enough games played.',
  fromTo: fromTo,
  streaks: function(s, f) {
    return [
      m('div.streak', [
        m('h3', 'Longest: '), f(s.max)
      ]),
      m('div.streak', [
        m('h3', 'Current streak'), f(s.cur)
      ])
    ];
  },
  green: function(v) {
    return m('green', v);
  },
  red: function(v) {
    return m('red', v);
  },
  identity: function(v) {
    return v;
  },
  formatSeconds: function(s, format) {
    var d = moment.duration(s, 'seconds');
    var hours = d.days() * 24 + d.hours();
    if (format === 'short') return hours + 'h, ' + d.minutes() + 'm';
    return hours + ' hours, ' + d.minutes() + ' minutes';
  }
};
