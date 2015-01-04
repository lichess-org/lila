var m = require('mithril');
var partial = require('chessground').util.partial;
var tournament = require('../tournament');
var util = require('./util');
var arena = require('./arena');
var swiss = require('./swiss');
var pairings = require('./pairings');

module.exports = {
  main: function(ctrl) {
    var myPairing = tournament.myCurrentPairing(ctrl);
    var gameId = myPairing ? myPairing.gameId : null;
    return [
      m('div.tournament_clock.title_tag', {
        config: function(el, isUpdate) {
          if (!isUpdate) $(el).clock({
            time: ctrl.data.seconds
          });
        }
      }, m('div.time.text[data-icon=p]')),
      util.title(ctrl),
      gameId ? m('a.is.is-after.pov.button.glowing', {
        href: '/' + gameId
      }, [
        'You are playing!',
        m('span.text[data-icon=G]', ctrl.trans('joinTheGame'))
      ]) : null,
      m('div.standing_wrap.scroll-shadow-soft',
        m('table.slist.standing' + (ctrl.data.scheduled ? '.scheduled' : ''),
          ctrl.data.system === 'arena' ? arena.standing(ctrl) : swiss.standing(ctrl))),
      util.games(ctrl.data.lastGames)
    ];
  },
  side: pairings
};
