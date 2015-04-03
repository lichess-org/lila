var m = require('mithril');
var partial = require('chessground').util.partial;
var simul = require('../simul');
var util = require('./util');
var pairings = require('./pairings');
var results = require('./results');

module.exports = function(ctrl) {
  var myPairing = simul.myCurrentPairing(ctrl);
  var gameId = myPairing ? myPairing.game.id : null;
  return [
    gameId ? m('a.is.is-after.pov.button.glowing.top_right', {
      href: '/' + gameId
    }, m('span.text[data-icon=G]', ctrl.trans('joinTheGame'))) : null,
    util.title(ctrl),
    results(ctrl),
    pairings(ctrl)
  ];
};
