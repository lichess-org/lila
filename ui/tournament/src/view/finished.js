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
  return m('div.stats.box', [
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
    return ctrl.vm.playerInfo.id ? playerInfo(ctrl) : [
      stats ? stats(ctrl.data.stats) : null,
      pairings(ctrl)
    ];
  }
};
