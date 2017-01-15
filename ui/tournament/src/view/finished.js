var m = require('mithril');
var arena = require('./arena');
var pairings = require('./pairings');
var playerInfo = require('./playerInfo');
var pagination = require('../pagination');
var header = require('./header');
var numberRow = require('./util').numberRow;

function confetti(data) {
  if (!data.me) return;
  if (!data.isRecentlyFinished) return;
  if (data.me.rank > 3) return;
  if (!lichess.once('tournament.end.canvas.' + data.id)) return;
  return m('canvas', {
    id: 'confetti',
    config: function(el, isUpdate) {
      if (isUpdate) return;
      lichess.loadScript('/assets/javascripts/confetti.js');
    }
  });
}

function stats(st) {
  if (st) return m('div.stats.box', [
    m('h2', 'Tournament complete'),
    m('table', [
      numberRow('Average rating', st.averageRating),
      numberRow('Games played', st.games),
      numberRow('Moves played', st.moves),
      numberRow('White wins', [st.whiteWins, st.games], 'percent'),
      numberRow('Black wins', [st.blackWins, st.games], 'percent'),
      numberRow('Draws', [st.draws, st.games], 'percent'),
      numberRow('Berserk rate', [st.berserks / 2, st.games], 'percent')
    ])
  ]);
}

function nextTournament(t) {
  if (t) return [
    m('a.next', {
      href: '/tournament/' + t.id
    }, [
      m('i', {
        'data-icon': t.perf.icon
      }),
      m('span.content', [
        m('span', 'Next ' + t.perf.name + ' tournament:'),
        m('span.name', t.name),
        m('span.more', [
          ctrl.trans('nbConnectedPlayers', t.nbPlayers),
          ' • ',
          t.finishesAt ? [
            'finishes ',
            m('time.moment-from-now', {
              datetime: t.finishesAt
            }, t.finishesAt)
          ] : m('time.moment-from-now', {
            datetime: t.startsAt
          }, t.startsAt)
        ])
      ])
    ]),
    m('a.others[href=/tournament]', 'View more tournaments')
  ];
}

module.exports = {
  main: function(ctrl) {
    var pag = pagination.players(ctrl);
    return [
      m('div.big_top', [
        confetti(ctrl.data),
        header(ctrl),
        arena.podium(ctrl)
      ]),
      arena.standing(ctrl, pag)
    ];
  },
  side: function(ctrl) {
    var player = ctrl.vm.playerInfo.id;
    return [
      nextTournament(ctrl.data.next),
      player ? playerInfo(ctrl) : null,
      player ? null : stats(ctrl.data.stats),
      player ? null : pairings(ctrl)
    ];
  }
};
