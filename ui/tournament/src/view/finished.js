var m = require('mithril');
var partial = require('chessground').util.partial;
var util = require('./util');
var arena = require('./arena');
var pairings = require('./pairings');
var playerInfo = require('./playerInfo');
var pagination = require('../pagination');

module.exports = {
  main: function(ctrl) {
    var pag = pagination.players(ctrl);
    return [
      util.title(ctrl),
      arena.podium(ctrl),
      arena.standing(ctrl, pag),
      util.games(ctrl.data.lastGames)
    ];
  },
  side: function(ctrl) {
    return ctrl.vm.playerInfo.id ? playerInfo(ctrl) : pairings(ctrl);
  }
};
