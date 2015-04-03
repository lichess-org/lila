var m = require('mithril');
var partial = require('chessground').util.partial;
var simul = require('../simul');
var util = require('./util');

module.exports = function(ctrl) {
  var myPairing = simul.myCurrentPairing(ctrl);
  var gameId = myPairing ? myPairing.gameId : null;
  return [
    util.title(ctrl),
    gameId ? m('a.is.is-after.pov.button.glowing', {
      href: '/' + gameId
    }, [
      'You are playing!',
      m('span.text[data-icon=G]', ctrl.trans('joinTheGame'))
    ]) : null,
    util.games(ctrl.data.lastGames)
  ];
};
