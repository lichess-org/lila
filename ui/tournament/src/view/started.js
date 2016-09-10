var m = require('mithril');
var arena = require('./arena');
var pairings = require('./pairings');
var playerInfo = require('./playerInfo');
var pagination = require('../pagination');
var header = require('./header');
var myCurrentGameId = require('../tournament').myCurrentGameId;

function joinTheGame(ctrl, gameId) {
  return m('a.is.is-after.pov.button.glowed', {
    href: '/' + gameId
  }, [
    'You are playing!',
    m('span.text[data-icon=G]', ctrl.trans('joinTheGame'))
  ]);
}

module.exports = {
  main: function(ctrl) {
    var gameId = myCurrentGameId(ctrl);
    var pag = pagination.players(ctrl);
    return [
      header(ctrl),
      gameId ? joinTheGame(ctrl, gameId) : null,
      arena.standing(ctrl, pag)
    ];
  },
  side: function(ctrl) {
    return ctrl.vm.playerInfo.id ? playerInfo(ctrl) : pairings(ctrl);
  }
};
