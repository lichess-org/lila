var m = require('mithril');
var partial = require('chessground').util.partial;
var tournament = require('../tournament');
var util = require('./util');
var arena = require('./arena');
var swiss = require('./swiss');
var pairings = require('./pairings');

module.exports = {
  main: function(ctrl) {
    var gameId = tournament.myCurrentGame(ctrl);
    return [
      m('div.title_tag', ctrl.trans('finished')),
      util.title(ctrl),
      m('div.standing_wrap.scroll-shadow-soft',
        m('table.slist.standing' + (ctrl.data.scheduled ? '.scheduled' : ''),
          ctrl.data.system === 'arena' ? arena.standing(ctrl) : swiss.standing(ctrl))),
      util.games(ctrl.data.lastGames)
    ];
  },
  side: pairings
};
