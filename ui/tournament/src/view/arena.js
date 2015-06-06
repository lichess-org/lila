var m = require('mithril');
var partial = require('chessground').util.partial;
var tournament = require('../tournament');
var util = require('./util');
var button = require('./button');

var scoreTagName = {
  1: 'span',
  2: 'streak',
  3: 'double'
};

function scoreTag(s) {
  return {
    tag: scoreTagName[s[1] || 1],
    children: [Array.isArray(s) ? s[0] : [s]]
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

function playerTr(ctrl, maxScore, player) {
  return {
    tag: 'tr',
    attrs: {
      key: player.id,
      class: ctrl.userId === player.name.toLowerCase() ? 'me' : ''
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
  };
}

var trophy = m('div.trophy');

function podiumUsername(p) {
  return m('a', {
    class: 'text ulpt user_link',
    href: '/@/' + p.name
  }, p.name);
}

function podiumStats(p, data) {
  var perf;
  if (p.perf === 0) perf = m('span', ' =');
  else if (p.perf > 0) perf = m('span.positive[data-icon=N]', p.perf);
  else if (p.perf < 0) perf = m('span.negative[data-icon=M]', -p.perf);
  var nbGames = p.sheet.scores.length;
  var winP = Math.round(p.sheet.scores.filter(function(s) {
    return s[1] === 'double' ? s[0] >= 4 : s[0] >= 2;
  }).length * 100 / nbGames);
  var berserkP = Math.round(p.sheet.scores.filter(function(s) {
    return s === 3 || s[0] === 3 || s[0] === 5;
  }).length * 100 / nbGames);
  return [
    m('span.rating.progress', [
      p.rating,
      perf
    ]),
    m('table.stats', m('tbody', [
      m('tr', [m('th', 'Win rate'), m('td', winP + '%')]),
      berserkP > 0 ? m('tr', [m('th', 'Berserk win rate'), m('td', berserkP + '%')]) : null,
      p.opposition ? m('tr', [m('th', 'Opponents rating'), m('td', p.opposition)]) : null,
      m('tr', [m('th', 'Games played'), m('td', nbGames)])
    ]))
  ];
}

function podiumPosition(p, data, pos) {
  if (p) return m('div.' + pos, [
    trophy,
    podiumUsername(p),
    podiumStats(p, data)
  ]);
}

module.exports = {
  podium: function(ctrl) {
    return m('div.podium', [
      podiumPosition(ctrl.data.players[1], ctrl.data, 'second'),
      podiumPosition(ctrl.data.players[0], ctrl.data, 'first'),
      podiumPosition(ctrl.data.players[2], ctrl.data, 'third')
    ]);
  },
  standing: function(ctrl, pag) {
    var maxScore = Math.max.apply(Math, ctrl.data.players.map(function(p) {
      return p.sheet.total;
    }));
    var player = util.currentPlayer(ctrl);
    return [
      m('thead',
        m('tr', [
          m('th.large', [
            ctrl.trans('standing'),
            (player && !player.withdraw) ? [
              m('strong.player_rank', player.rank),
              ' / ' + pag.nbResults
            ] : ' (' + pag.nbResults + ')'
          ]),
          m('th.legend[colspan=2]', [
            m('streak', 'Streak starter'),
            m('double', 'Double points'),
            button.joinWithdraw(ctrl)
          ])
        ])),
      m('tbody', pag.currentPageResults.map(partial(playerTr, ctrl, maxScore)))
    ];
  }
};
