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
    'You are playing!',
    m('span.text[data-icon=G]', ctrl.trans('joinTheGame'))
  ]);
}

function notice(ctrl) {
  return tour.willBePaired(ctrl) ? m('div.notice.wait',
    'Stand-by ' + ctrl.data.me.username + ', pairing players, get ready!'
  ) : m('div.notice.closed',
    'The tournament pairings are now closed.');
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
