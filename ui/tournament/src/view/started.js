var m = require('mithril');
var partial = require('chessground').util.partial;
var util = require('./util');
var arena = require('./arena');
var pairings = require('./pairings');
var pagination = require('../pagination');

function myCurrentPairing(ctrl) {
  if (!ctrl.userId) return null;
  return ctrl.data.pairings.filter(function(p) {
    return p.s === 0 && (
      p.u[0].toLowerCase() === ctrl.userId || p.u[1].toLowerCase() === ctrl.userId
    );
  })[0];
}

module.exports = {
  main: function(ctrl) {
    var myPairing = myCurrentPairing(ctrl);
    var gameId = myPairing ? myPairing.id : null;
    var pag = pagination.players(ctrl);
    return [
      m('div.tournament_clock.title_tag', {
          config: util.clock(ctrl.data.secondsToFinish)
        },
        m('div.time.text[data-icon=p]')),
      util.title(ctrl),
      gameId ? m('a.is.is-after.pov.button.glowing', {
        href: '/' + gameId
      }, [
        'You are playing!',
        m('span.text[data-icon=G]', ctrl.trans('joinTheGame'))
      ]) : null, m('div.standing_wrap',
        pagination.render(ctrl, pag, function() {
          return m('table.slist.standing' + (ctrl.data.scheduled ? '.scheduled' : ''), arena.standing(ctrl, pag));
        })),
      util.games(ctrl.data.lastGames)
    ];
  },
  side: pairings
};
