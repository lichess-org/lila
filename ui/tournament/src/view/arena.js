var m = require('mithril');
var partial = require('chessground').util.partial;
var classSet = require('chessground').util.classSet;
var util = require('./util');
var button = require('./button');

var scoreTagNames = ['score', 'streak', 'double'];

function scoreTag(s, i) {
  return {
    tag: scoreTagNames[(s[1] || 1) - 1],
    children: [Array.isArray(s) ? s[0] : s]
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

function playerTr(ctrl, player) {
  var isLong = player.sheet.scores.length > 40;
  var userId = player.name.toLowerCase();
  return {
    tag: 'tr',
    attrs: {
      key: userId,
      class: classSet({
        'me': ctrl.userId === userId,
        'long': isLong
      }),
      onclick: partial(ctrl.showPlayerInfo, userId)
    },
    children: [
      m('td', [
        player.withdraw ? m('rank', {
          'data-icon': 'b',
          'title': ctrl.trans('withdraw')
        }) : rank(player),
        util.player(player)
      ]),
      ctrl.data.startsAt ? m('td') : m('td.sheet', player.sheet.scores.map(scoreTag)),
      ctrl.data.startsAt ? null : m('td.total', m('strong',
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
  var winP = nbGames ? Math.round(p.sheet.scores.filter(function(s) {
    return s[1] === 3 ? s[0] >= 3 : (s >= 2 || s[0] >= 2);
  }).length * 100 / nbGames) : 0;
  var berserkP = nbGames ? Math.round(p.sheet.scores.filter(function(s) {
    return s === 3 || s[0] === 3 || s[0] === 5;
  }).length * 100 / nbGames) : 0;
  return [
    m('span.rating.progress', [
      p.rating + p.perf,
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
      podiumPosition(ctrl.data.podium[1], ctrl.data, 'second'),
      podiumPosition(ctrl.data.podium[0], ctrl.data, 'first'),
      podiumPosition(ctrl.data.podium[2], ctrl.data, 'third')
    ]);
  },
  standing: function(ctrl, pag) {
    var player = util.currentPlayer(ctrl, pag);
    return [
      m('thead',
        m('tr', ctrl.data.startsAt ? [
          m('th.large', ctrl.data.nbPlayers + ' Players'),
          ctrl.userId ? m('th',
            ctrl.data.me && !ctrl.data.me.withdraw ? button.withdraw(ctrl) : button.join(ctrl)
          ) : m('th')
        ] : [
          m('th.large', [
            ctrl.trans('standing'), (player && !player.withdraw) ? [
              m('strong.player_rank', player.rank),
              ' / ' + pag.nbResults
            ] : ' (' + pag.nbResults + ')'
          ]),
          m('th.legend[colspan=2]', [
            m('streak.nover', 'Streak starter'),
            m('double.nover', 'Double points'),
            button.joinWithdraw(ctrl)
          ])
        ])),
      m('tbody', pag.currentPageResults.map(partial(playerTr, ctrl)))
    ];
  }
};
