var m = require('mithril');
var partial = require('chessground').util.partial;
var util = require('./util');
var arena = require('./arena');
var pairings = require('./pairings');
var playerInfo = require('./playerInfo');
var pagination = require('../pagination');
var header = require('./header');

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
    return ctrl.vm.playerInfo.id ? playerInfo(ctrl) : pairings(ctrl);
  }
};
