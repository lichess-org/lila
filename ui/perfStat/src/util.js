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
  });
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
        m('h3', 'Longest: '), (f || streak)(s.max)
      ]),
      m('div.streak', [
        m('h3', 'Current streak'), (f || streak)(s.cur)
      ])
    ];
  }
};
