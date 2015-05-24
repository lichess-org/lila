var m = require('mithril');
var partial = require('chessground').util.partial;
var tournament = require('../tournament');
var util = require('./util');
var button = require('./button');

function scoreTag(s) {
  return {
    tag: s[1] || 'span',
    children: [s[0]]
  };
}

function rank(p) {
  return {
    tag: 'rank',
    attrs: p.rank > 99 ? {
      class: 'big'
    } : {},
    children: [p.rank]
  };
}

function playerTrs(ctrl, maxScore, player) {
  return [{
    tag: 'tr',
    attrs: {
      key: player.id,
      class: ctrl.userId === player.id ? 'me' : ''
    },
    children: [
      m('td', [
        player.withdraw ? m('rank', {
          'data-icon': 'b',
          'title': ctrl.trans('withdraw')
        }) : rank(player),
        util.player(player)
      ]),
      m('td.sheet', player.sheet.scores.map(scoreTag)),
      m('td.total', m('strong',
        player.sheet.fire ? {
          class: 'is-gold',
          'data-icon': 'Q'
        } : {}, player.sheet.total))
    ]
  }, {
    tag: 'tr',
    attrs: {
      key: player.id + '.bar'
    },
    children: [
      m('td', {
        class: 'around-bar',
        colspan: 3
      }, m('div', {
        class: 'bar',
        style: {
          width: Math.ceil(player.sheet.total * 100 / maxScore) + '%'
        }
      }))
    ]
  }];
}

var trophy = m('div.trophy');

function podiumUsername(p) {
  return m('a', {
    class: 'text ulpt user_link',
    href: '/@/' + p.username
  }, p.username);
}

function podiumStats(p, data) {
  var perf;
  if (p.perf === 0) perf = m('span', ' =');
  else if (p.perf > 0) perf = m('span.positive[data-icon=N]', p.perf);
  else if (p.perf < 0) perf = m('span.negative[data-icon=M]', -p.perf);
  var winP = Math.round(p.sheet.scores.filter(function(s) {
    return s[1] === 'double' ? s[0] >= 4 : s[0] >= 2;
  }).length * 100 / p.sheet.scores.length);
  var berserkP = Math.round(p.sheet.scores.filter(function(s) {
    return s[0] === 3 || s[0] === 5;
  }).length * 100 / p.sheet.scores.length);
  return [
    m('span.rating.progress', [
      p.rating,
      perf
    ]),
    m('table.stats', m('tbody', [
      m('tr', [m('th', 'Win rate'), m('td', winP + '%')]),
      p.opposition ? m('tr', [m('th', 'Opponents rating'), m('td', p.opposition)]) : null,
      berserkP > 0 ? m('tr', [m('th', 'Berserk rate'), m('td', berserkP + '%')]) : null
    ]))
  ];
}

function podiumFirst(p, data) {
  if (p) return m('div.first', [
    trophy,
    podiumUsername(p),
    podiumStats(p, data)
  ]);
}

function podiumSecond(p, data) {
  if (p) return m('div.second', [
    trophy,
    podiumUsername(p),
    podiumStats(p, data)
  ]);
}

function podiumThird(p, data) {
  if (p) return m('div.third', [
    trophy,
    podiumUsername(p),
    podiumStats(p, data)
  ]);
}

module.exports = {
  podium: function(ctrl) {
    return m('div.podium', [
      podiumSecond(ctrl.data.players[1], ctrl.data),
      podiumFirst(ctrl.data.players[0], ctrl.data),
      podiumThird(ctrl.data.players[2], ctrl.data)
    ]);
  },
  standing: function(ctrl) {
    var maxScore = Math.max.apply(Math, ctrl.data.players.map(function(p) {
      return p.sheet.total;
    }));
    var player = util.currentPlayer(ctrl);
    return [
      m('thead',
        m('tr', [
          m('th.large', [
            ctrl.trans('standing'),
            player ? [
              m('strong.player_rank', player.rank),
              ' / ' + ctrl.data.players.length
            ] : ' (' + ctrl.data.players.length + ')'
          ]),
          m('th.legend[colspan=2]', [
            m('streakstarter', 'Streak starter'),
            m('double', 'Double points'),
            button.joinWithdraw(ctrl)
          ])
        ])),
      m('tbody', ctrl.data.players.map(partial(playerTrs, ctrl, maxScore)))
    ];
  }
};
