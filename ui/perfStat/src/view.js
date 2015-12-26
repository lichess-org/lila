var m = require('mithril');

function fMap(v, f, orDefault) {
  return v ? f(v) : (orDefault || null);
}

function showUser(u, rating) {
  return m('a', {
    href: '/@/' + u.name
  }, (u.title ? (u.title + ' ') : '') + u.name + ' (' + rating + ')');
}

function percent(x, y) {
  if (y == 0) return 'N/A';
  return [m('strong', Math.round(x * 100 / y)), '%'];
}

function date(d) {
  return m('time', {
    class: 'moment',
    datetime: d,
    'data-format': 'calendar'
  }, d)
}

function gameLink(id, content) {
  return m('a', {
    href: '/' + id
  }, content);
}

function ratingAt(r) {
  return m('p.ratingAt', [
    m('strong', r.int),
    ', ',
    gameLink(r.gameId, ['the ', date(r.at)])
  ]);
}

function result(r) {
  return m('p.result', [
    m('strong', showUser(r.opId, r.opInt)),
    ', ',
    gameLink(r.gameId, ['the ', date(r.at)])
  ]);
}

function streak(s) {
  return [
    m('strong', s.v),
    ' games',
    fMap(s.from, function(r) {
      return [
        ', from ',
        gameLink(r.gameId, date(r.at)),
        ' to ',
        fMap(s.to, function(r) {
          return gameLink(r.gameId, date(r.at));
        }, 'now')
      ];
    })
  ];
}

function streaks(s) {
  return [
    m('div.streak', [
      m('h3', 'Longest streak'),
      streak(s.max)
    ]),
    m('div.streak', [
      m('h3', 'Current streak'),
      streak(s.cur)
    ])
  ];
}

module.exports = function(ctrl) {
  var d = ctrl.data;
  return [
    m('div.glicko', [
      m('h2', 'Glicko rating'),
      m('p', 'Exact rating: ' + d.perf.glicko.rating),
      m('p', 'Progression: ' + d.perf.progress),
      m('p', 'Deviation: ' + d.perf.glicko.deviation),
      m('p', 'Volatility: ' + d.perf.glicko.volatility)
    ]),
    fMap(d.stat.highest, function(r) {
      return m('div', [
        m('h2', 'Highest rating'),
        ratingAt(r)
      ]);
    }),
    fMap(d.stat.lowest, function(r) {
      return m('div', [
        m('h2', 'Lowest rating'),
        ratingAt(r)
      ]);
    }),
    fMap(d.stat.bestWin, function(r) {
      return m('div', [
        m('h2', 'Best win'),
        result(r)
      ]);
    }),
    fMap(d.stat.worstLoss, function(r) {
      return m('div', [
        m('h2', 'Worst loss'),
        result(r)
      ]);
    }),
    fMap(d.stat.count, function(c) {
      var per = function(v) {
        return m('span.percent', ['(', percent(v, c.all), ')']);
      };
      return m('div', [
        m('h2', 'Counters'),
        m('p', 'Total games: ' + c.all),
        m('p', ['Rated games: ' + c.rated, per(c.rated)]),
        m('p', 'Average opponent rating: ' + c.opAvg),
        m('p', ['Victories: ' + c.win, per(c.win)]),
        m('p', ['Draws: ' + c.draw, per(c.draw)]),
        m('p', ['Defeats: ' + c.loss, per(c.loss)])
      ]);
    }),
    fMap(d.stat.resultStreak.win, function(s) {
      return m('div', [
        m('h2', 'Wining streak'),
        streaks(s)
      ]);
    }),
    fMap(d.stat.resultStreak.loss, function(s) {
      return m('div', [
        m('h2', 'Losing streak'),
        streaks(s)
      ]);
    })
  ];
};
