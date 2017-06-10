var m = require('mithril');
var arena = require('./arena');
var pairings = require('./pairings');
var playerInfo = require('./playerInfo');
var pagination = require('../pagination');
var header = require('./header');
var tour = require('../tournament');

function joinTheGame(ctrl, gameId) {
  return m('a.is.is-after.pov.button.glowed', {
    href: '/' + gameId
  }, [
    ctrl.trans('youArePlaying'),
    m('span.text[data-icon=G]', ctrl.trans('joinTheGame'))
  ]);
}

function notice(ctrl) {
  return tour.willBePaired(ctrl) ? m('div.notice.wait',
    ctrl.trans('standByX', ctrl.data.me.username)
  ) : m('div.notice.closed', ctrl.trans('tournamentPairingsAreNowClosed'));
}

module.exports = {
  main: function(ctrl) {
    var gameId = tour.myCurrentGameId(ctrl);
    return [
      header(ctrl),
      gameId ? joinTheGame(ctrl, gameId) : (tour.isIn(ctrl) ? notice(ctrl) : null),
      arena.standing(ctrl, pagination.players(ctrl))
    ];
  },
  side: function(ctrl) {
    return ctrl.vm.playerInfo.id ? playerInfo(ctrl) : pairings(ctrl);
  }
};
